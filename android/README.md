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
| Feature: sign in | `ui/login/` — email/password + a Google SSO stub (see note below) |
| Feature: Settings menu | `ui/settings/` — language, notifications, location-sharing, payment/credits, logout |
| Creation/use of the REST API | `data/remote/QuickGigApi.kt` (Retrofit interface) + `../api` (the server) |
| Integration of your REST API in your app | `data/repository/` — every repository calls through `QuickGigApi` |
| User Defined Feature 1 — Hustle Score (gamification) | `gamification/HustleScoreCalculator.kt` (client preview) + `api/src/hustle.js` (source of truth). **14 JUnit tests, actually compiled and run — see "What's actually verified" below.** |
| User Defined Feature 2 — Offline mode with sync | `data/local/` (Room cache + draft/outbox queues), `sync/SyncWorker.kt` (WorkManager), `sync/SyncMerge.kt` (conflict resolution, unit tested) |
| User Defined Feature 3 — Multi-language (English/isiZulu/Afrikaans) | `res/values*/strings.xml` + every screen uses `stringResource()`. `ui/AppLanguage.kt` (`ProvideAppLanguage`) makes those calls follow the language chosen **in the app** — on the login chips or in Settings — rather than the phone's language, so the whole UI, including the bottom nav, relabels immediately. The choice is stored on-device and survives sign-in, sign-out and restarts. |
| The app runs on a mobile device | You'll need Android Studio to build and run it — see "What I could not verify" below |
| User interface | Compose + Material 3, one consistent theme (`ui/theme/`) |
| Github, Readme and automated testing | This file, `.github/workflows/android-ci.yml`, `app/src/test/` (unit) and `app/src/androidTest/` (instrumented) |
| Demonstration video | Not something I can produce — see below |

## What's actually verified (not just written)

I don't have Android Studio, the Android SDK, or an emulator available to
me, so I can't compile or run the app itself here. What I *could* do,
and did:

- **`gamification/HustleScoreCalculator.kt` and `sync/SyncMerge.kt`** have
  zero Android dependencies, so I compiled them with a standalone Kotlin
  compiler and ran their 14 JUnit tests for real: `14 tests, OK`.
- **The API** (`../api`) is a real, running Node process — 19 Jest/Supertest
  tests pass against it, including the exact Hustle Score math
  (`10 + 10 + 5 = 25` for a 5-star, on-time completion) that
  `HustleScoreCalculator.kt` mirrors.
- The Gradle wrapper is real (fetched from Gradle's own GitHub repo, not
  hand-written), and `./gradlew --version` runs correctly — it only fails
  to *download* the Gradle distribution in this sandbox because
  `services.gradle.org` isn't reachable from here. It will download
  normally on your machine or in GitHub Actions.

## What I could not verify — do this next

1. **Open in Android Studio and let it sync.** Everything here is
   written to compile, but I have never run the Kotlin/Android compiler
   (`kotlinc` + the Android Gradle Plugin + `d8`) against the full app —
   only against the two dependency-free files above. Expect to fix a
   handful of small issues (an import, a Compose API that shifted
   between library versions) — normal for a first sync, not a sign
   anything is fundamentally wrong.
2. **Run the unit tests inside Android Studio** (`./gradlew testDebugUnitTest`) —
   should be an easy pass, since it's the same 14 tests I already ran
   standalone, run again inside Gradle.
3. **Run the instrumented tests** (`./gradlew connectedAndroidTest`, needs
   an emulator/device) — `app/src/androidTest/.../LoginScreenTest.kt`.
4. **Record the demonstration video and take your screenshots** — only
   possible from a real running build.
5. **Make the Google SSO real**, if your rubric needs more than the code
   path demonstrated: register an OAuth client in Google Cloud Console
   and swap the demo button in `LoginScreen.kt` for Credential Manager /
   Google Identity Services (see the note in `../api/README.md`).
6. **Push to GitHub yourself, with real incremental commits** — I
   deliberately haven't touched git here. "Multiple commits" is a grading
   criterion for a reason: it's evidence of your own process, not just a
   final artifact. Committing this as a single dump would undercut the
   thing that line is meant to show.

## Known simplifications (documented, not hidden)

- **Google SSO** is a stub matching the code path, not a production
  OAuth integration (needs your own Google Cloud credentials — see above).
- **Push notifications**: Firebase Cloud Messaging isn't wired up (needs
  your own Firebase project); the WorkManager-based sync trigger is real
  and independent of FCM.
- **Gig Detail / Chat UI**: the API fully supports applying and chatting
  (`GET/POST /api/gigs/:gigId/messages`), and `MessageRepository` is
  wired for it, but there's no dedicated Gig Detail screen in this pass —
  `onOpenGig` in `QuickGigNavHost.kt` is a no-op you can extend.
- **Location** is a fixed demo coordinate (Cape Town), not read from
  `FusedLocationProviderClient`, to keep this pass focused on the
  rubric's core features.
- **isiZulu and Afrikaans strings** are a good-faith translation for a
  student demo, not a professional localisation — worth a native
  speaker's review before you'd ship it for real.
