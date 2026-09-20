const express = require('express');
const multer = require('multer');
const axios = require('axios');
const FormData = require('form-data');
const cors = require('cors');
const crypto = require('crypto');

const app = express();
const upload = multer({ storage: multer.memoryStorage() });

app.use(cors());
app.use(express.json());

const GROQ_API_KEY = process.env.GROQ_API_KEY;
const STAMP_SECRET = process.env.STAMP_SECRET || 'safebuffer-default-secret';

if (!GROQ_API_KEY) {
  console.error('GROQ_API_KEY 환경변수가 없습니다.');
  process.exit(1);
}

app.get('/', (req, res) => {
  res.json({ status: 'ok', service: 'SafeBuffer Proxy' });
});

app.post('/api/transcribe', upload.single('file'), async (req, res) => {
  try {
    if (!req.file) return res.status(400).json({ error: '오디오 파일이 없습니다.' });
    const form = new FormData();
    form.append('file', req.file.buffer, {
      filename: req.file.originalname || 'audio.m4a',
      contentType: req.file.mimetype || 'audio/mp4',
    });
    form.append('model', 'whisper-large-v3');
    form.append('language', 'ko');
    form.append('response_format', 'verbose_json');
    const response = await axios.post(
      'https://api.groq.com/openai/v1/audio/transcriptions',
      form,
      { headers: { Authorization: `Bearer ${GROQ_API_KEY}`, ...form.getHeaders() }, maxBodyLength: Infinity }
    );
    res.json(response.data);
  } catch (err) {
    console.error('Groq 오류:', err.response?.data || err.message);
    res.status(500).json({ error: err.response?.data || err.message });
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
    const sig = crypto.createHmac('sha256', STAMP_SECRET).update(payload).digest('hex');
    console.log(`[STAMP] token=${token} serverTime=${serverTime} deviceTime=${deviceTime || 'N/A'} hashes=${hashList}`);
    res.json({ token, serverTime, hashes, sig });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => console.log(`SafeBuffer 프록시 서버 실행 중: ${PORT}`));
