# WallpaperCropFixer Status

**Updated:** 2026-09-19
**State:** Post-RC1 main; integrated RC2 candidate code gates PASS, physical and Play gates pending. No RC2 tag.
**Starting main:** `ce002089d3ff9aabcacffbf9cb5ed7a2121eadcc` (live main checked at campaign start).
Historical RC1 results do not certify current source. See the release readiness report for the candidate record and `QA_CHECKLIST.md` for pending physical/Play gates.

## Purpose

Wallpaper crop and adjustment utility using on-device ML Kit Face Detection to assist positioning crops around faces, handling EXIF orientation mapping, and preventing aggressive system wallpaper crops.

## Source capabilities (candidate verification tracked separately)

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

- **Physical-device QA matrix** (Samsung One UI, Pixel, API 26–28 legacy storage) — see QA_CHECKLIST.md; not executed for this candidate.
- **Public privacy URL:** deployment was recorded successful on 2026-09-05; recheck reachability and updated content before submission.
- **Public developer/entity and privacy contact** must replace explicit owner fields in `PRIVACY.md`.
- **Play Console Data Safety form** based on the PRIVACY.md worksheet (OWNER ACTION; not submitted).
- **Upload-key signing / Play App Signing** configuration (OWNER ACTION; env-var signing is wired in `app/build.gradle.kts`).
- Closed testing (12+ testers / 14 days) and production access application.

## Repository checks

Branch protection was configured during the RC2 campaign to require `Build, lint, and unit tests`, `Dependency declaration policy`, and `Secrets scan`, including administrator enforcement and pull requests. Candidate readiness still requires CI on the exact reviewed head.

## Integrated candidate evidence

- Candidate source SHA: `6fc6972a151a97d59dabcb20f02868ac1e151ca0`.
- Final matrix: lintDebug, lintReleaseVerification, 112 unit tests, assembleDebug, assembleReleaseVerification and bundleReleaseVerification all PASS.
- Pinned release preflight: PASS with expected owner/device action lines; artifact hashes and paths are recorded in the release readiness report.

## Blockers

- Public identity/contact, physical QA, privacy traffic, secure signing and Play actions remain owner/device gates.

## Verification

- `.\gradlew.bat :app:testDebugUnitTest :app:lintDebug`
- `.\gradlew.bat :app:lintDebug :app:lintReleaseVerification :app:testDebugUnitTest :app:assembleDebug :app:assembleReleaseVerification :app:bundleReleaseVerification`
- `pwsh -File tools/release-preflight.ps1 -RepoRoot . -BundletoolPath <pinned bundletool jar> -GitleaksPath <pinned gitleaks binary>`

## Evidence Sources

- [README.md](README.md)
- [PRIVACY.md](PRIVACY.md)
- [QA_CHECKLIST.md](QA_CHECKLIST.md)
