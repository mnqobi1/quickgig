// src/google.js — server-side verification of a Google Sign-In ID token.
//
// The Android app obtains an ID token from Google (Credential Manager) and POSTs it to
// /api/auth/google. We must NOT trust an email address sent by the client: anyone could claim
// to be anyone. Instead we verify the token's signature against Google's public keys and check
// that it was issued for *our* OAuth client.
const { OAuth2Client } = require('google-auth-library');
const { GOOGLE_CLIENT_IDS } = require('../config');

const client = new OAuth2Client();

/**
 * @param {string} idToken
 * @returns {Promise<{sub: string, email: string, emailVerified: boolean, name: string, picture?: string}>}
 * @throws if the token is invalid, expired, or minted for a different app
 */
async function verifyGoogleIdToken(idToken) {
  const ticket = await client.verifyIdToken({ idToken, audience: GOOGLE_CLIENT_IDS });
  const p = ticket.getPayload();
  if (!p || !p.sub || !p.email) throw new Error('Google token is missing required claims');
  return {
    sub: p.sub,
    email: String(p.email).toLowerCase(),
    emailVerified: p.email_verified === true,
    name: p.name || String(p.email).split('@')[0],
    picture: p.picture,
  };
}

module.exports = { verifyGoogleIdToken };
