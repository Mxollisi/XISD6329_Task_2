# Implementation status (honest matrix)
Legend: **Implemented** = source written · **Tested** = automated test executed and passed · **Manually verified** = checked on a real device/provider · **Blocked** = needs external setup/access.

Environment used to author this project had no Android SDK, Gradle, network, Firebase project or phone, therefore **nothing below is Tested or Manually verified yet**, and no APK was built.

| Feature | Implemented | Tested | Manually verified | Blocked / notes |
|---|---|---|---|---|
| Gradle/Compose/Hilt/Room project skeleton, manifest, resources | ✔ | ✘ | ✘ | Not compiled; versions must be confirmed on first sync. Gradle wrapper jar/scripts not generated (see README) |
| Room DB: 6 tables, FKs, indices, DAOs, transactions | ✔ | ✘ (tests written) | ✘ | Schema JSON is generated on first build — commit `app/schemas/` |
| Seed catalogue (10 services) + 10 FAQs | ✔ | ✘ | ✘ | Prices are demonstration values pending business confirmation |
| Debug fixtures (≥10 rows in all 6 tables) | ✔ | ✘ (test written) | ✘ | "10-record requirement met" may only be claimed after running fixtures and seeing counts |
| Email/password auth, verification gate, resend | ✔ | ✘ (VM tests written) | ✘ | **Blocked:** Firebase project + `google-services.json` |
| Google SSO (Credential Manager) | ✔ | ✘ | ✘ | **Blocked:** SHA-1/256 + OAuth client; provider-backed manual check required |
| Password reset email | ✔ | ✘ | ✘ | In-app completion of reset link **not implemented** (Firebase hosted page used) |
| Change password (reauth) | ✔ | ✘ | ✘ | Firebase required |
| Services list/search/filter/details | ✔ | ✘ (filter unit-tested) | ✘ | |
| Booking create/list/detail/cancel with transactions + idempotency | ✔ | ✘ (tests written) | ✘ | Local only, not sent to business (by design) |
| Notifications inbox + bell badge | ✔ | ✘ | ✘ | Local in-app/OS notifications only; no remote push/email delivery |
| Profile/settings/theme (Room + DataStore), permission handling API 33+ | ✔ | ✘ | ✘ | |
| Help (numbered guide + DB FAQs), About, Contact | ✔ | ✘ | ✘ | Business contact details unconfirmed |
| GitHub Actions workflow | ✔ | ✘ | ✘ | Needs repo + `GOOGLE_SERVICES_JSON` secret |
| Azure DevOps backlog template | ✔ | n/a | n/a | No real Azure items created/updated (no access) |
| Real-phone demo video | script ✔ | n/a | ✘ | **Blocked:** must be recorded by the student on a physical phone |
| Debug APK | ✘ | | | **Blocked:** no Android SDK in authoring environment — build in Android Studio |
