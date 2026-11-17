# QuickGig API

The custom REST API for QuickGig (Part 1, "API and Database Design"),
implemented in Node.js + Express with a file-based SQLite database
(`better-sqlite3`), so it runs anywhere with no external DB service to
configure — clone it, `npm install`, `npm start`.

This satisfies the Part 2 rubric rows **"Integration of your REST API in
your app"** and **"Creation/use of the REST API"**: it's a real,
independently runnable and independently tested API that the Android app
(in `../android`) talks to over HTTP.

## Run it

```bash
npm install
npm start          # starts on http://localhost:3000
```

Health check: `GET /api/health` → `{"status":"ok"}`

## Run the tests

```bash
npm test
```

19 tests (Jest + Supertest): registration/validation, login, the Google
SSO stub, gig creation, radius-based feed filtering, the apply flow,
poster-only status changes, Hustle Score + badge awarding on completion,
settings updates with an ownership check, and chat access control. Tests
run against an isolated SQLite file (`tests/test.db`), never your dev
database.

## Endpoints

| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | `/api/auth/register` | — | Create an account (student-email domains like `.ac.za` auto-verify) |
| POST | `/api/auth/login` | — | Email + password login |
| POST | `/api/auth/google` | — | Google SSO stub (see note below) |
| GET | `/api/gigs` | — | List/filter gigs — `?lat&lng&radiusKm&category&status` |
| POST | `/api/gigs` | Y | Post a new gig |
| GET | `/api/gigs/:id` | — | Gig detail |
| POST | `/api/gigs/:id/apply` | Y | Apply to a gig |
| PATCH | `/api/gigs/:id/status` | Y (poster) | Assign / complete / cancel — completing awards Hustle Points |
| GET | `/api/users/:id` | — | Public profile: Hustle Score, badges, rating |
| PATCH | `/api/users/:id/settings` | Y (self) | Update profile/settings |
| GET | `/api/users/:id/hustle` | — | Score, badges, and point history |
| GET | `/api/gigs/:gigId/messages` | Y (participant) | Chat history for a gig |
| POST | `/api/gigs/:gigId/messages` | Y (participant) | Send a chat message |

Protected routes expect `Authorization: Bearer <token>`, issued by
register/login/google and validated in `src/middleware/authenticate.js`.

### Note on Google SSO

`POST /api/auth/google` demonstrates the SSO code path (issue a token,
create/find the user) without requiring a production Google Cloud OAuth
client. To make it real: register an OAuth client in Google Cloud
Console, verify the ID token server-side with `google-auth-library`
instead of trusting the posted email directly, and update the Android
app's SSO button to use Credential Manager / Google Identity Services
with that client ID.

## Hustle Score formula (`src/hustle.js`)

`points = 10 base + up to 10 scaled by the requester's star rating + 5 if on time`,
matching "weighted by the requester's rating, punctuality, and full
completion" from the Planning doc. Badges (`Verified Student`,
`Top Rated`, `Reliable Runner`) are re-checked after every completion —
see `BADGE_RULES` for the exact thresholds. The same formula is
duplicated (and independently unit-tested) in the Android app's
`gamification/HustleScoreCalculator.kt` for an instant client-side
preview; the server call is always the source of truth.

## Environment variables

| Variable | Default | Purpose |
|---|---|---|
| `PORT` | `3000` | HTTP port |
| `JWT_SECRET` | `dev-secret-change-me` | **Change this before deploying anywhere real** |
| `QUICKGIG_DB` | `./quickgig.db` | SQLite file path |
