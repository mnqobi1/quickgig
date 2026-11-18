// src/config.js — one place for every environment-driven setting.
require('dotenv').config({ quiet: true });

const isProduction = process.env.NODE_ENV === 'production';

const DEV_JWT_SECRET = 'dev-secret-change-me';
const JWT_SECRET = process.env.JWT_SECRET || DEV_JWT_SECRET;
if (isProduction && JWT_SECRET === DEV_JWT_SECRET) {
  // Refuse to run a public server that signs tokens with a secret published in the source.
  throw new Error('JWT_SECRET must be set when NODE_ENV=production');
}

// The OAuth *web* client ID from Firebase / Google Cloud (client_type 3 in google-services.json).
// The Android app asks Google for an ID token whose audience is this ID; the API only accepts
// tokens minted for it.
const DEFAULT_GOOGLE_CLIENT_ID = '785348177066-9hdkgurrtnofhejbc9bfk4cjm6cnhg3n.apps.googleusercontent.com';
const GOOGLE_CLIENT_IDS = (process.env.GOOGLE_CLIENT_ID || DEFAULT_GOOGLE_CLIENT_ID)
  .split(',')
  .map((s) => s.trim())
  .filter(Boolean);

module.exports = {
  isProduction,
  JWT_SECRET,
  JWT_EXPIRY: '7d',
  PORT: Number(process.env.PORT) || 3000,
  HOST: process.env.HOST || '0.0.0.0',
  GOOGLE_CLIENT_IDS,
  // Optional: path to (or inline JSON of) a Firebase service-account key. Without it the API
  // still delivers real-time notifications over SSE; FCM push to closed apps is simply off.
  FIREBASE_SERVICE_ACCOUNT: process.env.FIREBASE_SERVICE_ACCOUNT || '',
  SSE_HEARTBEAT_MS: Number(process.env.SSE_HEARTBEAT_MS) || 15000,
};
