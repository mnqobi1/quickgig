// src/fcm.js — Firebase Cloud Messaging (HTTP v1) sender, so notifications reach the phone even
// when the app is closed. Entirely optional: with no service account configured every call is a
// no-op, and the app still gets real-time notifications over SSE while it is open.
const fs = require('fs');
const db = require('./db');
const { FIREBASE_SERVICE_ACCOUNT } = require('../config');

let cached; // { auth, projectId } | null

function loadServiceAccount() {
  if (!FIREBASE_SERVICE_ACCOUNT) return null;
  const raw = FIREBASE_SERVICE_ACCOUNT.trim().startsWith('{')
    ? FIREBASE_SERVICE_ACCOUNT
    : fs.readFileSync(FIREBASE_SERVICE_ACCOUNT, 'utf8');
  return JSON.parse(raw);
}

function getSender() {
  if (cached !== undefined) return cached;
  try {
    const sa = loadServiceAccount();
    if (!sa) {
      cached = null;
    } else {
      const { GoogleAuth } = require('google-auth-library');
      cached = {
        projectId: sa.project_id,
        auth: new GoogleAuth({ credentials: sa, scopes: ['https://www.googleapis.com/auth/firebase.messaging'] }),
      };
    }
  } catch (e) {
    console.warn('FCM disabled — could not load FIREBASE_SERVICE_ACCOUNT:', e.message);
    cached = null;
  }
  return cached;
}

function isEnabled() {
  return getSender() !== null;
}

/** Push one notification to every registered device of a user. Never throws. */
async function pushToUser(userId, notification) {
  const sender = getSender();
  if (!sender) return { sent: 0 };
  const tokens = db.prepare('SELECT token FROM device_tokens WHERE user_id = ?').all(userId).map((r) => r.token);
  if (tokens.length === 0) return { sent: 0 };

  let sent = 0;
  try {
    const accessToken = await sender.auth.getAccessToken();
    for (const token of tokens) {
      // Data-only message: the app's FirebaseMessagingService always runs, so the notification can
      // be built in the user's chosen in-app language and respect their notification setting.
      const res = await fetch(`https://fcm.googleapis.com/v1/projects/${sender.projectId}/messages:send`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${accessToken}`, 'Content-Type': 'application/json' },
        body: JSON.stringify({
          message: {
            token,
            android: { priority: 'HIGH' },
            data: {
              id: String(notification.id),
              type: String(notification.type),
              title: String(notification.title),
              body: String(notification.body),
              gigId: notification.gigId == null ? '' : String(notification.gigId),
            },
          },
        }),
      });
      if (res.ok) {
        sent += 1;
      } else if (res.status === 404 || res.status === 400) {
        // Token no longer valid (app uninstalled / token rotated) — forget it.
        db.prepare('DELETE FROM device_tokens WHERE token = ?').run(token);
      }
    }
  } catch (e) {
    console.warn('FCM push failed:', e.message);
  }
  return { sent };
}

/** Test hook: forget the cached sender so env changes are re-read. */
function _reset() {
  cached = undefined;
}

module.exports = { isEnabled, pushToUser, _reset };
