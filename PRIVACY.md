# Privacy Policy — Wallpaper Crop Fixer

**Last updated:** 2026-08-13  
**Placeholder URL:** `https://example.invalid/wallpaper-crop-fixer-privacy` (replace before store submission)

Wallpaper Crop Fixer processes photos **on your device**. We do not operate a backend that receives your images.

## Summary

- **No account required.** The app does not create user accounts or collect personal identifiers.
- **No network permission.** The app does not declare `INTERNET` and does not transmit your photos to our servers.
- **No third-party analytics or ads** in the current codebase.

## Photo selection

- **Android 13+ (API 33+):** Uses the system **Photo Picker** (`PickVisualMedia`). You choose one image; the app does not request `READ_MEDIA_IMAGES` or broad gallery access.
- **Android 12 and below (API 32 and below):** Uses the same Photo Picker flow, but may request **`READ_EXTERNAL_STORAGE`** when the system requires it for legacy gallery access.
- **Temporary access:** The picker grants short-lived read access to the chosen image. The app copies that image into its **app cache** so crop, face detection, and export can run without retaining gallery permissions.

## On-device processing

- **Crop and preview math** run entirely on your device.
- **EXIF orientation** is read locally to correct rotation before cropping.
- **Face-aware crop** uses **ML Kit Face Detection** (`com.google.mlkit:face-detection:16.1.7`) with the **bundled** on-device model shipped in the APK (`libface_detector_v2_jni.so`). Face detection does **not** upload photos and does **not** require a model download at runtime. If detection fails or finds no faces, the app falls back to a center crop.

## Wallpaper and export

- **`SET_WALLPAPER`:** When you apply a wallpaper, the cropped bitmap is passed to Android's `WallpaperManager` on your device only.
- **Save to gallery:** On Android 10+ (API 29+), exports are written via **MediaStore** to `Pictures/WallpaperCropFixer` without storage permissions. On older Android versions, exports go to the public Pictures folder.

## Local preferences

Default crop mode, wallpaper target, face-aware toggle, and JPEG quality are stored locally with **DataStore** on your device.

## Data we do not collect

- We do not sell personal data.
- We do not receive your photos on developer-operated servers.

## Contact

Replace this section with a support email or web form before public release.
