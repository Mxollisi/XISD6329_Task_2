# Giant Auto Upholstery — native Android app (Kotlin · Jetpack Compose · Room/SQLite · Firebase Auth)

Task 2 submission project for **Mxo Mngomeni**. Customers browse upholstery services, register/sign in (email + Google SSO),
request bookings, track/cancel them, read an in-app notification inbox, manage settings, and use Help/About/Contact.

> **Honest status:** this source tree was written in an environment with **no Android SDK, Gradle, network or device**.
> It has **not been compiled, unit-tested, run on a phone, or connected to Firebase yet**. See `docs/STATUS.md` for the
> implemented / tested / manually-verified / blocked matrix. Expect to fix a few compile/version issues on first sync —
> that is normal; the architecture, tests and docs are in place to make that quick.

## Tech stack
Kotlin 2.0.21 · AGP 8.7.3 · Gradle 8.9 · JDK 17 · Compose BOM 2024.10.01 + Material 3 · Navigation Compose (single activity) ·
MVVM + StateFlow · Hilt · **Room over SQLite** (authoritative app data, 6 tables) · DataStore (device theme) ·
**Firebase Authentication only** (identity) + Credential Manager Google sign-in · Coil · JUnit/coroutines-test/Compose UI test.
minSdk 26, compile/target SDK 35 (versions pinned in `gradle/libs.versions.toml`; bump together if Android Studio suggests newer stable ones).

## 1. Open and build
1. Install **Android Studio (current stable)**, JDK 17 (bundled JBR is fine) and SDK Platform 35.
2. `File ▸ Open` this folder. If `gradle/wrapper/gradle-wrapper.jar` is missing, Android Studio will use its own Gradle, or run
   `gradle wrapper --gradle-version 8.9` once and commit `gradlew`, `gradlew.bat` and the jar.
3. Add Firebase config (section 2) → **Sync** → run the `app` configuration on a phone/emulator.

## 2. Firebase setup (required for sign-in)
1. console.firebase.google.com → create a project → **Add Android app**, package `za.co.giantautoupholstery`.
2. Add your **debug SHA-1 and SHA-256** (`./gradlew signingReport`) — required for Google sign-in. Add release fingerprints later.
3. **Authentication ▸ Sign-in method**: enable **Email/Password** and **Google**. Under Settings ▸ Password policy you may require
   8+ chars, upper case and numbers to match the app rules.
4. Customise the email-verification and password-reset templates if you wish.
5. Download `google-services.json` into `app/` (it is git-ignored). Never commit it if your repo is public; use the CI secret below.
6. The Google plugin generates `R.string.default_web_client_id` from the file (needs the Web client the console creates automatically).

**Password recovery limitation (honest):** Firebase sends the reset email and completes the reset on its hosted web page in the
browser; the app then signs in normally. In-app completion of `oobCode` links needs a custom action-URL domain with Android App Links —
documented as future work, not implemented.

## 3. GitHub Actions
`.github/workflows/android-ci.yml` runs on PRs/pushes: unit tests → lint → `assembleDebug` → uploads APK, test and lint reports,
then an emulator job runs `connectedDebugAndroidTest`. Add repository secret **`GOOGLE_SERVICES_JSON`** (file contents) for a working-auth
APK; without it CI uses `google-services.json.example` (builds, but sign-in is non-functional). No keystore/secrets are ever committed.

## 4. Development-only data fixture (10 records per table)
Debug builds only: sign in → **Profile ▸ Settings ▸ Developer tools** → *Run fixture setup* → *Show table counts*. It transactionally and
idempotently inserts fictional rows (simulated profiles `SIMULATED-PROFILE-NN` cannot log in) and attaches 6 bookings / 5 notifications / 3
contact messages to *your* UID so private lists can be shown. *Clean up fixtures* removes only fixture rows. Production/release builds
never show these tools or seed private data. Catalogue (10 services) and FAQs (10) are labelled demonstration reference data seeded
idempotently on launch. Verify with Android Studio **App Inspection ▸ Database Inspector** (`giant_auto_upholstery.db`).

## 5. Run tests
```
./gradlew testDebugUnitTest          # validation, rand format, upcoming/past, filtering, ViewModels
./gradlew lintDebug
./gradlew connectedDebugAndroidTest  # needs emulator/phone: Room DAO/FK/unique, repos, isolation, fixtures >=10, persistence, Compose
```

## 6. Install the APK on a phone
Debug APK: `app/build/outputs/apk/debug/app-debug.apk` → enable *USB debugging*, `adb install -r app-debug.apk` (or copy the file and open it,
allowing "install unknown apps"). Release builds: see `docs/RELEASE.md`.

## Documentation index
`docs/ARCHITECTURE.md` (layers, ER diagram, data dictionary, security limits) · `docs/TEST_PLAN.md` (automated + manual acceptance checklist) ·
`docs/USER_GUIDE.md` · `docs/DEMO_SCRIPT.md` (voice-over + shot list) · `docs/AZURE_DEVOPS.md` + `docs/azure-devops-backlog.csv` ·
`docs/RELEASE.md` · `docs/STATUS.md` · `docs/EVIDENCE_CHECKLIST.md`.

## Known limitations (by design of this version)
* SQLite is **per installation**: no sync between phones; uninstall/clear-storage deletes data; no staff dashboard.
* Bookings and contact messages are **saved on the device only — not sent to the business** (the UI says so).
* Client-side account isolation is not tamper-proof on rooted/modified devices.
* "Email notifications" is a saved preference only; no email/remote-push delivery is configured. Device notifications are *local* OS notifications.
* Fraunces/Manrope fonts are not bundled (licence/files needed) — Serif/default fallbacks are used. No genuine service photos were supplied: accessible icon fallbacks are shown.
* UI strings are hard-coded English (no localisation yet). Business address/hours/phone/email are unconfirmed and shown as "To be confirmed".
