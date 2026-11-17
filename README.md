# QuickGig — Part 2: App Prototype Development

Two independent pieces that talk to each other over HTTP:

```
quickgig-app/
├── api/        Node.js + Express + SQLite REST API — real, running, tested (19 passing tests)
└── android/    Kotlin + Jetpack Compose Android app — source complete, not yet compiled (see android/README.md)
```

## Quick start

```bash
# 1. Start the API
cd api
npm install
npm test        # 19 tests should pass
npm start       # http://localhost:3000

# 2. In Android Studio: open the android/ folder, let Gradle sync, run on an emulator
```

## Honesty about what's been verified

This sandbox has Node.js but not the Android SDK, an emulator, or
Android Studio. So:

- **`api/`** is fully real: I wrote it, installed its dependencies,
  started it as an actual process, and ran 19 Jest/Supertest tests
  against it — all passing. `curl http://localhost:3000/api/health`
  works right now if you run it.
- **The two Android files with zero Android-framework dependencies**
  (`HustleScoreCalculator.kt`, `SyncMerge.kt`) were compiled with a
  standalone Kotlin compiler and their 14 JUnit tests actually run and
  pass — not just written, genuinely executed.
- **The rest of the Android app** (six Compose screens, Room database,
  Retrofit networking, WorkManager sync, three-language string
  resources, navigation, theming, a real Gradle wrapper, a GitHub
  Actions workflow) is complete, carefully cross-checked source code —
  but I have never run the Android/Kotlin compiler or Android Gradle
  Plugin against it, because they aren't available to me here. Expect
  Android Studio's first sync to surface a handful of small fixes
  (an import, an API that shifted between library versions); that's
  normal for any first sync, not evidence of a deeper problem.

`android/README.md` has the full rubric-row-by-rubric-row mapping of
where each requirement is implemented, plus a "do this next" checklist
for everything that only you can do (run it, test it on a device, record
the demo, push your own incremental commits).

## On the AI Usage Write-up

Part 1 asked you to disclose how AI was used. Given how much of Part 2's
codebase was AI-written in this pass — effectively all of it, with you
still needing to build, test, debug, and understand it before it's
really "yours" — it's worth writing (or extending) that same kind of
honest disclosure for Part 2 too, even if your brief doesn't explicitly
ask for one this time.
