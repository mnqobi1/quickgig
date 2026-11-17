# Getting QuickGig building in Android Studio

This is the guide for the very first thing you'll do with this project:
open it, sync it, and get it running on an emulator talking to the real
API. I found and fixed one real bug while writing this (see below) —
that's the kind of thing I can only catch by reasoning through the setup
carefully, not by running it, since I don't have Android Studio here.

## Bug found and fixed while preparing this guide

**Every network call would have silently failed.** `targetSdk 34` makes
Android block plain-HTTP ("cleartext") traffic by default, and the API
runs on plain `http://` for local dev — so without an exception, every
Retrofit call would throw `CLEARTEXT communication to ... not permitted`.
Fixed with a network security config, already applied:

- **Release builds** (`src/main/res/xml/network_security_config.xml`) only
  allow plain HTTP to `10.0.2.2`, `localhost` and `127.0.0.1` — the
  addresses the emulator and a USB tunnel use.
- **Debug builds** (`src/debug/res/xml/network_security_config.xml`, which
  Android merges over the one above) allow plain HTTP to any address, because
  testing on a real phone over Wi-Fi means reaching your computer at an address
  like `192.168.1.50` that can't be listed in advance. Debug builds only ever
  talk to your own development API.

## Prerequisites

- **Android Studio** — Iguana (2023.2) or newer. Anything recent bundles
  its own JDK 17, which is what this project targets, so you shouldn't
  need to install a JDK separately.
- **An internet connection for the first sync** — Gradle needs to
  download the Android Gradle Plugin, Kotlin, and every library
  (Compose, Room, Retrofit, etc.) from Google's and Maven Central's
  servers. This is the one thing I couldn't do for you: I built this in
  a sandbox that can't reach those servers, so this project has never
  actually been synced by any copy of Gradle. The wrapper itself
  (`gradlew`, the jar, `gradle-wrapper.properties`) is real and correct —
  I confirmed that much — but the distribution download and the full
  dependency graph are untested.
- **Android SDK Platform 34** — if it's missing, Android Studio will
  offer to install it automatically the moment you open the project;
  just accept the prompt.

## Step by step

1. **Unzip** `QuickGig_Part2_App_Prototype.zip` somewhere permanent (not
   `Downloads`, so you don't lose track of it).
2. **Open Android Studio → Open → select the `android/` folder** (not
   the outer `quickgig-app` folder — `android/` is the actual Gradle
   project root, identifiable by `settings.gradle.kts` sitting directly
   inside it).
3. **Let it sync.** The status bar at the bottom shows progress. First
   sync on a fresh machine can take several minutes (downloading
   Gradle 8.7 itself, then every dependency). Don't interrupt it.
4. **If it asks to install anything** (SDK Platform 34, a missing
   cmdline-tools component) — accept. That's normal, not an error.
5. **Create a device to run on**, if you don't have one: Device Manager
   (phone-with-a-plus icon in the top toolbar) → Create Device → any
   phone profile → a system image for **API 34** (download it if
   prompted) → Finish.
6. **Start the API** in a terminal: `cd api && npm install && npm start`
   — leave this running.
7. **Run the app**: green ▶ Run button, pick your emulator, wait for it
   to boot and install. You should land on the Login screen.
8. **Register an account** in the app (any email/password 8+ characters)
   and confirm you land on the Home feed. If that works, the app is
   correctly talking to the API — the hardest part is done.

## The app runs but says `Failed to connect to /10.0.2.2:3000`

The app built, installed and is correctly *trying* to reach the API — but
nothing is answering on port 3000 on your computer. (If the cleartext fix
above weren't working you'd see a different message: `CLEARTEXT communication
... not permitted`.) Nearly always, the API simply isn't running:

1. In a terminal inside the `api` folder: `npm install`, then `npm start`.
   You should see `QuickGig API listening on http://localhost:3000`. Leave
   that window open.
2. On your computer, open <http://localhost:3000/api/health> in a browser —
   expect `{"status":"ok","service":"quickgig-api"}`.
3. In the **emulator's** Chrome, open <http://10.0.2.2:3000/api/health> — the
   same JSON means the app can reach it too. Then retry in the app.

If it still fails:

- **`npm install` errors mentioning `node-gyp`, Python or Visual Studio** —
  the `better-sqlite3` database module couldn't find a prebuilt binary for
  your Node version (usually because it's the newest "Current" release).
  Install the **LTS** version from nodejs.org, delete `api/node_modules`,
  and run `npm install` again.
- **`EADDRINUSE`** — something else is already using port 3000. Start the
  API on another port (PowerShell: `$env:PORT=3001; npm start`), then set
  `QUICKGIG_API_BASE_URL=http://10.0.2.2:3001/` in `gradle.properties` and
  sync again.

## If sync fails — reading the actual error

Android Studio's error messages are usually specific; the fix is almost
always in the text itself. A few you're likely to see with a from-source
project like this one, and what they mean:

| Message contains | What it means | Fix |
|---|---|---|
| `SDK location not found` | Android Studio hasn't been pointed at an SDK yet | File → Project Structure → SDK Location, or let the "Open" wizard's first-run setup handle it |
| `Failed to find target with hash string "android-34"` | SDK Platform 34 isn't installed | Tools → SDK Manager → check "Android 14.0 (API 34)" → Apply |
| `Unsupported Java` / JDK-related | Android Studio is pointed at an old JDK | File → Settings → Build Tools → Gradle → Gradle JDK → pick 17 (or "jbr-17" if listed) |
| `Could not resolve` + a library name | A network hiccup, a proxy, or a firewall blocking `dl.google.com` / `repo.maven.apache.org` | Retry sync (File → Sync Project with Gradle Files); check you're not on a restrictive network/VPN |
| `Out of memory` during sync/build | Gradle daemon ran out of heap | Add `org.gradle.jvmargs=-Xmx4096m` to `gradle.properties` (it currently requests 2048m) |
| Anything mentioning `KSP` and a version mismatch | Kotlin and KSP plugin versions drifted apart | They're pinned together in `build.gradle.kts` (`1.9.24` / `1.9.24-1.0.20`) — don't bump one without the other |

If you hit something not in this table, paste the actual error text back
to me — I can reason about a specific error message far better than I
can guess at what "might" go wrong in the abstract.

## Testing on a physical phone instead of an emulator

`10.0.2.2` is an address that only exists **inside the emulator**. On a real
phone it leads nowhere, so the app waits 10 seconds and shows:

> `failed to connect to /10.0.2.2 (port 3000) from /192.168.1.107 (port 43272) after 10000ms`

The "from" address is the phone's own Wi-Fi address — that's how you can tell
the app is running on a real device. The fix is to change where the app looks
for the API. The one setting is `QUICKGIG_API_BASE_URL` in `gradle.properties`
(change it, click **Sync Now**, run the app again):

| Where the app runs | `QUICKGIG_API_BASE_URL` |
|---|---|
| Emulator | `http://10.0.2.2:3000/` |
| Phone over Wi-Fi | `http://<your computer's Wi-Fi address>:3000/` |
| Phone over USB (`adb reverse`) | `http://localhost:3000/` |

### Option A — phone over Wi-Fi

1. Put the phone and the computer on the **same Wi-Fi** (not mobile data, and
   not a guest network).
2. Find the computer's address: open a terminal (File Explorer → click the
   address bar → type `cmd` → Enter), run `ipconfig`, and read the
   **IPv4 Address** under "Wireless LAN adapter Wi-Fi" — it looks like
   `192.168.1.50`.
3. Start the API (`npm start` in the `api` folder) and let it through the
   Windows firewall. The first time Node runs, Windows asks; tick **Private
   networks** and Allow. If you missed it, open an *Administrator* terminal
   (Start → type `cmd` → right-click → Run as administrator) and run:
   `netsh advfirewall firewall add rule name="QuickGig API" dir=in action=allow protocol=TCP localport=3000`
4. **Test before touching the app**: on the phone, open Chrome and go to
   `http://192.168.1.50:3000/api/health` (use your address). Seeing
   `{"status":"ok",...}` means the phone can reach the API. If it doesn't load,
   the problem is the firewall or the Wi-Fi — not the app. (Campus and guest
   Wi-Fi often block devices from talking to each other; use Option B there.)
5. Set `QUICKGIG_API_BASE_URL=http://192.168.1.50:3000/` — keep the `http://`
   and the trailing `/` — then Sync Now and run again.

Your computer's address can change when you reconnect to Wi-Fi; if the app
stops connecting later, run `ipconfig` again and update the line.

### Option B — phone over USB (works on any Wi-Fi)

1. Connect the phone by USB with USB debugging on.
2. In a terminal, run (cmd):
   `"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" reverse tcp:3000 tcp:3000`
   This tunnels the phone's `localhost:3000` to your computer's
   `localhost:3000`. Run it again each time you re-plug the phone.
3. Set `QUICKGIG_API_BASE_URL=http://localhost:3000/`, Sync Now, run again.
   (An earlier version of this guide said you didn't need to change anything
   for this option. That was wrong: `adb reverse` doesn't make `10.0.2.2`
   work, it makes `localhost` work.)

## Running the tests from inside Android Studio

- **Unit tests** (the 14 I already verified standalone, plus anything
  else under `app/src/test/`): right-click `app/src/test/java` →
  Run Tests, or `./gradlew testDebugUnitTest` in the terminal panel.
- **Instrumented tests** (`LoginScreenTest.kt`): need an emulator/device
  running — right-click `app/src/androidTest/java` → Run Tests, or
  `./gradlew connectedAndroidTest`.
