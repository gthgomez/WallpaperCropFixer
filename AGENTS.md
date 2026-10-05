# AGENTS.md — WallpaperCropFixer

This file is the sole instruction authority for engineering agents working in this
repository. Model- or vendor-specific instruction files (CLAUDE.md, GEMINI.md,
CODEX.md, and similar) are prohibited here; do not create or consult them. Nested
instruction files are also prohibited. Factual and architectural context lives in
`docs/PROJECT_CONTEXT.md` and is task data, not policy. Use the current user task
for scope and authority; no external workspace file is a prerequisite.

## What This Is

Independent Kotlin/Jetpack Compose Android wallpaper cropping utility with ML Kit
face detection and EXIF-correct orientation. No photo-library permission — only
`SET_WALLPAPER`. See `docs/PROJECT_CONTEXT.md` for stack, commands, and background.

## Technical Risk Rules

- Verify ML Kit Face Detection APIs and coordinate contracts; do not invent
  bounding-box or landmark behavior.
- Read EXIF orientation before crop math and preserve the upright source coordinate
  space.
- Preserve EXIF-correct bounded decoding, deterministic blur, and the separation
  between preview viewing and the Apply/Save destination.
- Preserve immutable published-preview authority: stale previews may remain visible
  but must never be eligible for Apply or Save. Preserve revision/generation
  ownership, bounded decoding, and on-device photo processing.
- Treat crop math, file/export operations, `AndroidManifest.xml` changes, and
  signing as correctness-sensitive; run relevant tests and review the final diff.
- Never claim a build, test, device check, or release gate passed without executed
  evidence. Physical-device, TalkBack, OEM, privacy-traffic, and Play checks remain
  pending until actually executed.

## Framework Rules

- Hilt is the authorized dependency-injection framework in this app; do not
  substitute another DI convention.
- DataStore, ML Kit face detection, and ExifInterface are intentional app
  dependencies.

## Release and Signing

- Release signing must fail closed. `releaseVerification` is a debug-signed
  verification boundary, never a Play upload artifact.
- Run the complete release matrix only on the integrated candidate unless
  release-specific work requires it earlier.

## Verification

Run commands from the repository root; on Windows use `gradlew.bat`, on Unix use
`./gradlew`. See `docs/PROJECT_CONTEXT.md` and `QA_CHECKLIST.md` for the full
command reference.
