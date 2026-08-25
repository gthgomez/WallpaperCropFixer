# WallpaperCropFixer Status

**Last verified:** 2026-08-25
**Status:** remediated / pre-release verification
**Confidence:** high

## Purpose

Wallpaper crop and adjustment utility using on-device ML Kit Face Detection to assist positioning crops around faces, handling EXIF orientation mapping, and preventing aggressive system wallpaper crops.

## Verified Capabilities

- Full EXIF orientation matrix mapping (orientations 1–8) and upright canonical bounds calculation.
- Bundled ML Kit Face Detection with downsampled image bounds (max 1080x1080) to prevent OOM/memory amplification on 50–100 MP images.
- Multi-face group protection with clipped-face detection and full-source preservation in `SAFE_FIT` padding mode.
- Bidirectional WYSIWYG focus coordinate mapping between preview canvas space and source image space.
- Transactional MediaStore export on API 29+ (clean rollback on failure) and fallback storage on API 26–28.
- Explicit WallpaperManager return code evaluation and accurate partial failure reporting for `BOTH` targets.
- Disclosed ML Kit diagnostic telemetry and active repository privacy policy URL.

## In Progress

- Full device QA matrix (Samsung One UI, Pixel, API 26-28 legacy storage, 16 KB page-size verification).

## Blockers

- None. All P1 code findings (WCF-001 through WCF-010) remediated.

## Verification

- `.\gradlew.bat :app:testDebugUnitTest :app:lintDebug`
- `.\gradlew.bat :app:assembleRelease :app:bundleRelease`

## Evidence Sources

- [README.md](file:///C:/Workspace/Project_Android/WallpaperCropFixer/README.md)
- [QA_CHECKLIST.md](file:///C:/Workspace/Project_Android/WallpaperCropFixer/QA_CHECKLIST.md)
