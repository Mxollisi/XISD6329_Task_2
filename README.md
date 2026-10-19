# Giant Auto Upholstery — Native Android Application 🛋️🚗

Developed by **Mxo Mngomeni** for **Giant Auto Upholstery**, a premier South African upholstery restoration business. Customers can browse upholstery services, register and authenticate securely (Email/Password with verification gates + Google SSO), request and manage bookings, track an in-app notification inbox, customize appearance/settings, and utilize a dedicated **Cloud API & Sync Center**.

---

## 🚀 Key Features

- **Service Catalog & Search:** Browse professional upholstery services (sofas, leather repair, vehicle seats, caravan interiors) with real-time filtering by category and query.
- **Secure Authentication:** Email/password registration with strict validation rules, confirmation email gating, password reset, and **Google Single Sign-On (SSO)** via Android Credential Manager.
- **Booking Management:** Request bookings choosing preferred dates (Material Date Picker), hourly time slots (08:00–16:00), and delivery addresses. Track statuses (*Pending*, *Confirmed*, *Completed*, *Cancelled*) with transactional idempotency and cancellation controls.
- **Notifications Inbox:** Unread badge counters, status updates, and interactive notifications management.
- **Profile & Settings:** Edit personal details, toggle notification preferences, switch between system/light/dark theme modes, and manage passwords.
- **Cloud API & Sync Center:** Inspect API connection status, fetch remote catalogs, synchronize offline bookings with the cloud API, and review live HTTP response logs.
- **Help & Support:** Numbered usage guides, dynamic database FAQs, business info, and contact messaging.

---

## 🛠️ Tech Stack & Architecture

- **Language:** Kotlin 2.0.21 (Coroutines & Flows)
- **UI Toolkit:** Jetpack Compose & Material 3 (with adaptive Edge-to-Edge support)
- **Navigation:** Jetpack Navigation Compose (Single-Activity architecture)
- **Dependency Injection:** Dagger Hilt
- **Local Persistence:** Room over SQLite (7 normalized relational tables with Foreign Keys, Cascade deletes, indices, and safe v1→v2 migrations)
- **Preferences:** Jetpack DataStore Preferences (Theme modes & user preferences)
- **Identity & Auth:** Firebase Authentication SDK & Android Credential Manager (Google Sign-In) with offline development fallback
- **Networking & REST API:** Custom asynchronous REST API Client (`UpholsteryApiClient`) with generic response wrapping (`ApiResponse<T>`), HTTP status codes (`200 OK`, `201 Created`, `400 Bad Request`, `500 Server Error`), and mock latency simulation
- **Image Loading:** Coil Compose
- **Testing:** JUnit 4, Kotlin Coroutines Test, and AndroidX Test suites

---

## 📱 1. Open & Build in Android Studio

1. Install **Android Studio** (current stable), JDK 17, and SDK Platform 35.
2. **File ▸ Open** this root folder (`GiantAutoUpholstery`).
3. Let Gradle sync dependencies.
4. Run the `app` configuration on an emulator or physical device.

---

## 🔐 2. Firebase Setup (Optional for Live Auth)

The app includes a robust local development fallback for authentication. To connect live Firebase:
1. Go to the [Firebase Console](https://console.firebase.google.com/) and create a project.
2. Add an Android app with package name `za.co.giantautoupholstery`.
3. Generate your debug SHA-1 and SHA-256 fingerprints via Android Studio Terminal (`.\gradlew signingReport`) and add them to your Firebase project.
4. Enable **Email/Password** and **Google** under **Authentication ▸ Sign-in method**.
5. Download `google-services.json` and place it into the `app/` directory.
6. Sync Gradle.

---

## 🧪 3. Running Automated Tests

```bash
./gradlew testDebugUnitTest          # Unit tests (validation, filtering, ViewModels, repository logic)
./gradlew lintDebug                  # Android Lint checks
