// src/middleware/authenticate.js
const { verifyToken } = require('../auth');
const db = require('../db');

function readUserId(req) {
  const header = req.headers.authorization || '';
  const [scheme, token] = header.split(' ');
  if (scheme !== 'Bearer' || !token) return { error: 'Missing or malformed Authorization header' };
  try {
    const userId = Number(verifyToken(token).sub);
    // A deleted account's still-unexpired token must stop working immediately.
    if (!db.prepare('SELECT 1 FROM users WHERE id = ?').get(userId)) return { error: 'Account no longer exists' };
    return { userId };
  } catch (err) {
    return { error: 'Invalid or expired token' };
  }
}

/** Requires a valid Bearer token; sets req.userId. */
function authenticate(req, res, next) {
  const { userId, error } = readUserId(req);
  if (error) return res.status(401).json({ error });
  req.userId = userId;
  next();
}

/** Sets req.userId when a valid token is present, but lets anonymous requests through. */
function optionalAuthenticate(req, res, next) {
  const { userId } = readUserId(req);
  req.userId = userId ?? null;
  next();
}

module.exports = authenticate;
module.exports.authenticate = authenticate;
module.exports.optionalAuthenticate = optionalAuthenticate;
