const express = require('express');
const multer = require('multer');
const axios = require('axios');
const FormData = require('form-data');
const cors = require('cors');
const crypto = require('crypto');

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

app.get('/', (req, res) => {
  res.json({ status: 'ok', service: 'SafeBuffer Proxy' });
});

app.post('/api/transcribe', uploadAudio, async (req, res) => {
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
});

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

// TEMP (PHASE 2A-0): Render client IP 실측용. 측정 후 제거한다.
// IPCHECK_KEY 환경변수가 있을 때만 활성화되며, 측정 값은 서버 로그에 기록하지 않는다.
const IPCHECK_KEY = process.env.IPCHECK_KEY;
if (IPCHECK_KEY && IPCHECK_KEY.length >= 32) {
  const sha256 = (v) => crypto.createHash('sha256').update(String(v)).digest();
  const expectedKeyHash = sha256(IPCHECK_KEY);
  app.get('/__ipcheck', (req, res, next) => {
    const givenKey = req.get('x-ipcheck-key');
    // key가 없거나 틀리면 라우트가 없는 것과 같은 기본 404로 넘긴다
    if (!givenKey || !crypto.timingSafeEqual(sha256(givenKey), expectedKeyHash)) return next();
    const xff = String(req.get('x-forwarded-for') || '')
      .split(',').map((s) => s.trim()).filter(Boolean);
    const header = (name) => req.get(name) ?? null;
    res.set('Cache-Control', 'no-store').json({
      socketRemoteAddress: req.socket.remoteAddress,
      xForwardedForCount: xff.length,
      xForwardedFor: xff,
      cfConnectingIp: header('cf-connecting-ip'),
      trueClientIp: header('true-client-ip'),
      xRealIp: header('x-real-ip'),
      xForwardedProto: header('x-forwarded-proto'),
      hasCfRay: req.get('cf-ray') !== undefined,
      reqIp: req.ip,
    });
  });
  console.log('IPCHECK 임시 endpoint 활성화됨');
} else if (IPCHECK_KEY) {
  console.warn('IPCHECK_KEY가 32자 미만이라 임시 endpoint를 활성화하지 않습니다.');
}

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
