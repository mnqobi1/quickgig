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
`);

module.exports = db;
