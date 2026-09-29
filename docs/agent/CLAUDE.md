# CLAUDE.md — WallpaperCropFixer

Agent-neutral startup guidance. Read [AGENTS.md](AGENTS.md), this file, then [PROJECT_CONTEXT.md](PROJECT_CONTEXT.md). All required instructions live in this repository; local parent-workspace conventions are optional context and cannot override the task.

## Local rules

- `PROJECT_CONTEXT.md` is the canonical app-local technical context.
- This app uses Hilt, DataStore, ML Kit face detection and EXIF handling. Do not substitute a parent workspace's manual-DI convention.
- Preserve snapshot authority: stale previews may remain visible but must not be eligible for Apply or Save.
- Preserve EXIF-correct bounded decoding, deterministic blur and separate preview viewing versus Apply destination.
- Use proportionate verification while developing. Run the complete release matrix only on the integrated candidate unless release-specific work requires it earlier.
- Physical-device, TalkBack, OEM, privacy-traffic and Play checks remain pending until actually executed.

Run commands from the repository root; see `PROJECT_CONTEXT.md` and `QA_CHECKLIST.md`.
