// src/auth.js
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');

const JWT_SECRET = process.env.JWT_SECRET || 'dev-secret-change-me';
const JWT_EXPIRY = '7d';

function hashPassword(plain) {
  return bcrypt.hashSync(plain, 10);
}
function verifyPassword(plain, hash) {
  return bcrypt.compareSync(plain, hash);
}
function signToken(user) {
  return jwt.sign({ sub: user.id, email: user.email }, JWT_SECRET, { expiresIn: JWT_EXPIRY });
}
function verifyToken(token) {
  return jwt.verify(token, JWT_SECRET);
}

module.exports = { hashPassword, verifyPassword, signToken, verifyToken };
