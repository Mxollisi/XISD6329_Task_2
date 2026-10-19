# Real-phone demonstration — voice-over script and shot list
**Assumed length: ~6–8 minutes (assumption — follow your lecturer's limit if one is given).** Record on a physical Android phone using the built APK (screen-record + voice-over; e.g. phone's screen recorder with mic, or record voice afterwards in an editor). Use fictional data only. Never show passwords, tokens, the Firebase console's secrets or other users' private data. Note device model/Android version on screen or in your evidence log.

| # | Shot | Voice-over (adapt to your own words) |
|---|---|---|
| 1 | Launch app, Home as guest | "This is Giant Auto Upholstery, a native Android app built in Kotlin with Jetpack Compose, running on my <device> with Android <version>." |
| 2 | Services: search "sofa", filter Leather/Auto, open a detail | "Customers can browse without an account. The catalogue is stored in a local SQLite database through Room." |
| 3 | Tap Book as guest → sign-in prompt | "Booking needs an account; the app remembers where I was going and returns me there after sign-in." |
| 4 | Register: show weak-password validation, then a valid account; show 'confirm your email' | "Authentication uses Firebase. Passwords are never stored in my database." |
| 5 | Login (show an invalid-credentials message), then **Continue with Google** | "This is single sign-on with Google through Android Credential Manager." |
| 6 | Home greeting, search, latest booking | |
| 7 | Booking form: submit invalid (no terms / today) → errors; then valid → reference | "Validation is shared Kotlin code and is enforced again in the repository. The date must be tomorrow or later in Johannesburg time." |
| 8 | Bookings Upcoming → details → Cancel → Past; Notifications + bell | "Booking and notification are written in one database transaction." |
| 9 | Profile ▸ Settings: change phone/name, theme Dark, toggle prefs; force-stop & reopen to show persistence | "Settings persist and the session is restored." |
| 10 | Help (FAQs), About, Contact (shows 'Saved on this device') | "Contact messages are saved locally and are not sent to the business in this version." |
| 11 | Developer tools: run fixtures → counts all ≥10 ✓ | "Every table has at least ten records." |
| 12 | Laptop: Android Studio Database Inspector showing the six tables | "Here is the data stored on the phone." Blur/skip anything private. |
| 13 | GitHub Actions run (green) + Azure DevOps board | "CI runs unit tests, lint and builds the APK; work items track progress." |
| 14 | Logout → guest | "Logging out returns to the guest experience." |

Editing/export: trim dead time, keep voice-over clear, export MP4 1080p H.264, check audio, and make sure no secrets/tokens/passwords are visible.
