# WallpaperCropFixer Status

**Last verified:** 2026-08-13
**Status:** usable
**Confidence:** high

## Purpose

Wallpaper crop and adjustment utility using ML Kit Face Detection to assist positioning crops around faces and handling EXIF orientation mapping.

## Current State

The app is an exception to the manual DI stack rule: Hilt is explicitly authorized for dependency injection here. Uses DataStore for user preferences, Coil for image loading, and ML Kit for face detection.

## Verified Capabilities

- EXIF orientation mapping and pre-crop bounds calculation.
- ML Kit Face Detection (bundled `com.google.mlkit:face-detection` model) to position crop rectangles around single and multi-face photos — no runtime model download, no `INTERNET` permission.
- Graceful fallback to center crop if face detection returns no faces or errors.
- Android Photo Picker on API 33+ without `READ_MEDIA_IMAGES`; `READ_EXTERNAL_STORAGE` only on API 32 and below.
- Jetpack Compose Material3 UI with Hilt dependency injection.

## Recent Evidence

- `PROJECT_CONTEXT.md` explicitly authorizes Hilt dependency injection for this project.
- `QA_CHECKLIST.md` details 6-point EXIF orientation gate and ML Kit fallback requirements.

## In Progress

- Fine-tuning ML Kit multi-face bounding box expansion math for group photo wallpapers.

## Blockers

- None currently blocking development.

## Risks and Unknowns

- ML Kit cold-start latency on low-end devices (model is bundled; no download required).
- Photo Picker copy-to-cache on very large images (>20 MB) may add brief main-thread work.

## Verification

- `.\gradlew.bat :app:assembleDebug` build command setup present.

## Next Actions

1. Run physical device manual QA pass on EXIF 90/270 degree rotated camera photos.
2. Test ML Kit multi-face crop boundary calculation.
3. Validate Android Photo Picker on API 34+ (no permission dialog) and legacy `READ_EXTERNAL_STORAGE` path on API 32.

## Evidence Sources

- [README.md](file:///C:/Workspace/Project_Android/WallpaperCropFixer/README.md)
- [QA_CHECKLIST.md](file:///C:/Workspace/Project_Android/WallpaperCropFixer/QA_CHECKLIST.md)
