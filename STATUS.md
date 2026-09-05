# WallpaperCropFixer Status

**Last verified:** 2026-09-05
**Status:** 1.0 release candidate (RC1) — bounded polish complete; awaiting physical-device QA + Play Console owner actions
**Confidence:** high for engineering and release-trust verification. Hosted CI is green on PR #2 (executed 2026-09-05 after the earlier billing block cleared); post-merge main re-verified locally the same day. See `docs/release/WALLPAPERCROPFIXER_RELEASE_READINESS_REPORT.md` for the full gate record and `docs/release/OWNER_ACTIONS.md` for the owner handoff.

## Purpose

Wallpaper crop and adjustment utility using on-device ML Kit Face Detection to assist positioning crops around faces, handling EXIF orientation mapping, and preventing aggressive system wallpaper crops.

## Verified Capabilities

- Explicit `releaseVerification` AAB/APK builds run R8/resource shrinking and are not upload-ready; `bundleRelease` fails closed without runtime signing inputs.
- Full EXIF orientation matrix mapping (orientations 1–8) and upright canonical bounds calculation.
- Bundled ML Kit Face Detection with bounded decode budget (max 14 MP decode, 4096 px side) preventing OOM/memory amplification on 50–100 MP images; sequential HOME/LOCK rendering.
- Concurrency-safe editor: generation-token preview/load pipeline; stale results are discarded; cancellation never surfaces as a user error.
- Bitmap ownership: published preview bitmaps are never manually recycled (Compose/apply/export can always reference them safely).
- Correct focus/tap coordinate mapping for wide HOME canvases (viewport transform).
- Maximum-window display metrics for split-screen/foldable correctness.
- Opaque wallpaper output (no alpha edges).
- Transactional MediaStore export on API 29+ and accurate app-folder reporting on API 26–28.
- Explicit WallpaperManager return-code evaluation and typed, accurate partial-failure messaging for BOTH targets.
- Manifest contains only `SET_WALLPAPER` (+ dependency-merged INTERNET for ML Kit telemetry, disclosed in PRIVACY.md).
- Explicit backup rules (preferences only; photos never backed up).
- Public privacy-policy publishing workflow (GitHub Pages) + proposed Data Safety worksheet.
- 16 KB verification is split into APK ZIP, AAB `PAGE_ALIGNMENT_16K`, and native ELF checks in `tools/release-preflight.ps1`.
- CI uses explicit action SHAs, checksum-verified gitleaks, and a single-source dependency declaration policy (`tools/release-preflight.ps1 -DependencyOnly`); GitHub's vulnerability review remains unavailable until the owner enables Dependency graph + Advanced Security.
- Hosted CI jobs resumed executing on 2026-08-31 once the account billing/spending-limit state cleared; the earlier pre-start failures (run `33129249156`) are historical.
- Unit/Robolectric/Compose tests include deterministic revision-race coverage for preview, Apply, Save, and BOTH pairing.

## In Progress / Remaining (non-code)

- **Physical-device QA matrix** (Samsung One UI, Pixel, API 26–28 legacy storage) — see QA_CHECKLIST.md.
- **GitHub Pages enablement** for the privacy-policy URL (OWNER ACTION; current deploy failed with GitHub Pages 404).
- **Public developer/entity and privacy contact** must replace explicit owner fields in `PRIVACY.md`.
- **Play Console Data Safety form** based on the PRIVACY.md worksheet (OWNER ACTION).
- **Upload-key signing / Play App Signing** configuration (OWNER ACTION; env-var signing is wired in `app/build.gradle.kts`).
- Closed testing (12+ testers / 14 days) and production access application.

## Blockers

- No known unaddressed code blocker after local verification; public privacy hosting/contact, vulnerability database gate, physical QA, and secure signing remain external gates.

## Verification

- `.\gradlew.bat :app:testDebugUnitTest :app:lintDebug`
- `.\gradlew.bat :app:lintDebug :app:lintReleaseVerification :app:testDebugUnitTest :app:assembleDebug :app:assembleReleaseVerification :app:bundleReleaseVerification`
- `pwsh -File tools/release-preflight.ps1 -RepoRoot . -BundletoolPath <pinned bundletool jar> -GitleaksPath <pinned gitleaks binary>`

## Evidence Sources

- [README.md](file:///C:/Workspace/Project_Android/WallpaperCropFixer/README.md)
- [PRIVACY.md](file:///C:/Workspace/Project_Android/WallpaperCropFixer/PRIVACY.md)
- [QA_CHECKLIST.md](file:///C:/Workspace/Project_Android/WallpaperCropFixer/QA_CHECKLIST.md)
