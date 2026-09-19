# AGENTS.md — WallpaperCropFixer

Agent-neutral guidance for all engineering assistants. Read this file, [CLAUDE.md](CLAUDE.md), then [PROJECT_CONTEXT.md](PROJECT_CONTEXT.md) before editing.

## Technical risk rules

- Verify ML Kit Face Detection APIs and coordinate contracts; do not invent bounding-box or landmark behavior.
- Read EXIF orientation before crop math and preserve the upright source coordinate space.
- Hilt is the authorized dependency-injection framework in this app.
- Preserve immutable published-preview authority, revision/generation ownership, bounded decoding, and on-device photo processing.
- Release signing must fail closed. `releaseVerification` is a debug-signed verification boundary, never a Play upload artifact.
- Treat crop math, file/export operations, manifest changes and signing as correctness-sensitive; run relevant tests and review the final diff.
- Never claim a build, test, device check or release gate passed without executed evidence.

Use the current user task for scope and authority. No model-specific framing or external workspace file is a prerequisite.
