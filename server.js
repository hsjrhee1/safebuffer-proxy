const express = require('express');
const crypto  = require('crypto');

const app    = express();
const SECRET = process.env.TIMESTAMP_SECRET;

app.use(express.json());

app.use((req, res, next) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST');
  next();
});

app.get('/health', (req, res) => {
  res.json({ ok: true, time: new Date().toISOString() });
});

app.get('/timestamp', (req, res) => {
  if (!SECRET) return res.status(500).json({ error: 'TIMESTAMP_SECRET not configured' });
  const ts   = new Date().toISOString();
  const hash = (req.query.hash || '').toString().trim();
  const sig  = crypto.createHmac('sha256', SECRET).update(`${ts}:${hash}`).digest('hex');
  res.json({ timestamp: ts, hash, signature: sig });
});

app.post('/timestamp', (req, res) => {
  if (!SECRET) return res.status(500).json({ error: 'TIMESTAMP_SECRET not configured' });
  const ts   = new Date().toISOString();
  const hash = (req.body?.hash || '').toString().trim();
  const sig  = crypto.createHmac('sha256', SECRET).update(`${ts}:${hash}`).digest('hex');
  res.json({ timestamp: ts, hash, signature: sig });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => console.log(`SafeBuffer server listening on port ${PORT}`));
