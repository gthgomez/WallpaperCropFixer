# PROJECT_CONTEXT.md - WallpaperCropFixer App Module

## What This Is

Android application module for WallpaperCropFixer.

## Startup Sequence

1. Read `C:\Workspace\ENGINEERING.md`.
2. Read `C:\Workspace\AGENTS.md`.
3. Read `C:\Workspace\Project_Android\WallpaperCropFixer\PROJECT_CONTEXT.md`.
4. Read this file.

## Architecture & Invariants

- Parent app context remains authoritative.
- Crop math, EXIF/orientation handling, ML Kit labeling, media permissions, billing, and release behavior are high-risk.
- Verify visual/image behavior with targeted samples or manual device notes when changed.

## Verification & Commands

- From parent root: `.\gradlew.bat assembleDebug`
- From parent root: `.\gradlew.bat test`
- From parent root: `.\gradlew.bat connectedAndroidTest` when device/emulator verification is needed.
