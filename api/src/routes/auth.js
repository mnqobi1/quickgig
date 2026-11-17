// src/routes/auth.js
const express = require('express');
const db = require('../db');
const { hashPassword, verifyPassword, signToken } = require('../auth');

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
  };
}

router.post('/register', (req, res) => {
  const { name, email, password, preferredLanguage } = req.body;

  if (!name || !email || !password) {
    return res.status(400).json({ error: 'name, email and password are required' });
  }
  if (password.length < 8) {
    return res.status(400).json({ error: 'password must be at least 8 characters' });
  }

  const existing = db.prepare('SELECT id FROM users WHERE email = ?').get(email);
  if (existing) {
    return res.status(409).json({ error: 'An account with that email already exists' });
  }

  const isStudentEmail = /\.(ac\.za|edu)$/i.test(email.split('@')[1] || '');

  const info = db
    .prepare(
      `INSERT INTO users (name, email, password_hash, preferred_language, verification_status)
       VALUES (?, ?, ?, ?, ?)`
    )
    .run(name, email, hashPassword(password), preferredLanguage || 'en', isStudentEmail ? 'verified' : 'unverified');

  const user = db.prepare('SELECT * FROM users WHERE id = ?').get(info.lastInsertRowid);
  const token = signToken(user);
  res.status(201).json({ token, user: publicUser(user) });
});

router.post('/login', (req, res) => {
  const { email, password } = req.body;
  if (!email || !password) {
    return res.status(400).json({ error: 'email and password are required' });
  }

  const user = db.prepare('SELECT * FROM users WHERE email = ?').get(email);
  if (!user || !verifyPassword(password, user.password_hash)) {
    return res.status(401).json({ error: 'Invalid email or password' });
  }

  const token = signToken(user);
  res.json({ token, user: publicUser(user) });
});

router.post('/google', (req, res) => {
  const { email, name } = req.body;
  if (!email || !name) {
    return res.status(400).json({ error: 'email and name are required (stubbed SSO payload)' });
  }

  let user = db.prepare('SELECT * FROM users WHERE email = ?').get(email);
  if (!user) {
    const info = db
      .prepare(
        `INSERT INTO users (name, email, password_hash, verification_status)
         VALUES (?, ?, ?, 'verified')`
      )
      .run(name, email, hashPassword(`google-sso:${email}:${Date.now()}`));
    user = db.prepare('SELECT * FROM users WHERE id = ?').get(info.lastInsertRowid);
  }

  const token = signToken(user);
  res.json({ token, user: publicUser(user) });
});

module.exports = { router, publicUser };
