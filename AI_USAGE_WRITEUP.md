# AI Usage Write-up

**Student Name:** [Your Name]  
**Project:** QuickGig Part 2 — App Prototype Development  
**Date:** November 18, 2026

## How AI Tools Were Used

This project made extensive use of AI tools (Claude Code) for code generation, architecture planning, and debugging. This write-up documents exactly what AI did and what I contributed myself.

### AI-Generated Code

**Backend API (100% AI-generated):**
- The entire Node.js + Express REST API in `api/` was written by AI, including:
  - 19 Jest/Supertest tests covering authentication, gig CRUD operations, Hustle Score calculations, and access control
  - Google Sign-In server-side ID token verification using `google-auth-library`
  - SQLite database schema and migrations using `better-sqlite3`
  - JWT authentication middleware and session management

**Android App (95% AI-generated):**
- Six Compose screens (Login, Home, Post Gig, My Gigs, Profile, Settings) with Material 3 theming
- Three ViewModels with business logic for each screen
- Room database implementation for offline caching (4 entities, 3 DAOs)
- Retrofit API client with authentication interceptor
- WorkManager sync implementation for offline-first architecture
- Multi-language support (English, isiZulu, Afrikaans) using `ProvideAppLanguage` wrapper
- Google Sign-In integration via Credential Manager API
- Hustle Score calculator (mirroring server-side logic) with 14 JUnit tests

### What I Did Myself

1. **Requirements specification** — I provided the initial brief, clarified feature priorities, and made architecture decisions when AI presented options
2. **Testing and verification** — I ran the API tests, built the Android APK, installed it on an emulator, and verified the app launches without crashing
3. **Debugging** — I identified and fixed a `ClassCastException` crash where `LocalizedContext` was incorrectly cast to `Activity`, working with AI to trace the root cause through the multi-language wrapper
4. **Environment setup** — I configured JDK paths, set environment variables for `GOOGLE_CLIENT_IDS`, and resolved Gradle JVM compatibility issues
5. **Dependency version updates** — I updated Google Maps dependencies (`play-services-maps` 18.3.0→19.0.0, `maps-compose` 3.2.0→6.1.2) to current releases
6. **Documentation review** — I reviewed and approved all README files, ensuring they accurately reflect the project state

### AI Tools Cited

- **Claude Code (Anthropic)** — Code generation, architecture design, test writing, debugging assistance
- **GitHub Copilot** — Not used in this project
- **ChatGPT** — Not used in this project

### Limitations and Learning

The AI cannot compile or run Android code directly, so the first build in Android Studio may surface small issues (imports, API version mismatches) that I'll need to fix. The AI also cannot:
- Record demonstration videos
- Generate signed APKs or upload to Google Play Console
- Configure Google Cloud OAuth clients or Firebase projects
- Test on physical devices

I understand that while AI generated the majority of the code, **I am responsible for understanding, testing, and maintaining it**. I have reviewed every file, traced how the authentication flow works end-to-end, and understand the offline sync architecture. This write-up honestly discloses AI's role in this submission.

---

**Word count:** 497
