// src/db.js
const Database = require('better-sqlite3');
const path = require('path');

const DB_PATH = process.env.QUICKGIG_DB || path.join(__dirname, '..', 'quickgig.db');
const db = new Database(DB_PATH);

db.pragma('journal_mode = WAL');
db.pragma('foreign_keys = ON');

db.exec(`
CREATE TABLE IF NOT EXISTS users (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL,
  email TEXT NOT NULL UNIQUE,
  password_hash TEXT NOT NULL,
  phone TEXT,
  photo_url TEXT,
  preferred_language TEXT NOT NULL DEFAULT 'en',
  campus_area TEXT,
  hustle_score INTEGER NOT NULL DEFAULT 0,
  verification_status TEXT NOT NULL DEFAULT 'unverified',
  created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS badges (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  badge_code TEXT NOT NULL,
  unlocked_at TEXT NOT NULL DEFAULT (datetime('now')),
  UNIQUE(user_id, badge_code)
);

CREATE TABLE IF NOT EXISTS gigs (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  title TEXT NOT NULL,
  description TEXT,
  category TEXT NOT NULL,
  price_credits INTEGER NOT NULL DEFAULT 0,
  lat REAL,
  lng REAL,
  needed_by TEXT,
  status TEXT NOT NULL DEFAULT 'open',
  poster_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  assigned_to INTEGER REFERENCES users(id),
  created_at TEXT NOT NULL DEFAULT (datetime('now')),
  updated_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS gig_applications (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  gig_id INTEGER NOT NULL REFERENCES gigs(id) ON DELETE CASCADE,
  applicant_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  status TEXT NOT NULL DEFAULT 'pending',
  created_at TEXT NOT NULL DEFAULT (datetime('now')),
  UNIQUE(gig_id, applicant_id)
);

CREATE TABLE IF NOT EXISTS messages (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  gig_id INTEGER NOT NULL REFERENCES gigs(id) ON DELETE CASCADE,
  sender_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  recipient_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  body TEXT NOT NULL,
  created_at TEXT NOT NULL DEFAULT (datetime('now')),
  read_at TEXT
);

CREATE TABLE IF NOT EXISTS hustle_transactions (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  gig_id INTEGER REFERENCES gigs(id),
  points INTEGER NOT NULL,
  reason TEXT NOT NULL,
  created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS ratings (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  gig_id INTEGER NOT NULL REFERENCES gigs(id) ON DELETE CASCADE,
  rated_user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  stars INTEGER NOT NULL CHECK (stars BETWEEN 1 AND 5),
  created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

-- A worker pulling out of a gig they were assigned. This (not "the poster cancelled
-- the gig") is what counts as a late cancellation for the Reliable Runner badge.
CREATE TABLE IF NOT EXISTS gig_withdrawals (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  gig_id INTEGER NOT NULL REFERENCES gigs(id) ON DELETE CASCADE,
  user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

-- Notification inbox: every real-time push (SSE / FCM) is also stored here so a device
-- that was offline can catch up, and the app can show an unread badge.
CREATE TABLE IF NOT EXISTS notifications (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  type TEXT NOT NULL,
  title TEXT NOT NULL,
  body TEXT NOT NULL,
  gig_id INTEGER REFERENCES gigs(id) ON DELETE SET NULL,
  created_at TEXT NOT NULL DEFAULT (datetime('now')),
  read_at TEXT
);

-- Firebase Cloud Messaging registration tokens (one user can have several devices).
CREATE TABLE IF NOT EXISTS device_tokens (
  token TEXT PRIMARY KEY,
  user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  platform TEXT NOT NULL DEFAULT 'android',
  updated_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_gigs_status ON gigs(status);
CREATE INDEX IF NOT EXISTS idx_gigs_poster ON gigs(poster_id);
CREATE INDEX IF NOT EXISTS idx_gigs_assigned ON gigs(assigned_to);
CREATE INDEX IF NOT EXISTS idx_apps_applicant ON gig_applications(applicant_id);
CREATE INDEX IF NOT EXISTS idx_messages_gig ON messages(gig_id);
CREATE INDEX IF NOT EXISTS idx_notifications_user ON notifications(user_id, id);
`);

// ---- Idempotent migrations for databases created by an earlier version ----------------
function hasColumn(table, column) {
  return db.prepare(`PRAGMA table_info(${table})`).all().some((c) => c.name === column);
}
function addColumnIfMissing(table, column, ddl) {
  if (!hasColumn(table, column)) db.exec(`ALTER TABLE ${table} ADD COLUMN ${column} ${ddl}`);
}

addColumnIfMissing('users', 'notifications_enabled', 'INTEGER NOT NULL DEFAULT 1');
addColumnIfMissing('users', 'google_sub', 'TEXT');
// Client-generated ids make offline sync idempotent: if a request succeeded but its response was
// lost, the app's retry returns the existing row instead of creating a duplicate.
addColumnIfMissing('gigs', 'client_id', 'TEXT');
addColumnIfMissing('messages', 'client_id', 'TEXT');

db.exec('CREATE UNIQUE INDEX IF NOT EXISTS idx_users_google_sub ON users(google_sub) WHERE google_sub IS NOT NULL');
db.exec('CREATE UNIQUE INDEX IF NOT EXISTS idx_gigs_client ON gigs(poster_id, client_id) WHERE client_id IS NOT NULL');
db.exec('CREATE UNIQUE INDEX IF NOT EXISTS idx_messages_client ON messages(sender_id, client_id) WHERE client_id IS NOT NULL');
try {
  // Emails are matched case-insensitively. If an old database somehow already holds two
  // accounts that differ only by case this index can't be built; lookups still work.
  db.exec('CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email_lower ON users(lower(email))');
} catch (e) {
  console.warn('Could not create case-insensitive email index:', e.message);
}

module.exports = db;
