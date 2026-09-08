# DATA_SAFETY_DRAFT.md — Play Console "Data safety" answers

App: WallpaperCropFixer (`com.wallpapercropfixer`) v1.0
Worksheet source: **PRIVACY.md in the repository** (authoritative privacy facts; this document transcribes it into Play Console form answers).
All answers below are **REPOSITORY_DERIVED_ANSWER** — derived from the 2026-09-05 direct code audit of the merged manifest and app code. Form structure verified 2026-09-05: https://support.google.com/googleplay/android-developer/answer/10787469 ("You do not need to declare collection or sharing unless data is actually collected and/or shared").

---

## Q1. Does your app collect or share any of the required user data types?
**Answer: No.**

- Collection = **None**. Sharing = **None**. No data types collected (no personal info, no photos/videos, no identifiers, no location, no diagnostics).
- Google Play will then display **"No data collected"** and **"No data shared with third parties"** on the listing.

## Q2. (If a data type were ever declared) Security practices — for reference
These become moot when Q1 = No, but record them in case a future version changes the answer:

| Practice | Answer for this app |
|---|---|
| Data encrypted in transit | Not applicable — no user data transmitted |
| Users can request data deletion | Not applicable — no user data collected; Data Store holds only preferences (crop mode, target surface, fill mode, face-aware toggle, JPEG quality), all resident on device |

## Q3. Why "No" is accurate for THIS app (audit basis)
- Photos picked via the system **Photo Picker** are rendered and applied **entirely on-device**; cached copies live only in the app `cacheDir`, excluded from backups, pruned after 24 h.
- Face-aware framing uses **Google ML Kit Face Detection bundled on-device**; faces are never uploaded or transmitted.
- No ads SDK, no analytics, no crash-reporting SDK, no telemetry, no accounts, no app-owned network calls.
- Only runtime permission requested: `SET_WALLPAPER` (normal permission).
- Preferences are stored locally via Data Store and are not "collected" under Play's definition (never leaves the device).

## Q4. Note on the merged INTERNET permission — keep this explanation ready
The **merged manifest includes `INTERNET` + `ACCESS_NETWORK_STATE`** because the ML Kit SDK declares them; they are used only by ML Kit's optional model-download/telemetry path. **App-owned code makes no network calls**, and no user data is sent anywhere.

**Exact explanation to use if Google flags the INTERNET permission during review (paste as reply / policy declaration note):**

> "The INTERNET and ACCESS_NETWORK_STATE permissions appear in the merged manifest solely because they are declared by the bundled Google ML Kit Face Detection SDK (a transitive manifest merger), and are used only by that SDK's optional model-download path. Our app-owned code performs no network requests of any kind: there are no ads, analytics, telemetry, or accounts, and user photos are processed entirely on-device and never uploaded. The app's only declared runtime permission is SET_WALLPAPER. This is consistent with our Data safety declaration of 'No data collected, No data shared'."

## Q5. Privacy policy URL field (same App content page)
- URL: `https://gthgomez.github.io/WallpaperCropFixer/PRIVACY.html`
- Privacy policy is required for **all** apps (VERIFIED_CURRENT_GOOGLE_REQUIREMENT: https://support.google.com/googleplay/android-developer/answer/9859455). OWNER_MUST_CONFIRM: the page must be live (GitHub Pages enabled) before submission.

---

### Sign-off
- ☐ Owner reviewed each answer against PRIVACY.md — **OWNER_MUST_CONFIRM** before submission.
- ☐ Re-run this audit if any future dependency adds an SDK that transmits data (a Firebase/ads addition would invalidate Q1).
