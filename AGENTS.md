# AGENTS.md — WallpaperCropFixer (Gemini 3 Flash Override)

> Inherits from root [AGENTS.md](file:///C:/Workspace/Project_Android/AGENTS.md). General guidance is in [CLAUDE.md](./CLAUDE.md).

## Gemini-Specific Risks
- Hallucinated ML Kit Face Detection API — bounding box coordinates and face landmarks
- Incorrect EXIF orientation mapping applied to crop math — orientation must be read before cropping
- Confusion about Hilt DI — Hilt IS authorized here (exception to workspace no-DI convention)

**Verification gate:** `./gradlew assembleDebug`
