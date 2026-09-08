# PLAY_SUBMISSION_CHECKLIST.md — WallpaperCropFixer (com.wallpapercropfixer, v1.0 / versionCode 1)

Ordered checklist from "app entry created" to "submitted for review".
Labels: **VERIFIED_CURRENT_GOOGLE_REQUIREMENT** (checked 2026-09-05, source cited) · **REPOSITORY_DERIVED_ANSWER** (from code audit) · **OWNER_MUST_CONFIRM**.

Legend: ☐ = owner action · ▨ = agent-prepared artifact to paste/upload.

## Phase 0 — Prerequisites
- ☐ Play Console account registered (US$25 one-time fee — **VERIFIED_CURRENT_GOOGLE_REQUIREMENT**: https://support.google.com/googleplay/android-developer/answer/6112435) and identity verification finished. OWNER_MUST_CONFIRM status.
- ☐ Upload keystore generated offline per SIGNING_RUNBOOK.md; four `RELEASE_*` env vars set locally.
- ☐ Privacy policy live at `https://gthgomez.github.io/WallpaperCropFixer/PRIVACY.html` (GitHub Pages enabled). Required for all apps — **VERIFIED_CURRENT_GOOGLE_REQUIREMENT**: https://support.google.com/googleplay/android-developer/answer/9859455
- ☐ Device QA passed per DEVICE_QA_RUNBOOK.md (Galaxy S25 Ultra primary).

## Phase 1 — App entry
- ☐ Create app: name **"Wallpaper Crop Fixer"** (20/30 chars), default language English (en-US), App or Game = **App**, Free or Paid = **Free** (no ads, no IAP — REPOSITORY_DERIVED_ANSWER).
- ☐ Declaration checkboxes: developer program policies + US export laws → accept.

## Phase 2 — Store listing (Grow → Store presence → Main store listing)
Fields:
- ▨ App name: `Wallpaper Crop Fixer` — 20/30 chars (limit 30 — **VERIFIED_CURRENT_GOOGLE_REQUIREMENT**: https://support.google.com/googleplay/android-developer/answer/13393723).
- ▨ Short description: `Preview how your photo fits home and lock screens before you set it. No ads.` — 76/80 chars (limit 80 — same source).
- ▨ Full description: STORE_LISTING_DRAFT.md §2 (≤4000 chars, same source).
- ▨ App icon: 512×512 32-bit PNG ≤1 MB (export from repo adaptive-icon foreground/background — REPOSITORY_DERIVED_ANSWER).
- ▨ Feature graphic: 1024×500, JPEG or 24-bit PNG without alpha (**VERIFIED_CURRENT_GOOGLE_REQUIREMENT**: https://support.google.com/googleplay/android-developer/answer/9866151) — concept in STORE_LISTING_DRAFT.md §4.
- ▨ Phone screenshots: **minimum 2** (we ship 5), PNG/JPEG, 16:9 or 9:16, each side 320–3840 px, max 8 per device type (**VERIFIED_CURRENT_GOOGLE_REQUIREMENT**: same source) — plan in STORE_LISTING_DRAFT.md §3.
- ☐ Category: **Personalization**; tags: wallpaper, photos/customization (Play offers tag picker — OWNER_MUST_CONFIRM exact tags available).
- ☐ Store contact: email OWNER_MUST_CONFIRM; website `https://github.com/gthgomez/WallpaperCropFixer` (or Pages site); no phone required.

## Phase 3 — App content declarations (Policy and programs → App content)
- ▨ **Privacy policy**: `https://gthgomez.github.io/WallpaperCropFixer/PRIVACY.html` — required for all apps (**VERIFIED_CURRENT_GOOGLE_REQUIREMENT**: https://support.google.com/googleplay/android-developer/answer/9859455).
- ▨ **Ads**: "No, my app does not contain ads" (REPOSITORY_DERIVED_ANSWER — no ad SDKs in code).
- ▨ **In-app purchases**: none — free utility, no Billing integration (REPOSITORY_DERIVED_ANSWER).
- ▨ **Data safety**: paste answers from DATA_SAFETY_DRAFT.md — "No data collected / No data shared". Structure per (**VERIFIED_CURRENT_GOOGLE_REQUIREMENT**: https://support.google.com/googleplay/android-developer/answer/10787469).
- ▨ **Content rating**: IARC questionnaire for all apps (**VERIFIED_CURRENT_GOOGLE_REQUIREMENT**: https://support.google.com/googleplay/android-developer/answer/9859655). For this utility: no violence/gambling/UGC/user interaction → expected rating **Everyone (3+)**. Email for IARC confirmation: OWNER_MUST_CONFIRM.
- ▨ **Target audience**: age group **13+** ("Not designed for children"); no appeal-to-children marketing (photos/wallpapers = adult framing) (**VERIFIED_CURRENT_GOOGLE_REQUIREMENT**: https://support.google.com/googleplay/android-developer/answer/9867159).
- ▨ **News**: not a news app → No.
- ▨ **COVID-19**: not related → No.
- ▨ **Data safety / government apps**: not a government app → No.
- ▨ **Financial features**: none → declare "none of these".
- ▨ **Health apps**: none → No.
- ▨ **App access**: "All functionality is available without special access" / no credentials needed (REPOSITORY_DERIVED_ANSWER — no accounts, no restrictions).
- ☐ Re-check every declaration after Play Console UI updates; field names drift.

## Phase 4 — Release track (Testing → Closed testing)
- ☐ Create closed testing track; name e.g. `Release Candidates`.
- ☐ Add **≥12 tester emails**; requirement: 12 opted-in testers for 14 consecutive days before production access for personal accounts created after Nov 13, 2023 (**VERIFIED_CURRENT_GOOGLE_REQUIREMENT**: https://support.google.com/googleplay/android-developer/answer/14151465).
- ☐ Country availability: start with your own country; expand later.
- ☐ Release notes (what's new): `Initial release. Preview, position, and apply wallpapers with true proportions.` ▨

### 4a. First AAB upload — read before uploading
- ⚠ **Do NOT upload the `bundleReleaseVerification` AAB.** It is debug-signed; it exists only as a CI/verification boundary and Play will reject it as unsigned-with-wrong-key.
- ▨ Agent prepares the build inputs (tag, versionName/versionCode, artifact name). **The upload AAB must come from an owner-signed local build**: run `:app:bundleRelease` locally with the four `RELEASE_*` env vars set (SIGNING_RUNBOOK.md §6), verify with `jarsigner -verify` (§7), then:
- ☐ Upload the signed `.aab` to the closed testing release, add release notes, **Review release → Start rollout to Testing**.
- ☐ Play App Signing enrollment happens automatically here: Google keeps the app signing key; your local key is registered as the **upload key** (**BEST_KNOWLEDGE** — standard flow per Google Play Signing docs; confirm the enrollment screen text at upload time).

## Phase 5 — Production (after 14 qualifying days)
- ☐ Apply for production access (Testing → Production → apply; cite testing track). VERIFIED_CURRENT_GOOGLE_REQUIREMENT source above.
- ☐ Copy store-listing defaults + approved AAB into Production; choose **staged rollout 20%**, then 100%.
- ☐ Submit for review; monitor Play Console inbox (OWNER_ACTIONS.md §4).

## Sign-off
| Gate | Owner | Date |
|------|-------|------|
| QA passed on S25 Ultra + Pixel-class | ☐ | |
| Declarations submitted | ☐ | |
| Signed AAB uploaded to closed track | ☐ | |
| 14-day tester window complete | ☐ | |
| Production submitted | ☐ | |
