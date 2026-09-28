const express = require('express');
const multer = require('multer');
const axios = require('axios');
const FormData = require('form-data');
const cors = require('cors');
const crypto = require('crypto');
const net = require('net');

// STT 업로드 제한: 파일 1개, 최대 25MB (앱의 10분 청크는 최대 약 7.2MB)
const MAX_UPLOAD_BYTES = 25 * 1024 * 1024;
// Groq 요청 제한 시간 (앱의 OkHttp readTimeout 120초보다 짧게)
const GROQ_TIMEOUT_MS = 90 * 1000;

const app = express();
const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: MAX_UPLOAD_BYTES, files: 1 },
});

app.use(cors());
app.use(express.json());

const GROQ_API_KEY = process.env.GROQ_API_KEY;
const TIMESTAMP_SECRET = process.env.TIMESTAMP_SECRET;

if (!GROQ_API_KEY) {
  console.error('GROQ_API_KEY 환경변수가 없습니다.');
  process.exit(1);
}

if (!TIMESTAMP_SECRET || !TIMESTAMP_SECRET.trim()) {
  console.error('TIMESTAMP_SECRET 환경변수가 없습니다. 서버를 시작하지 않습니다.');
  process.exit(1);
}

// 업로드 파싱 오류(크기 초과·잘못된 multipart)는 400/413으로 응답하고 프로세스는 유지한다
function uploadAudio(req, res, next) {
  upload.single('file')(req, res, (err) => {
    if (!err) return next();
    if (err instanceof multer.MulterError && err.code === 'LIMIT_FILE_SIZE') {
      return res.status(413).json({ error: '오디오 파일이 너무 큽니다.' });
    }
    return res.status(400).json({ error: '업로드 요청이 올바르지 않습니다.' });
  });
}

// ── /api/transcribe 전용: kill switch · rate limit · 동시 처리 제한 ─────────────
// client IP는 limiter key로만 사용하고 로그에 기록하지 않는다.
const STT_RATE_WINDOW_MS = 10 * 60 * 1000;   // 고정 window 10분
const STT_RATE_MAX = 60;                      // window당 검증된 key별 최대 요청 수
const STT_MAX_RATE_BUCKETS = 10000;           // rate bucket 최대 개수 (메모리 상한)
const STT_SWEEP_INTERVAL_MS = 60 * 1000;      // 만료 bucket 정리 주기
const STT_GLOBAL_CONCURRENCY = 3;             // 서버 전체 동시 STT (미검증 client 포함)
const STT_PER_KEY_CONCURRENCY = 2;            // 검증된 key별 동시 STT
const STT_BUSY_RETRY_AFTER_S = 10;
const STT_DISABLED_RETRY_AFTER_S = 600;
const OVERFLOW_KEY = 'overflow';
const STT_TOO_MANY = { error: 'STT 요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.' };
const STT_UNAVAILABLE = { error: 'STT 서비스를 잠시 사용할 수 없습니다.' };

// 환경변수 값이 정확히 false(대소문자·앞뒤 공백 무시)일 때만 true. 미설정·그 밖의 값은 false.
function envIsFalse(name) {
  return String(process.env[name] || '').trim().toLowerCase() === 'false';
}

function rejectStt(res, status, retryAfterSec, body) {
  res.set('Retry-After', String(Math.max(1, Math.ceil(retryAfterSec))));
  return res.status(status).json(body);
}

// STT_ENABLED=false 일 때만 STT를 끈다. limiter 설정과 무관하게 항상 먼저 적용된다.
function sttGate(req, res, next) {
  if (envIsFalse('STT_ENABLED')) {
    return rejectStt(res, 503, STT_DISABLED_RETRY_AFTER_S, STT_UNAVAILABLE);
  }
  next();
}

// IPv6 주소의 앞 64bit(/64)를 정규화된 문자열로 돌려준다. 형식이 틀리면 null.
function ipv6Prefix64(ip) {
  let addr = ip.toLowerCase();
  const lastColon = addr.lastIndexOf(':');
  const tail = addr.slice(lastColon + 1);
  if (tail.includes('.')) {                   // 끝부분이 IPv4 표기인 형태
    if (!net.isIPv4(tail)) return null;
    const o = tail.split('.').map(Number);
    addr = addr.slice(0, lastColon + 1)
      + ((o[0] << 8) | o[1]).toString(16) + ':' + ((o[2] << 8) | o[3]).toString(16);
  }
  const halves = addr.split('::');
  if (halves.length > 2) return null;
  const head = halves[0] ? halves[0].split(':') : [];
  const rest = halves.length === 2 && halves[1] ? halves[1].split(':') : [];
  const fill = halves.length === 2 ? 8 - head.length - rest.length : 0;
  if (fill < 0) return null;
  const groups = [...head, ...new Array(fill).fill('0'), ...rest];
  if (groups.length !== 8 || groups.some((g) => !/^[0-9a-f]{1,4}$/.test(g))) return null;
  return groups.slice(0, 4).map((g) => parseInt(g, 16).toString(16)).join(':');
}

// IP 문자열을 limiter key로 정규화한다. IPv4는 주소 단위, IPv6는 /64 단위. 형식이 틀리면 null.
function normalizeIpKey(value) {
  const v = String(value || '').trim();
  if (!v || v.length > 45 || v.includes('%') || v.includes(',')) return null;
  const family = net.isIP(v);
  if (family === 4) return `v4:${v}`;
  if (family === 6) {
    const mapped = /^::ffff:(\d{1,3}(?:\.\d{1,3}){3})$/i.exec(v);
    if (mapped && net.isIPv4(mapped[1])) return `v4:${mapped[1]}`;
    const prefix = ipv6Prefix64(v);
    return prefix ? `v6:${prefix}::/64` : null;
  }
  return null;
}

// Cloudflare가 넣는 CF-Connecting-IP만 신뢰한다. True-Client-IP가 있으면 일치해야 한다.
// 확인하지 못하면 null(미검증). X-Forwarded-For 와 req.ip 는 사용하지 않는다.
function sttClientKey(req) {
  const cf = normalizeIpKey(req.get('cf-connecting-ip'));
  if (!cf) return null;
  const trueClientIp = req.get('true-client-ip');
  if (trueClientIp !== undefined && normalizeIpKey(trueClientIp) !== cf) return null;
  return cf;
}

const sttRateBuckets = new Map();             // 검증된 key → { windowStart, count }
let sttLastSweep = Date.now();
let sttLastForcedSweep = 0;
let sttUnknownCount = 0;
let sttRejectedCount = 0;

function removeExpiredSttBuckets(now) {
  for (const [key, bucket] of sttRateBuckets) {
    if (now - bucket.windowStart >= STT_RATE_WINDOW_MS) sttRateBuckets.delete(key);
  }
}

function sttRateLimit(req, res, next) {
  // STT_RATE_LIMIT_ENABLED=false 일 때만 limiter 전체를 우회한다 (긴급용).
  res.locals.sttLimiterOn = !envIsFalse('STT_RATE_LIMIT_ENABLED');
  if (!res.locals.sttLimiterOn) return next();

  const now = Date.now();
  if (now - sttLastSweep >= STT_SWEEP_INTERVAL_MS) {
    removeExpiredSttBuckets(now);
    sttLastSweep = now;
    if (sttUnknownCount || sttRejectedCount) {
      // 집계 숫자만 남긴다 (IP·key 기록 안 함)
      console.log(`[STT-LIMIT] unknown=${sttUnknownCount} rejected=${sttRejectedCount} buckets=${sttRateBuckets.size}`);
      sttUnknownCount = 0;
      sttRejectedCount = 0;
    }
  }

  let key = sttClientKey(req);
  if (!key) {
    // 미검증 client는 per-IP bucket에 넣지 않는다 (header 구조 변경 시 정상 사용자가
    // 하나의 공용 bucket으로 함께 막히는 것을 방지). global concurrency 보호만 받는다.
    sttUnknownCount++;
    res.locals.sttKey = null;
    return next();
  }

  let bucket = sttRateBuckets.get(key);
  if (!bucket && sttRateBuckets.size >= STT_MAX_RATE_BUCKETS) {
    if (now - sttLastForcedSweep >= 1000) {
      sttLastForcedSweep = now;
      removeExpiredSttBuckets(now);
    }
    // 상한 도달: 새 key를 만들지 않고 공용 overflow bucket으로 제한한다 (fail-open 금지)
    if (sttRateBuckets.size >= STT_MAX_RATE_BUCKETS) {
      key = OVERFLOW_KEY;
      bucket = sttRateBuckets.get(key);
    }
  }
  if (!bucket || now - bucket.windowStart >= STT_RATE_WINDOW_MS) {
    bucket = { windowStart: now, count: 0 };
    sttRateBuckets.set(key, bucket);
  }
  if (bucket.count >= STT_RATE_MAX) {
    sttRejectedCount++;
    return rejectStt(res, 429, (bucket.windowStart + STT_RATE_WINDOW_MS - now) / 1000, STT_TOO_MANY);
  }
  bucket.count++;
  res.locals.sttKey = key;
  next();
}

let sttActiveGlobal = 0;
const sttActiveByKey = new Map();             // 검증된 key → 진행 중 개수 (0이 되면 삭제)

// 업로드 전에 slot을 점유한다. 응답이 끝나고 handler도 끝났을 때 한 번만 반환한다.
// limiter OFF이면 아무것도 점유하지 않는다. 미검증 client(key=null)는 global slot만 점유한다.
function reserveSttSlot(req, res, next) {
  if (!res.locals.sttLimiterOn) return next();
  const key = res.locals.sttKey;
  const activeForKey = key ? (sttActiveByKey.get(key) || 0) : 0;
  if (sttActiveGlobal >= STT_GLOBAL_CONCURRENCY
      || (key && activeForKey >= STT_PER_KEY_CONCURRENCY)) {
    sttRejectedCount++;
    return rejectStt(res, 429, STT_BUSY_RETRY_AFTER_S, STT_TOO_MANY);
  }
  sttActiveGlobal++;
  if (key) sttActiveByKey.set(key, activeForKey + 1);

  let released = false;
  let handlerRunning = false;
  let responseDone = false;
  const release = () => {
    if (released) return;
    released = true;
    sttActiveGlobal = Math.max(0, sttActiveGlobal - 1);
    if (key) {
      const remaining = (sttActiveByKey.get(key) || 1) - 1;
      if (remaining > 0) sttActiveByKey.set(key, remaining);
      else sttActiveByKey.delete(key);
    }
  };
  const onResponseDone = () => {
    responseDone = true;
    if (!handlerRunning) release();
  };
  res.once('finish', onResponseDone);
  res.once('close', onResponseDone);
  res.locals.sttSlot = {
    isReleased: () => released,
    begin: () => { handlerRunning = true; },
    end: () => {
      handlerRunning = false;
      if (responseDone || res.writableEnded || res.destroyed) release();
    },
  };
  next();
}

// 기존 STT handler를 감싸 Groq 호출이 끝날 때까지 slot을 유지하고 반드시 반환한다.
// limiter OFF(slot 없음)이면 기존 handler를 그대로 실행한다.
function trackSttHandler(handler) {
  return async (req, res, next) => {
    const slot = res.locals.sttSlot;
    if (slot && slot.isReleased()) return;    // 업로드 중 연결이 끊긴 요청은 Groq로 보내지 않는다
    if (slot) slot.begin();
    try {
      await handler(req, res, next);
    } catch (err) {
      next(err);
    } finally {
      if (slot) slot.end();
    }
  };
}

app.get('/', (req, res) => {
  res.json({ status: 'ok', service: 'SafeBuffer Proxy' });
});

app.post('/api/transcribe', sttGate, sttRateLimit, reserveSttSlot, uploadAudio, trackSttHandler(async (req, res) => {
  try {
    if (!req.file) return res.status(400).json({ error: '오디오 파일이 없습니다.' });
    const form = new FormData();
    form.append('file', req.file.buffer, {
      filename: req.file.originalname || 'audio.m4a',
      contentType: req.file.mimetype || 'audio/mp4',
    });
    form.append('model', 'whisper-large-v3');
    form.append('response_format', 'verbose_json');
    const response = await axios.post(
      'https://api.groq.com/openai/v1/audio/transcriptions',
      form,
      {
        headers: { Authorization: `Bearer ${GROQ_API_KEY}`, ...form.getHeaders() },
        maxBodyLength: MAX_UPLOAD_BYTES + 1024 * 1024,
        timeout: GROQ_TIMEOUT_MS,
      }
    );
    res.json(response.data);
  } catch (err) {
    // 로그에는 상태·원인만 남기고, 클라이언트에는 Groq 응답 원문을 전달하지 않는다
    console.error(
      'Groq 오류:',
      err.response ? `HTTP ${err.response.status}` : (err.code || 'NO_RESPONSE'),
      err.response?.data?.error?.message || err.message
    );
    res.status(500).json({ error: 'STT 처리에 실패했습니다.' });
  }
}));

app.post('/api/stamp', (req, res) => {
  try {
    const { hashes, deviceTime } = req.body;
    if (!Array.isArray(hashes) || hashes.length === 0)
      return res.status(400).json({ error: 'hashes 배열이 필요합니다.' });
    const token = crypto.randomUUID();
    const serverTime = new Date().toISOString();
    const hashList = hashes.join(',');
    const payload = `${token}|${serverTime}|${hashList}`;
    const sig = crypto.createHmac('sha256', TIMESTAMP_SECRET).update(payload).digest('hex');
    console.log(`[STAMP] token=${token} serverTime=${serverTime} deviceTime=${deviceTime || 'N/A'} hashes=${hashList}`);
    res.json({ token, serverTime, hashes, sig });
  } catch (err) {
    console.error('Stamp 오류:', err.name, err.message);
    res.status(500).json({ error: '타임스탬프 발급에 실패했습니다.' });
  }
});

// JSON 파싱 오류 등은 Express 기본 HTML 오류 페이지(stack trace) 대신 일반 JSON으로 응답한다
app.use((err, req, res, next) => {
  if (res.headersSent) return next(err);
  const status = err.status >= 400 && err.status < 500 ? err.status : 500;
  if (status === 500) console.error('요청 처리 오류:', err.type || err.name);
  res.status(status).json({
    error: status === 500 ? '서버 오류가 발생했습니다.' : '요청 형식이 올바르지 않습니다.',
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => console.log(`SafeBuffer 프록시 서버 실행 중: ${PORT}`));
