# Privacy Policy — Wallpaper Crop Fixer

**Last updated:** 2026-08-25  
**Policy URL:** `https://github.com/gthgomez/WallpaperCropFixer/blob/main/PRIVACY.md`

Wallpaper Crop Fixer processes photos **on your device**. We do not operate a backend that receives your images.

## Summary

- **No account required.** The app does not create user accounts or collect personal identifiers.
- **No server photo upload.** The app does not transmit your photos to developer servers.
- **No ads or ad SDKs.** The app contains no advertising SDKs.
- **Diagnostics:** Google ML Kit Face Detection may collect diagnostic and performance telemetry as documented below.

## Photo selection & storage

- **Photo Picker (`PickVisualMedia`):** You choose one image using the Android system Photo Picker. The app does not request broad photo library permissions (`READ_MEDIA_IMAGES`).
- **Temporary local cache:** The app caches the selected image locally in private app storage solely for crop processing, face detection, and preview generation during your session.
- **Save to gallery:** On Android 10+ (API 29+), saved wallpapers are written via **MediaStore** to `Pictures/WallpaperCropFixer`. On earlier Android versions, exports are written to app media storage.

## On-device processing & ML Kit diagnostics

- **Crop and preview math** run entirely on your device.
- **EXIF orientation** is read locally to normalize rotation before cropping.
- **Face-aware crop** uses **Google ML Kit Face Detection** (`com.google.mlkit:face-detection:16.1.7`) with the **bundled** on-device model shipped within the APK. Image pixels and face detection results remain on your device and are never uploaded for ML inference.
- **SDK Diagnostic Telemetry:** As documented by Google ML Kit, the ML Kit SDK may transmit diagnostic metadata to Google (such as device information, app package name, bundled feature installation IDs, API configuration like image dimensions/format, performance latency metrics, and error codes). This diagnostic data is encrypted in transit and handled pursuant to Google's Privacy Policy.

## Wallpaper and export

- **`SET_WALLPAPER`:** When you apply a wallpaper, the rendered bitmap is handed directly to Android's on-device `WallpaperManager`.

## Local preferences

Default crop mode, wallpaper target, face-aware preference, and export quality are stored locally on your device using **DataStore**.

## Contact

For questions regarding this privacy policy, please file an issue or contact the repository maintainers at `https://github.com/gthgomez/WallpaperCropFixer/issues`.
