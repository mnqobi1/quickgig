// server.js
const express = require('express');
const cors = require('cors');
const { PORT, HOST, GOOGLE_CLIENT_IDS } = require('./src/config');
const fcm = require('./src/fcm');
const rateLimit = require('./src/middleware/rateLimit');

const { router: authRoutes } = require('./src/routes/auth');
const gigRoutes = require('./src/routes/gigs');
const userRoutes = require('./src/routes/users');
const messageRoutes = require('./src/routes/messages');
const notificationRoutes = require('./src/routes/notifications');
const deviceRoutes = require('./src/routes/devices');

const app = express();
app.disable('x-powered-by');
app.use(cors());
app.use(express.json({ limit: '100kb' }));
// Express 5 leaves req.body undefined when there is no JSON body; handlers expect an object.
app.use((req, res, next) => {
  if (req.body === undefined) req.body = {};
  next();
});

app.get('/api/health', (req, res) => res.json({ status: 'ok', service: 'quickgig-api' }));

app.use('/api/auth', rateLimit({ windowMs: 60_000, max: 30 }), authRoutes);
app.use('/api/gigs', gigRoutes);
app.use('/api/users', userRoutes);
app.use('/api/gigs', messageRoutes);
app.use('/api/notifications', notificationRoutes);
app.use('/api/devices', deviceRoutes);

app.use((req, res) => res.status(404).json({ error: 'Not found' }));

// eslint-disable-next-line no-unused-vars
app.use((err, req, res, next) => {
  if (err.type === 'entity.parse.failed') return res.status(400).json({ error: 'Request body is not valid JSON' });
  if (err.type === 'entity.too.large') return res.status(413).json({ error: 'Request body too large' });
  console.error(err);
  res.status(500).json({ error: 'Internal server error' });
});

if (require.main === module) {
  app.listen(PORT, HOST, () => {
    console.log(`QuickGig API listening on http://${HOST}:${PORT}`);
    console.log(`  Google sign-in audience : ${GOOGLE_CLIENT_IDS.join(', ')}`);
    console.log(`  FCM background push     : ${fcm.isEnabled() ? 'enabled' : 'off (set FIREBASE_SERVICE_ACCOUNT to enable; SSE real-time still works)'}`);
  });
}

module.exports = app;
