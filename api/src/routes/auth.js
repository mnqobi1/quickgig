// src/routes/auth.js
const crypto = require('crypto');
const express = require('express');
const db = require('../db');
const { hashPassword, verifyPassword, signToken } = require('../auth');
const { verifyGoogleIdToken } = require('../google');
const { normalizeEmail, isValidEmail, isStudentEmail, LANGUAGES } = require('../util');

const router = express.Router();

function publicUser(user) {
  return {
    id: user.id,
    name: user.name,
    email: user.email,
    preferredLanguage: user.preferred_language,
    campusArea: user.campus_area,
    hustleScore: user.hustle_score,
    verificationStatus: user.verification_status,
    notificationsEnabled: user.notifications_enabled !== 0,
  };
}

router.post('/register', (req, res) => {
  const name = String(req.body.name || '').trim();
  const email = normalizeEmail(req.body.email);
  const password = req.body.password;
  const language = LANGUAGES.includes(req.body.preferredLanguage) ? req.body.preferredLanguage : 'en';

  if (!name || !email || !password) {
    return res.status(400).json({ error: 'name, email and password are required' });
  }
  if (name.length > 80) return res.status(400).json({ error: 'name is too long' });
  if (!isValidEmail(email)) return res.status(400).json({ error: 'email is not a valid address' });
  if (typeof password !== 'string' || password.length < 8) {
    return res.status(400).json({ error: 'password must be at least 8 characters' });
  }
  if (password.length > 72) return res.status(400).json({ error: 'password must be at most 72 characters' });

  if (db.prepare('SELECT id FROM users WHERE lower(email) = ?').get(email)) {
    return res.status(409).json({ error: 'An account with that email already exists' });
  }

  const info = db
    .prepare(
      `INSERT INTO users (name, email, password_hash, preferred_language, verification_status)
       VALUES (?, ?, ?, ?, ?)`
    )
    .run(name, email, hashPassword(password), language, isStudentEmail(email) ? 'verified' : 'unverified');

  const user = db.prepare('SELECT * FROM users WHERE id = ?').get(info.lastInsertRowid);
  res.status(201).json({ token: signToken(user), user: publicUser(user) });
});

router.post('/login', (req, res) => {
  const email = normalizeEmail(req.body.email);
  const password = req.body.password;
  if (!email || !password || typeof password !== 'string') {
    return res.status(400).json({ error: 'email and password are required' });
  }

  const user = db.prepare('SELECT * FROM users WHERE lower(email) = ?').get(email);
  if (!user || !verifyPassword(password, user.password_hash)) {
    return res.status(401).json({ error: 'Invalid email or password' });
  }
  res.json({ token: signToken(user), user: publicUser(user) });
});

/**
 * Google Sign-In (SSO). The app sends the ID token it got from Google; we verify its signature and
 * audience with Google's public keys and only then trust the email inside it. (The earlier stub
 * trusted a client-supplied email, which let anyone log in as anyone.)
 */
router.post('/google', async (req, res) => {
  const { idToken } = req.body;
  if (!idToken || typeof idToken !== 'string') {
    return res.status(400).json({ error: 'idToken is required' });
  }

  let profile;
  try {
    profile = await verifyGoogleIdToken(idToken);
  } catch (e) {
    return res.status(401).json({ error: 'Google sign-in could not be verified' });
  }
  if (!profile.emailVerified) {
    return res.status(401).json({ error: 'Your Google email address is not verified' });
  }

  let user = db.prepare('SELECT * FROM users WHERE google_sub = ?').get(profile.sub)
    || db.prepare('SELECT * FROM users WHERE lower(email) = ?').get(profile.email);

  if (user && !user.google_sub) {
    db.prepare('UPDATE users SET google_sub = ? WHERE id = ?').run(profile.sub, user.id); // link accounts
  } else if (!user) {
    const language = LANGUAGES.includes(req.body.preferredLanguage) ? req.body.preferredLanguage : 'en';
    const info = db
      .prepare(
        `INSERT INTO users (name, email, password_hash, preferred_language, verification_status, google_sub)
         VALUES (?, ?, ?, ?, ?, ?)`
      )
      // Random, never-disclosed password: this account signs in with Google only.
      .run(profile.name.slice(0, 80), profile.email, hashPassword(crypto.randomBytes(24).toString('hex')),
        language, isStudentEmail(profile.email) ? 'verified' : 'unverified', profile.sub);
    user = db.prepare('SELECT * FROM users WHERE id = ?').get(info.lastInsertRowid);
  }

  user = db.prepare('SELECT * FROM users WHERE id = ?').get(user.id);
  res.json({ token: signToken(user), user: publicUser(user) });
});

module.exports = { router, publicUser };
