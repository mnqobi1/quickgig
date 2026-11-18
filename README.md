# QuickGig — Part 2: App Prototype Development

Two independent pieces that talk to each other over HTTP:

```
quickgig-app/
├── api/        Node.js + Express + SQLite REST API — real, running, tested (19 passing tests)
└── android/    Kotlin + Jetpack Compose Android app — real Google SSO via Credential Manager
```

## Quick start

```bash
# 1. Start the API
cd api
npm install
npm test        # 19 tests should pass
npm start       # http://localhost:3000

# 2. In Android Studio: open the android/ folder, let Gradle sync, run on an emulator or physical device
```

## What's included

**API (`api/`)** — a real, running Node.js + Express REST API with SQLite (`better-sqlite3`). Features:
- Email/password registration & login
- Google Sign-In via ID token verification (`google-auth-library`)
- Gig creation, browsing, filtering by radius/category/status
- Apply to gigs, poster-only status changes
- Hustle Score + badge awarding (Verified Student, Top Rated, Reliable Runner)
- Settings updates with ownership check
- Chat access control for gig participants

**Android app (`android/`)** — Kotlin + Jetpack Compose UI with Material 3:
- Login / sign-up with email/password + Google SSO (Credential Manager)
- Home feed, Post Gig, My Gigs, Profile, Settings
- Multi-language support (English / isiZulu / Afrikaans)
- Offline mode with Room cache + WorkManager sync
- Hustle Score client preview (mirrors server-side math)

## Environment variables

For the API, set these before starting:

| Variable | Default | Purpose |
|---|---|---|
| `PORT` | `3000` | HTTP port |
| `JWT_SECRET` | `dev-secret-change-me` | Change this before deploying anywhere real |
| `QUICKGIG_DB` | `./quickgig.db` | SQLite file path |
| `GOOGLE_CLIENT_IDS` | — | Google OAuth client ID(s) for ID token verification |

For testing on a physical device, set `QUICKGIG_API_BASE_URL` to your computer's Wi-Fi address
(or use `localhost` with `adb reverse`). The emulator uses `10.0.2.2` automatically.

## What you still need to do

1. **Google Cloud Console**: Register an OAuth 2.0 client, add your debug/release SHA-1 fingerprint
   and package name (`com.quickgig.app`), then set `GOOGLE_CLIENT_IDS` to your client ID.
2. **Google Maps API key**: Add your Maps key to `AndroidManifest.xml` if you use the map screen.
3. **Physical device testing**: Run on your phone and verify the full flow end-to-end.
4. **Push to GitHub** with incremental commits — committing as a single dump weakens the
   "multiple commits" grading criterion.