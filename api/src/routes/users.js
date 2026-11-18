// src/routes/users.js
const express = require('express');
const db = require('../db');
const { authenticate } = require('../middleware/authenticate');
const { LANGUAGES, iso } = require('../util');
const hustle = require('../hustle');

const router = express.Router();

function requireSelf(req, res, next) {
  if (Number(req.params.id) !== Number(req.userId)) {
    return res.status(403).json({ error: 'You can only modify your own account' });
  }
  next();
}

function serializeBadge(b) {
  return { badgeCode: b.badge_code, unlockedAt: iso(b.unlocked_at) };
}

router.get('/:id', (req, res) => {
  const user = db.prepare('SELECT * FROM users WHERE id = ?').get(req.params.id);
  if (!user) return res.status(404).json({ error: 'User not found' });

  const badges = db
    .prepare('SELECT badge_code, unlocked_at FROM badges WHERE user_id = ?')
    .all(req.params.id)
    .map(serializeBadge);
  const stats = hustle.getUserStats(req.params.id);

  res.json({
    id: user.id,
    name: user.name,
    phone: user.phone,
    photoUrl: user.photo_url,
    campusArea: user.campus_area,
    hustleScore: user.hustle_score,
    verificationStatus: user.verification_status,
    notificationsEnabled: user.notifications_enabled !== 0,
    badges,
    completedGigs: stats.completedCount,
    averageRating: Math.round((stats.averageRating || 0) * 10) / 10,
  });
});

router.patch('/:id/settings', authenticate, requireSelf, (req, res) => {
  const fields = ['name', 'phone', 'preferred_language', 'campus_area', 'photo_url', 'notifications_enabled'];
  if (req.body.preferredLanguage !== undefined && !LANGUAGES.includes(req.body.preferredLanguage)) {
    return res.status(400).json({ error: `preferredLanguage must be one of ${LANGUAGES.join(', ')}` });
  }
  if (req.body.name !== undefined && !String(req.body.name).trim()) {
    return res.status(400).json({ error: 'name cannot be empty' });
  }
  if (req.body.notificationsEnabled !== undefined && typeof req.body.notificationsEnabled !== 'boolean') {
    return res.status(400).json({ error: 'notificationsEnabled must be true or false' });
  }
  const incoming = {
    name: req.body.name,
    phone: req.body.phone,
    preferred_language: req.body.preferredLanguage,
    campus_area: req.body.campusArea,
    photo_url: req.body.photoUrl,
    notifications_enabled: req.body.notificationsEnabled === undefined ? undefined : (req.body.notificationsEnabled ? 1 : 0),
  };

  const sets = [];
  const values = [];
  for (const f of fields) {
    if (incoming[f] !== undefined) {
      sets.push(`${f} = ?`);
      values.push(incoming[f]);
    }
  }
  if (sets.length === 0) {
    return res.status(400).json({ error: 'No recognised settings fields in request body' });
  }
  values.push(req.params.id);

  db.prepare(`UPDATE users SET ${sets.join(', ')} WHERE id = ?`).run(...values);
  const updated = db.prepare('SELECT * FROM users WHERE id = ?').get(req.params.id);
  res.json({
    id: updated.id,
    name: updated.name,
    email: updated.email,
    phone: updated.phone,
    preferredLanguage: updated.preferred_language,
    campusArea: updated.campus_area,
    photoUrl: updated.photo_url,
    notificationsEnabled: updated.notifications_enabled !== 0,
  });
});

router.get('/:id/hustle', (req, res) => {
  const stats = hustle.getUserStats(req.params.id);
  if (!stats) return res.status(404).json({ error: 'User not found' });

  const badges = db
    .prepare('SELECT badge_code, unlocked_at FROM badges WHERE user_id = ?')
    .all(req.params.id)
    .map(serializeBadge);
  const history = db
    .prepare('SELECT * FROM hustle_transactions WHERE user_id = ? ORDER BY created_at DESC LIMIT 20')
    .all(req.params.id);

  res.json({ ...stats, badges, history });
});

// Account deletion (required by Google Play for any app that lets people create an account).
// Removes the user and everything that belongs to them; other people's records that merely
// reference them (gigs they were working on, points history) are detached rather than lost.
router.delete('/:id', authenticate, requireSelf, (req, res) => {
  const id = Number(req.params.id);
  db.transaction(() => {
    db.prepare("UPDATE hustle_transactions SET gig_id = NULL WHERE gig_id IN (SELECT id FROM gigs WHERE poster_id = ?)").run(id);
    db.prepare("UPDATE gigs SET status = 'open', assigned_to = NULL WHERE assigned_to = ? AND status = 'assigned'").run(id);
    db.prepare('UPDATE gigs SET assigned_to = NULL WHERE assigned_to = ?').run(id);
    db.prepare('DELETE FROM users WHERE id = ?').run(id);
  })();
  res.status(204).end();
});

module.exports = router;
