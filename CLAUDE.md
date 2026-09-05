# CLAUDE.md - WallpaperCropFixer

Agent-neutral startup router for the WallpaperCropFixer Android app. Root `ENGINEERING.md` and root `CLAUDE.md` for general protocols. Refer to root `AGENTS.md` for Gemini-specific overrides.

## Startup Sequence

1. Read this file (`CLAUDE.md`) — project-local agent guidance.
2. Read `PROJECT_CONTEXT.md` in this directory — directory map and invariants.
3. Read `C:\Workspace\Project_Android\PROJECT_CONTEXT.md` — workspace-wide context.
4. Read `C:\Workspace\Project_Android\CLAUDE.md` — behavioral rules and Android patterns.
5. Review `C:\Workspace\Project_Android\tasks\lessons.md` if it exists.

## Local Rules

- `PROJECT_CONTEXT.md` is the canonical app-local context for all agents.
- This app uses Hilt, DataStore, ML Kit face detection, and EXIF handling.
- Do not assume the parent Project_Android "manual DI only" pattern applies here.
- Treat image crop math, EXIF handling, file/export behavior, and Android manifest changes as high risk.

## Verification

Use the Gradle commands in `PROJECT_CONTEXT.md`. Do not claim Android build/test success unless the command was actually run and passed.
