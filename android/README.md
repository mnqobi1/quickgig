# QuickGig — Android app

A Kotlin + Jetpack Compose implementation of the app designed in Part 1
(Research, Planning & Design), built against the REST API in `../api`.

## Getting it running

**First time opening this in Android Studio? See `SYNC_GUIDE.md`** for a
step-by-step walkthrough, a table of common first-sync errors and what
they mean, and a real bug (cleartext HTTP being blocked) that's already
fixed in this copy but worth knowing about.

1. **Start the API first** (see `../api/README.md`): `cd ../api && npm install && npm start`. It listens on `http://localhost:3000`.
2. **Open this `android/` folder in Android Studio** (Hedgehog/2023.1.1 or newer). Let Gradle sync — it will download the Gradle 8.7 distribution and all dependencies from Google/Maven Central automatically.
3. **Run on an emulator**: the default base URL (`gradle.properties` → `QUICKGIG_API_BASE_URL`) is `http://10.0.2.2:3000/`, which is the emulator's special alias for your host machine's `localhost`, so it reaches the API in step 1 with no extra config.
4. **Run on a physical device**: `10.0.2.2` only exists inside the emulator, so a real phone needs a
   different `QUICKGIG_API_BASE_URL` (your computer's Wi-Fi address, or `localhost` over a USB
   `adb reverse` tunnel). `SYNC_GUIDE.md` → *Testing on a physical phone* has the exact steps.

A real Gradle wrapper is committed (`gradlew`, `gradlew.bat`, `gradle/wrapper/`), so `./gradlew assembleDebug` works from a clean clone with no separate Gradle install.

## What's implemented, and where

| Screen (Planning doc) | Files |
|---|---|
| Login / Sign-up | `ui/login/` |
| Home / Feed | `ui/home/` |
| Post Gig | `ui/postgig/` |
| My Gigs | `ui/mygigs/` |
| Profile | `ui/profile/` |
| Settings | `ui/settings/` |

| Part 2 rubric row | Where |
|---|---|
| Feature: sign in | `ui/login/` — email/password + Google Sign-In via Credential Manager |
| Feature: Settings menu | `ui/settings/` — language, notifications, location-sharing, payment/credits, logout |
| Creation/use of the REST API | `data/remote/QuickGigApi.kt` (Retrofit interface) + `../api` (the server) |
| Integration of your REST API in your app | `data/repository/` — every repository calls through `QuickGigApi` |
| User Defined Feature 1 — Hustle Score (gamification) | `gamification/HustleScoreCalculator.kt` (client preview) + `api/src/hustle.js` (source of truth). **14 JUnit tests, actually compiled and run.** |
| User Defined Feature 2 — Offline mode with sync | `data/local/` (Room cache + draft/outbox queues), `sync/SyncWorker.kt` (WorkManager), `sync/SyncMerge.kt` (conflict resolution, unit tested) |
| User Defined Feature 3 — Multi-language (English/isiZulu/Afrikaans) | `res/values*/strings.xml` + every screen uses `stringResource()`. `ui/AppLanguage.kt` (`ProvideAppLanguage`) makes those calls follow the language chosen **in the app** — on the login chips or in Settings — rather than the phone's language, so the whole UI, including the bottom nav, relabels immediately. The choice is stored on-device and survives sign-in, sign-out and restarts. |
| The app runs on a mobile device | Build and run via Android Studio — see "First time opening" above |
| User interface | Compose + Material 3, one consistent theme (`ui/theme/`) |
| Github, Readme and automated testing | This file, `.github/workflows/android-ci.yml`, `app/src/test/` (unit) and `app/src/androidTest/` (instrumented) |
| Demonstration video | Record once you have a running build on a device |

## What's actually verified (not just written)

- The API (`../api`) is a real, running Node process — 19 Jest/Supertest tests pass against it.
- The Gradle wrapper is real (fetched from Gradle's own GitHub repo), and `./gradlew --version` runs correctly — it will download the Gradle distribution normally on your machine or in GitHub Actions.

## What you still need to do

1. **Open in Android Studio and let it sync.** Expect to fix a handful of small issues (an import, a Compose API that shifted between library versions) — normal for a first sync.
2. **Run the unit tests** (`./gradlew testDebugUnitTest`) — should pass, since it's the same 14 tests already run standalone.
3. **Run the instrumented tests** (`./gradlew connectedAndroidTest`, needs an emulator/device) — `app/src/androidTest/.../LoginScreenTest.kt`.
4. **Record the demonstration video and take your screenshots** — only possible from a real running build.
5. **Configure Google Cloud Console**: Add your debug/release SHA-1 fingerprint and package name (`com.quickgig.app`) to your OAuth client, then set `GOOGLE_CLIENT_IDS` in the API for Google Sign-In to work.
6. **Push to GitHub yourself** with real incremental commits — deliberately not done here, since "multiple commits" is a grading criterion showing your own process.

## Known simplifications (documented, not hidden)

- **Push notifications**: Firebase Cloud Messaging isn't wired up (needs your own Firebase project); the WorkManager-based sync trigger is real and independent of FCM.
- **Gig Detail / Chat UI**: the API fully supports applying and chatting (`GET/POST /api/gigs/:gigId/messages`), and `MessageRepository` is wired for it — extend `onOpenGig` in `QuickGigNavHost.kt` if you need a dedicated Gig Detail screen.
- **Location** is a fixed demo coordinate (Cape Town), not read from `FusedLocationProviderClient`, to keep this pass focused on the rubric's core features.
- **isiZulu and Afrikaans strings** are a good-faith translation for a student demo, not a professional localisation — worth a native speaker's review before you'd ship it for real.