// src/routes/notifications.js — inbox + real-time stream.
const express = require('express');
const db = require('../db');
const { authenticate } = require('../middleware/authenticate');
const notify = require('../notify');
const { SSE_HEARTBEAT_MS } = require('../../config');

const router = express.Router();
router.use(authenticate);

// GET /api/notifications?after=<id>&limit=<n> → oldest-first, so the client can append.
router.get('/', (req, res) => {
  const after = Number(req.query.after) || 0;
  const limit = Math.min(Number(req.query.limit) || 50, 200);
  const unread = db
    .prepare('SELECT COUNT(*) AS n FROM notifications WHERE user_id = ? AND read_at IS NULL')
    .get(req.userId).n;
  res.json({ notifications: notify.listForUser(req.userId, { after, limit }), unreadCount: unread });
});

// POST /api/notifications/read  { ids?: number[] }  — omit ids to mark everything read.
router.post('/read', (req, res) => {
  const ids = Array.isArray(req.body.ids) ? req.body.ids.filter(Number.isInteger) : null;
  if (ids && ids.length) {
    const marks = ids.map(() => '?').join(',');
    db.prepare(`UPDATE notifications SET read_at = datetime('now') WHERE user_id = ? AND read_at IS NULL AND id IN (${marks})`)
      .run(req.userId, ...ids);
  } else {
    db.prepare("UPDATE notifications SET read_at = datetime('now') WHERE user_id = ? AND read_at IS NULL").run(req.userId);
  }
  res.status(204).end();
});

// POST /api/notifications/test — sends the caller a notification (demo / diagnostics button).
router.post('/test', (req, res) => {
  const n = notify.notifyUser(req.userId, {
    type: 'test',
    title: 'Test notification',
    body: 'Real-time notifications are working 🎉',
  });
  res.status(201).json(n);
});

// GET /api/notifications/stream — Server-Sent Events. The connection stays open and every new
// notification is written to it the instant it is created. `Last-Event-ID` (sent automatically by
// the app on reconnect) replays anything missed while the connection was down.
router.get('/stream', (req, res) => {
  res.status(200).set({
    'Content-Type': 'text/event-stream',
    'Cache-Control': 'no-cache, no-transform',
    Connection: 'keep-alive',
    'X-Accel-Buffering': 'no',
  });
  res.flushHeaders();
  res.write('retry: 3000\n\n');

  const lastId = Number(req.get('Last-Event-ID')) || Number(req.query.after) || 0;
  if (lastId > 0) {
    for (const n of notify.listForUser(req.userId, { after: lastId, limit: 200 })) res.write(notify.sseFrame(n));
  }

  notify.subscribe(req.userId, res);
  const heartbeat = setInterval(() => res.write(': keep-alive\n\n'), SSE_HEARTBEAT_MS);
  const cleanup = () => {
    clearInterval(heartbeat);
    notify.unsubscribe(req.userId, res);
  };
  req.on('close', cleanup);
  res.on('error', cleanup);
});

module.exports = router;
