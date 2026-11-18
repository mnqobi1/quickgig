// src/middleware/rateLimit.js — tiny in-memory limiter for the auth endpoints (brute-force guard).
// Not a substitute for a real gateway in production, but it stops trivial password guessing.
function rateLimit({ windowMs = 60_000, max = 30 } = {}) {
  const hits = new Map(); // ip -> { count, resetAt }
  return (req, res, next) => {
    if (process.env.NODE_ENV === 'test') return next();
    const now = Date.now();
    const key = req.ip;
    let entry = hits.get(key);
    if (!entry || entry.resetAt <= now) {
      entry = { count: 0, resetAt: now + windowMs };
      hits.set(key, entry);
    }
    entry.count += 1;
    if (entry.count > max) {
      res.set('Retry-After', String(Math.ceil((entry.resetAt - now) / 1000)));
      return res.status(429).json({ error: 'Too many attempts — please wait a minute and try again' });
    }
    next();
  };
}
module.exports = rateLimit;
