---
layout: default
title: Privacy Policy — Wallpaper Crop Fixer
---

# Privacy Policy — Wallpaper Crop Fixer

**Last updated:** 2026-08-27

**Developer/entity:** `OWNER_PROVIDE_PLAY_DEVELOPER_ENTITY`

**Privacy contact:** `OWNER_PROVIDE_PUBLIC_PRIVACY_CONTACT`

The owner must replace both marked fields with the developer identity shown on
the Google Play listing and a durable public privacy-inquiry mechanism before
submitting the app. A private repository issue tracker is not a valid contact
mechanism for reviewers or users.

Wallpaper Crop Fixer processes photos **on your device**. We do not operate a backend that receives your images.

## Summary

- **No account required.** The app does not create user accounts or collect personal identifiers.
- **No server photo upload.** The app does not transmit your photos to developer servers.
- **No ads or ad SDKs.** The app contains no advertising SDKs.
- **Diagnostics:** The Google ML Kit Face Detection SDK may collect diagnostic and performance telemetry as documented below. We do not operate analytics of our own.

## Photo selection & storage

- **Photo Picker (`PickVisualMedia`):** You choose one image using the Android system Photo Picker. The app does not request broad photo-library permissions (`READ_MEDIA_IMAGES` or `READ_EXTERNAL_STORAGE`).
- **Temporary local cache:** The app copies the selected image into private app cache storage solely for crop processing, face detection, and preview generation during your session. Cache files are excluded from cloud backup. Files named by the app are pruned when a new photo is selected if they are older than 24 hours; Android may also clear cache storage earlier. The current photo can remain until the session ends or the operating system clears the cache.
- **Save to gallery:** On Android 10+ (API 29+), saved wallpapers are written via **MediaStore** to `Pictures/WallpaperCropFixer`. On earlier Android versions the file is written to the app's external folder (visible via a Files app, not the gallery) — the app never requests a storage permission.

## On-device processing & ML Kit diagnostics

- **Crop and preview math** run entirely on your device.
- **EXIF orientation** is read locally to normalize rotation before cropping.
- **Face-aware crop** uses **Google ML Kit Face Detection** (`com.google.mlkit:face-detection:16.1.7`) with the **bundled** on-device model shipped within the APK. Image pixels and face-detection results remain on your device and are never uploaded for ML inference.
- **SDK Diagnostic Telemetry:** As documented by Google's [ML Kit data-disclosure page](https://developers.google.com/ml-kit/android-data-disclosure), the ML Kit SDK may transmit diagnostic metadata to Google (device information, app package name, per-installation identifiers, API configuration such as image dimensions/format, performance latency metrics, and error codes). This diagnostic data is encrypted in transit and handled pursuant to Google's Privacy Policy. The application itself makes no network calls.

## Wallpaper and export

- **`SET_WALLPAPER`:** When you apply a wallpaper, the rendered bitmap is handed directly to Android's on-device `WallpaperManager`.

## Local preferences

Default crop mode, wallpaper target, face-aware preference, and export quality are stored locally on your device using **DataStore**. These preferences are included in Android Auto Backup so your defaults can be restored after a device transfer; no images are backed up.

## Google Play Data Safety worksheet (proposed; owner must confirm in Play Console)

Based on the verified dependency behavior above, the intended Data Safety declaration is:

| Question | Answer |
| --- | --- |
| Play taxonomy | Collected? | Shared? | Ephemeral? | Required/optional | Purpose | Encrypted in transit? | Evidence / owner check |
|---|---|---|---|---|---|---|---|
| Device or other IDs (per-installation identifiers) | Yes, by bundled ML Kit diagnostics | No, per ML Kit disclosure | Unknown; SDK retention is not specified here | Optional to the app's core photo workflow; emitted by SDK behavior | Analytics / diagnostics | Yes, HTTPS | ML Kit disclosure; verify packaged/runtime behavior |
| App info and performance (diagnostic events, latency, API configuration, input/output sizes, feature version, error codes) | Yes, by bundled ML Kit diagnostics | No, per ML Kit disclosure | Unknown; SDK retention is not specified here | Optional to the app's core photo workflow; emitted by SDK behavior | Analytics / diagnostics | Yes, HTTPS | ML Kit disclosure; verify packaged/runtime behavior |
| Photos or videos / precise face data | Accessed and processed locally; not collected by this app | No | Yes, in-memory/cache processing only | Optional, only after the user selects a photo and enables face-aware processing | App functionality | Not applicable to local processing | Source code and physical traffic capture |

The worksheet is advisory until physical-device traffic capture confirms the
packaged SDK's runtime behavior. Do not answer “no data collected” if the
packaged ML Kit diagnostics are present.

> **Before submitting, verify the final traffic behavior on a physical device** (see the QA checklist, "Privacy" section) and adjust this worksheet to match what the compiled application actually does.

## Contact

For privacy inquiries, use the public contact mechanism supplied by the owner
in `OWNER_PROVIDE_PUBLIC_PRIVACY_CONTACT`. The app does not offer accounts, so
there is no account-deletion workflow. Local cache files are removed by the
operating system's cache policy or by the app's 24-hour pruning pass; saved
exports remain where the user chose to save them until the user or platform
removes them. DataStore preferences may be included in Android Auto Backup;
selected photos and cache files are excluded by the app's backup rules.
