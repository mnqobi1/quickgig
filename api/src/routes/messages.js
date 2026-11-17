// src/routes/messages.js
const express = require('express');
const db = require('../db');
const authenticate = require('../middleware/authenticate');

const router = express.Router();

router.get('/:gigId/messages', authenticate, (req, res) => {
  const gig = db.prepare('SELECT * FROM gigs WHERE id = ?').get(req.params.gigId);
  if (!gig) return res.status(404).json({ error: 'Gig not found' });

  const participant = Number(req.userId) === gig.poster_id || Number(req.userId) === gig.assigned_to;
  if (!participant) {
    return res.status(403).json({ error: 'Only the gig poster and assigned worker can view this chat' });
  }

  const messages = db
    .prepare('SELECT * FROM messages WHERE gig_id = ? ORDER BY created_at ASC')
    .all(req.params.gigId);

  res.json({ messages });
});

router.post('/:gigId/messages', authenticate, (req, res) => {
  const gig = db.prepare('SELECT * FROM gigs WHERE id = ?').get(req.params.gigId);
  if (!gig) return res.status(404).json({ error: 'Gig not found' });

  const { recipientId, body } = req.body;
  if (!recipientId || !body) {
    return res.status(400).json({ error: 'recipientId and body are required' });
  }

  const info = db
    .prepare(
      'INSERT INTO messages (gig_id, sender_id, recipient_id, body) VALUES (?, ?, ?, ?)'
    )
    .run(req.params.gigId, req.userId, recipientId, body);

  const message = db.prepare('SELECT * FROM messages WHERE id = ?').get(info.lastInsertRowid);
  res.status(201).json(message);
});

module.exports = router;
