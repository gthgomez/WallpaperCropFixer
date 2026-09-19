# PROJECT_CONTEXT.md — WallpaperCropFixer

Independent Kotlin/Jetpack Compose Android wallpaper crop utility. This repository is its source of truth; it does not require a particular local checkout path or parent workspace.

## Startup

Read `AGENTS.md`, `CLAUDE.md`, then this file before editing.

## Architecture & Invariants

- `app/` contains the Android application.
- Compose is enabled and the project targets Java/Kotlin JVM 17.
- Hilt is used for dependency injection; it is an intentional app dependency.
- DataStore, ML Kit face detection, and ExifInterface are app dependencies.
- Image crop/math, EXIF orientation, face-detection-assisted positioning, file access, and wallpaper/export flows are correctness-sensitive.

## Verification & Commands

Run from the repository root. On Windows use `gradlew.bat`; on Unix use `./gradlew`.

- Build: `.\gradlew.bat :app:assembleDebug`
- Unit tests: `.\gradlew.bat :app:testDebugUnitTest`
- Install locally: `.\gradlew.bat :app:installDebug`

## Risk Zones

- `app/src/main/AndroidManifest.xml` permissions, activities, providers, and intent filters.
- Image processing, crop rectangles, EXIF orientation, and ML Kit face detection.
- DataStore schema/keys and any persisted user preferences.
- Release signing, minification, and Android export artifacts.

## Build provenance

Settings displays version name, version code and a short source commit from generated BuildConfig. Gradle reads the checked-out Git HEAD (including CI merge checkouts); when Git metadata is absent, a validated `SOURCE_COMMIT` or `GITHUB_SHA` environment value is used, otherwise `unknown`. Dirty Git checkouts append `-dirty`; this identifies local changes without pretending they are a reproducible commit. Rebuild after changing source. Archive builders should supply `SOURCE_COMMIT` from their trusted source record.

## Release state

Current main contains post-RC1 work toward RC2. Historical RC1 evidence is not current candidate certification. The integrated candidate record lives in `docs/release/WALLPAPERCROPFIXER_RELEASE_READINESS_REPORT.md`. Do not create `v1.0.0-rc2` before required physical QA or explicit owner authorization.
