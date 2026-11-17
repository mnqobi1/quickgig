// src/routes/users.js
const express = require('express');
const db = require('../db');
const authenticate = require('../middleware/authenticate');
const hustle = require('../hustle');

const router = express.Router();

function requireSelf(req, res, next) {
  if (Number(req.params.id) !== Number(req.userId)) {
    return res.status(403).json({ error: 'You can only modify your own account' });
  }
  next();
}

router.get('/:id', (req, res) => {
  const user = db.prepare('SELECT * FROM users WHERE id = ?').get(req.params.id);
  if (!user) return res.status(404).json({ error: 'User not found' });

  const badges = db
    .prepare('SELECT badge_code, unlocked_at FROM badges WHERE user_id = ?')
    .all(req.params.id);
  const stats = hustle.getUserStats(req.params.id);

  res.json({
    id: user.id,
    name: user.name,
    photoUrl: user.photo_url,
    campusArea: user.campus_area,
    hustleScore: user.hustle_score,
    verificationStatus: user.verification_status,
    badges,
    completedGigs: stats.completedCount,
    averageRating: Math.round((stats.averageRating || 0) * 10) / 10,
  });
});

router.patch('/:id/settings', authenticate, requireSelf, (req, res) => {
  const fields = ['name', 'phone', 'preferred_language', 'campus_area', 'photo_url'];
  const incoming = {
    name: req.body.name,
    phone: req.body.phone,
    preferred_language: req.body.preferredLanguage,
    campus_area: req.body.campusArea,
    photo_url: req.body.photoUrl,
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
  });
});

router.get('/:id/hustle', (req, res) => {
  const stats = hustle.getUserStats(req.params.id);
  if (!stats) return res.status(404).json({ error: 'User not found' });

  const badges = db
    .prepare('SELECT badge_code, unlocked_at FROM badges WHERE user_id = ?')
    .all(req.params.id);
  const history = db
    .prepare('SELECT * FROM hustle_transactions WHERE user_id = ? ORDER BY created_at DESC LIMIT 20')
    .all(req.params.id);

  res.json({ ...stats, badges, history });
});

module.exports = router;
