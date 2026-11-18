// src/routes/messages.js — in-app chat between a gig's poster and its assigned worker.
const express = require('express');
const db = require('../db');
const { authenticate } = require('../middleware/authenticate');
const { notifyUser } = require('../notify');
const { iso } = require('../util');

const router = express.Router();

function serialize(row) {
  return {
    id: row.id,
    gigId: row.gig_id,
    senderId: row.sender_id,
    recipientId: row.recipient_id,
    body: row.body,
    createdAt: iso(row.created_at),
  };
}

/** Loads the gig and checks the caller is one of the two chat participants. */
function participantGig(req, res) {
  const gig = db.prepare('SELECT * FROM gigs WHERE id = ?').get(req.params.gigId);
  if (!gig) {
    res.status(404).json({ error: 'Gig not found' });
    return null;
  }
  if (req.userId !== gig.poster_id && req.userId !== gig.assigned_to) {
    res.status(403).json({ error: 'Only the gig poster and assigned worker can use this chat' });
    return null;
  }
  return gig;
}

router.get('/:gigId/messages', authenticate, (req, res) => {
  const gig = participantGig(req, res);
  if (!gig) return;
  const after = Number(req.query.after) || 0;
  const rows = db
    .prepare('SELECT * FROM messages WHERE gig_id = ? AND id > ? ORDER BY id ASC')
    .all(gig.id, after);
  res.json({ messages: rows.map(serialize) });
});

router.post('/:gigId/messages', authenticate, (req, res) => {
  const gig = participantGig(req, res);
  if (!gig) return;

  const body = String(req.body.body || '').trim();
  if (!body) return res.status(400).json({ error: 'body is required' });
  if (body.length > 1000) return res.status(400).json({ error: 'body must be at most 1000 characters' });

  // The recipient is always "the other person" — never trust a client-supplied one.
  const recipientId = req.userId === gig.poster_id ? gig.assigned_to : gig.poster_id;
  if (!recipientId) {
    return res.status(409).json({ error: 'Chat opens once a worker has been assigned' });
  }

  const clientId = req.body.clientId ? String(req.body.clientId) : null;
  if (clientId) {
    const dup = db.prepare('SELECT * FROM messages WHERE sender_id = ? AND client_id = ?').get(req.userId, clientId);
    if (dup) return res.status(200).json(serialize(dup)); // offline retry — already stored
  }

  const info = db
    .prepare('INSERT INTO messages (gig_id, sender_id, recipient_id, body, client_id) VALUES (?, ?, ?, ?, ?)')
    .run(gig.id, req.userId, recipientId, body, clientId);
  const message = db.prepare('SELECT * FROM messages WHERE id = ?').get(info.lastInsertRowid);

  const sender = db.prepare('SELECT name FROM users WHERE id = ?').get(req.userId);
  notifyUser(recipientId, {
    type: 'message',
    title: `New message from ${sender.name}`,
    body: body.length > 80 ? `${body.slice(0, 77)}…` : body,
    gigId: gig.id,
  });

  res.status(201).json(serialize(message));
});

module.exports = router;
