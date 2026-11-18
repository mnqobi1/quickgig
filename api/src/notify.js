// src/notify.js — creates notifications and delivers them in real time.
//
// Delivery paths for one notification:
//   1. stored in the `notifications` table (the inbox / unread badge / catch-up after offline)
//   2. pushed instantly to the user's open app over Server-Sent Events (SSE)
//   3. pushed through Firebase Cloud Messaging to the user's devices (if configured)
const db = require('./db');
const fcm = require('./fcm');

// userId -> Set<http.ServerResponse> (a user may have the app open on several devices)
const subscribers = new Map();

function iso(sqliteTimestamp) {
  return sqliteTimestamp ? `${String(sqliteTimestamp).replace(' ', 'T')}Z` : null;
}

function serialize(row) {
  return {
    id: row.id,
    type: row.type,
    title: row.title,
    body: row.body,
    gigId: row.gig_id,
    createdAt: iso(row.created_at),
    read: row.read_at != null,
  };
}

function subscribe(userId, res) {
  const key = Number(userId);
  if (!subscribers.has(key)) subscribers.set(key, new Set());
  subscribers.get(key).add(res);
}

function unsubscribe(userId, res) {
  const key = Number(userId);
  const set = subscribers.get(key);
  if (!set) return;
  set.delete(res);
  if (set.size === 0) subscribers.delete(key);
}

function subscriberCount(userId) {
  return subscribers.get(Number(userId))?.size || 0;
}

/** Format one notification as an SSE frame. */
function sseFrame(notification) {
  return `id: ${notification.id}\nevent: notification\ndata: ${JSON.stringify(notification)}\n\n`;
}

/**
 * Create + deliver a notification. `type` is a stable machine code the app uses for its icon and
 * for translating text; title/body are English fallbacks.
 */
function notifyUser(userId, { type, title, body, gigId = null }) {
  const info = db
    .prepare('INSERT INTO notifications (user_id, type, title, body, gig_id) VALUES (?, ?, ?, ?, ?)')
    .run(userId, type, title, body, gigId);
  const notification = serialize(db.prepare('SELECT * FROM notifications WHERE id = ?').get(info.lastInsertRowid));

  const user = db.prepare('SELECT notifications_enabled FROM users WHERE id = ?').get(userId);
  const pushEnabled = !user || user.notifications_enabled !== 0;
  if (pushEnabled) {
    for (const res of subscribers.get(Number(userId)) || []) {
      try {
        res.write(sseFrame(notification));
      } catch (e) {
        unsubscribe(userId, res);
      }
    }
    // Fire and forget — a slow/failed FCM call must never block or break the API response.
    fcm.pushToUser(userId, notification).catch(() => {});
  }
  return notification;
}

function listForUser(userId, { after = 0, limit = 50 } = {}) {
  const rows = db
    .prepare('SELECT * FROM notifications WHERE user_id = ? AND id > ? ORDER BY id ASC LIMIT ?')
    .all(userId, after, limit);
  return rows.map(serialize);
}

module.exports = { notifyUser, listForUser, subscribe, unsubscribe, subscriberCount, sseFrame, serialize, iso };
