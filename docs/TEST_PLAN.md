# Test plan and acceptance checklist

## Automated (what exists)
| Area | Test | Type |
|---|---|---|
| Validation boundaries (name 2–80, email, password rules, address 5–200, description ≤500, phone regex, contact 3–120/10–1000) | `ValidationTest` | unit |
| Booking date = tomorrow+ in Africa/Johannesburg, midnight boundary (21:59:59Z vs 22:00:00Z), device-timezone independence, times 08:00–16:00 | `ValidationTest` | unit |
| Rand formatting (`R1 200`), initials, reference format, Upcoming/Past classification + ordering, service search/filter | `FormattingAndRulesTest` | unit |
| ViewModel Loading/Content/Empty/Error (services), login/register validation & safe errors | `ViewModelTest` | unit |
| DAO insert/read/update/delete, FK violation, unique reference, owner-scoped notifications | `DatabaseAndRepositoryTest` | instrumented (in-memory SQLite) |
| Booking create + read-back + notification, idempotent submission, invalid date writes nothing, account A/B isolation, cancel Pending only, no cancel of Confirmed, welcome notification once | `DatabaseAndRepositoryTest` | instrumented |
| Fixtures: ≥10 rows in all six tables, idempotent, scoped cleanup | `DatabaseAndRepositoryTest` | instrumented |
| Persistence across close/reopen of the database file | `DatabaseAndRepositoryTest` | instrumented |
| Compose: service card, empty state, password-visibility toggle accessibility | `ServiceCardUiTest` | instrumented |

## Not yet automated (gaps — do not claim coverage)
Migration tests (none needed until v2), navigation-guard/restore-after-sign-in UI test, full booking-flow Compose test (needs Hilt test runner + fake auth),
settings/logout UI tests, transaction-rollback fault injection, small/large phone and rotation checks, TalkBack pass.

## Needs a provider-backed manual check (mocks cannot prove these)
Email verification delivery, password-reset email delivery, Google SSO (SHA fingerprints/OAuth client), token refresh/expiry behaviour.

## Manual acceptance checklist (record device model, Android version, date, result)
1. Fresh install → Home as guest → browse/search/filter services, open details.  ☐
2. Tap *Book this service* as guest → sent to sign-in → after sign-in lands on the **same** booking form.  ☐
3. Register with weak password → inline errors; register valid → "confirm your email" screen; resend works; confirm via email; "I've confirmed" signs in.  ☐
4. Login wrong password → safe generic error; correct → Home greeting with first name.  ☐
5. Continue with Google → signed in; Settings shows no password section.  ☐
6. Booking: invalid (today's date / no terms) shows field errors; valid submit shows reference `GAU-XXXXXX`; appears under Bookings ▸ Upcoming; notification + bell badge.  ☐
7. Cancel Pending (confirm dialog) → status Cancelled, moves to Past, cancellation notification; cannot cancel again.  ☐
8. Notifications: mark one/all read, delete one.  ☐
9. Settings: edit name/phone/address (invalid phone rejected), toggle prefs, theme System/Light/Dark; **force-stop and reopen → persisted, still signed in**.  ☐
10. Change password (wrong current password rejected); forgot-password email arrives.  ☐
11. Debug fixture: run → counts all ≥10 (✓ marks) → verify in Database Inspector → clean up.  ☐
12. Airplane mode: catalogue, FAQs, local booking/contact saves work; login shows offline message.  ☐
13. Rotate during booking form: entered data kept. Large font + dark theme: no clipped text.  ☐
14. Logout → guest Home; sign in as a *different* account → previous account's bookings are not visible.  ☐
