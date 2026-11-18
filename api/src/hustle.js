// src/hustle.js — the Hustle Score (gamification) rules. Mirrored client-side in
// android/.../gamification/HustleScoreCalculator.kt for an instant preview; this file is the
// source of truth.
const db = require('./db');

const BADGE_RULES = [
  {
    code: 'verified_student',
    label: 'Verified Student',
    check: (stats) => stats.verificationStatus === 'verified',
  },
  {
    code: 'top_rated',
    label: 'Top Rated',
    check: (stats) => stats.ratingCount >= 5 && stats.averageRating >= 4.5,
  },
  {
    code: 'reliable_runner',
    label: 'Reliable Runner',
    check: (stats) => stats.completedCount >= 10 && stats.lateCancellations === 0,
  },
];

function pointsForCompletion({ stars, onTime }) {
  const BASE = 10;
  const RATING_BONUS = 10;
  const ON_TIME_BONUS = 5;
  const ratingPart = Math.round((Math.max(0, Math.min(5, stars)) / 5) * RATING_BONUS);
  const onTimePart = onTime ? ON_TIME_BONUS : 0;
  return BASE + ratingPart + onTimePart;
}

function getUserStats(userId) {
  const user = db.prepare('SELECT * FROM users WHERE id = ?').get(userId);
  if (!user) return null;

  const completedCount = db
    .prepare("SELECT COUNT(*) AS n FROM gigs WHERE assigned_to = ? AND status = 'completed'")
    .get(userId).n;

  // A late cancellation is the *worker* pulling out of a gig they had accepted. (A poster
  // cancelling a gig is not the worker's fault and must not count against them.)
  const lateCancellations = db
    .prepare('SELECT COUNT(*) AS n FROM gig_withdrawals WHERE user_id = ?')
    .get(userId).n;

  const ratingRow = db
    .prepare('SELECT COUNT(*) AS n, AVG(stars) AS avg FROM ratings WHERE rated_user_id = ?')
    .get(userId);

  return {
    verificationStatus: user.verification_status,
    completedCount,
    lateCancellations,
    ratingCount: ratingRow.n || 0,
    averageRating: ratingRow.avg || 0,
    hustleScore: user.hustle_score,
  };
}

function awardCompletion({ userId, gigId, stars, onTime }) {
  const points = pointsForCompletion({ stars, onTime });

  db.prepare('UPDATE users SET hustle_score = hustle_score + ? WHERE id = ?').run(points, userId);
  db.prepare(
    'INSERT INTO hustle_transactions (user_id, gig_id, points, reason) VALUES (?, ?, ?, ?)'
  ).run(userId, gigId, points, 'gig_completed');

  return { points, newBadges: refreshBadges(userId) };
}

function refreshBadges(userId) {
  const stats = getUserStats(userId);
  if (!stats) return [];

  const existing = new Set(
    db.prepare('SELECT badge_code FROM badges WHERE user_id = ?').all(userId).map((r) => r.badge_code)
  );

  const newly = [];
  for (const rule of BADGE_RULES) {
    if (!existing.has(rule.code) && rule.check(stats)) {
      db.prepare('INSERT INTO badges (user_id, badge_code) VALUES (?, ?)').run(userId, rule.code);
      newly.push({ code: rule.code, label: rule.label });
    }
  }
  return newly;
}

module.exports = { BADGE_RULES, pointsForCompletion, getUserStats, awardCompletion, refreshBadges };
