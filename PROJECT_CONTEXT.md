# PROJECT_CONTEXT.md - WallpaperCropFixer

## What This Is

Independent Android app under `C:\Workspace\Project_Android`. It is a Kotlin/Jetpack Compose wallpaper crop/fix utility using Hilt, DataStore, Coil, ML Kit face detection, and EXIF handling.

This file is the agent-neutral project context. Parent `Project_Android/PROJECT_CONTEXT.md` provides shared Android workspace rules, but this app has its own dependencies and risk zones.

## Startup Sequence

1. Read `AGENTS.md` in this directory — project-local agent guidance.
2. Read this file (`PROJECT_CONTEXT.md`) — directory map and invariants.
3. Read `C:\Workspace\Project_Android\PROJECT_CONTEXT.md` — workspace-wide context.
4. Read `C:\Workspace\Project_Android\CLAUDE.md` — behavioral rules and Android patterns.
5. Review `C:\Workspace\Project_Android\tasks\lessons.md` if it exists.

## Architecture & Invariants

- `app/` contains the Android application.
- Compose is enabled and the project targets Java/Kotlin JVM 17.
- Hilt is used for dependency injection; do not apply the parent workspace's "no DI framework" assumption here.
- DataStore, Coil, ML Kit face detection, and ExifInterface are app dependencies.
- Image crop/math, EXIF orientation, face-detection-assisted positioning, file access, and wallpaper/export flows are correctness-sensitive.

## Verification & Commands

Run from `C:\Workspace\Project_Android\WallpaperCropFixer`.

- Build: `.\gradlew.bat :app:assembleDebug`
- Unit tests: `.\gradlew.bat :app:testDebugUnitTest`
- Install locally: `.\gradlew.bat :app:installDebug`

## Risk Zones

- `app/src/main/AndroidManifest.xml` permissions, activities, providers, and intent filters.
- Image processing, crop rectangles, EXIF orientation, and ML Kit face detection.
- DataStore schema/keys and any persisted user preferences.
- Release signing, minification, and Android export artifacts.
