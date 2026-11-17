// server.js
const express = require('express');
const cors = require('cors');
require('dotenv').config();

const { router: authRoutes } = require('./src/routes/auth');
const gigRoutes = require('./src/routes/gigs');
const userRoutes = require('./src/routes/users');
const messageRoutes = require('./src/routes/messages');

const app = express();
app.use(cors());
app.use(express.json());

app.get('/api/health', (req, res) => res.json({ status: 'ok', service: 'quickgig-api' }));

app.use('/api/auth', authRoutes);
app.use('/api/gigs', gigRoutes);
app.use('/api/users', userRoutes);
app.use('/api/gigs', messageRoutes);

app.use((req, res) => res.status(404).json({ error: 'Not found' }));

// eslint-disable-next-line no-unused-vars
app.use((err, req, res, next) => {
  console.error(err);
  res.status(500).json({ error: 'Internal server error' });
});

const PORT = process.env.PORT || 3000;
if (require.main === module) {
  app.listen(3000, '0.0.0.0', () => {
    console.log('QuickGig API listening on port 3000');
  });
}

module.exports = app;
