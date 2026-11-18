// src/routes/devices.js — register a phone's FCM token so the server can push to it.
const express = require('express');
const db = require('../db');
const { authenticate } = require('../middleware/authenticate');

const router = express.Router();
router.use(authenticate);

router.post('/', (req, res) => {
  const token = req.body.token;
  if (typeof token !== 'string' || token.length < 20 || token.length > 4096) {
    return res.status(400).json({ error: 'token is required' });
  }
  db.prepare(
    `INSERT INTO device_tokens (token, user_id) VALUES (?, ?)
     ON CONFLICT(token) DO UPDATE SET user_id = excluded.user_id, updated_at = datetime('now')`
  ).run(token, req.userId);
  res.status(204).end();
});

router.delete('/:token', (req, res) => {
  db.prepare('DELETE FROM device_tokens WHERE token = ? AND user_id = ?').run(req.params.token, req.userId);
  res.status(204).end();
});

module.exports = router;
