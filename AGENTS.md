# AGENTS.md - WallpaperCropFixer

Agent-neutral startup router for the WallpaperCropFixer Android app. Root `ENGINEERING.md` and root `AGENTS.md` remain authoritative for safety, verification, deletion, scope, and truthfulness.

## Startup Sequence

1. Read `C:\Workspace\ENGINEERING.md`.
2. Read `C:\Workspace\AGENTS.md`.
3. Read `C:\Workspace\Project_Android\PROJECT_CONTEXT.md`.
4. Read `PROJECT_CONTEXT.md` in this directory.
5. Read a model adapter only when it applies to the active tool.

## Local Rules

- `PROJECT_CONTEXT.md` is the canonical app-local context for all agents.
- This app uses Hilt, DataStore, Coil, ML Kit face detection, and EXIF handling.
- Do not assume the parent Project_Android "manual DI only" pattern applies here.
- Treat image crop math, EXIF handling, file/export behavior, and Android manifest changes as high risk.

## Verification

Use the Gradle commands in `PROJECT_CONTEXT.md`. Do not claim Android build/test success unless the command was actually run and passed.
