---
layout: default
title: Privacy Policy — Wallpaper Crop Fixer
---

# Privacy Policy — Wallpaper Crop Fixer

**Last updated:** 2026-08-27

Wallpaper Crop Fixer processes photos **on your device**. We do not operate a backend that receives your images.

## Summary

- **No account required.** The app does not create user accounts or collect personal identifiers.
- **No server photo upload.** The app does not transmit your photos to developer servers.
- **No ads or ad SDKs.** The app contains no advertising SDKs.
- **Diagnostics:** The Google ML Kit Face Detection SDK may collect diagnostic and performance telemetry as documented below. We do not operate analytics of our own.

## Photo selection & storage

- **Photo Picker (`PickVisualMedia`):** You choose one image using the Android system Photo Picker. The app does not request broad photo-library permissions (`READ_MEDIA_IMAGES` or `READ_EXTERNAL_STORAGE`).
- **Temporary local cache:** The app copies the selected image into private app cache storage solely for crop processing, face detection, and preview generation during your session. Cache files are excluded from cloud backup and are pruned automatically.
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

## Google Play Data Safety worksheet (proposed)

Based on the verified dependency behavior above, the intended Data Safety declaration is:

| Question | Answer |
| --- | --- |
| Is your app collecting any data? | Yes — diagnostic data from the ML Kit SDK (device info, app info, performance, diagnostics). The app itself collects no data. |
| Data types | Device or other IDs (per-installation identifiers); App activity or performance (diagnostics). |
| Is the data collected shared? | Not shared with third parties. |
| Is the data encrypted in transit? | Yes (HTTPS). |
| Can users request deletion? | N/A — the app does not store user data on any server. |

> **Before submitting, verify the final traffic behavior on a physical device** (see the QA checklist, "Privacy" section) and adjust this worksheet to match what the compiled application actually does.

## Contact

For questions regarding this privacy policy, please file an issue or contact the repository maintainers at `https://github.com/gthgomez/WallpaperCropFixer/issues`.