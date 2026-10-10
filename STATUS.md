# WallpaperCropFixer Status

**Updated:** 2026-10-10
**State:** Post-RC1 main; integrated RC2 candidate code gates PASS, physical and Play gates pending. No RC2 tag. Post-RC2 feature work below is unreleased and not yet part of a certified candidate.
**Starting main:** `ce002089d3ff9aabcacffbf9cb5ed7a2121eadcc` (live main checked at campaign start).
Historical RC1 results do not certify current source. See the release readiness report for the candidate record and `QA_CHECKLIST.md` for pending physical/Play gates.

## Post-RC2 development (unreleased — 2026-10-10)

Competitive-gap work (see `~/.commandcode/plans/wallpapercropfixer-competitive-improvements.md`). Local gates executed and PASS: `lintDebug`, `lintReleaseVerification`, `testDebugUnitTest`, `assembleDebug`, `assembleReleaseVerification`. Not a candidate certification; device/Play gates remain owner actions.

- **Lock-clock / parallax honesty guides:** the preview now characterizes the lock clock + inset region and the home-screen parallax-visible window, warns when an analyzed subject would sit under the clock or scroll off the edge, and draws a faint clock-area guide.
- **Subject-aware framing:** added ML Kit **Subject Segmentation** (unbundled, `com.google.android.gms:play-services-mlkit-subject-segmentation:16.0.0-beta1`, declared via ML Kit `DEPENDENCIES`). Framing priority is now manual > subject > face > center; a missing/failed model degrades to faces then center. Privacy/Data-Safety docs updated to disclose the Play-services model download.
- **Export receipt:** the editor shows the exact rendered `W×H`, making the "source never silently resized" guarantee visible.
- **Palette-derived finishes:** Color/Gradient backgrounds now use a deterministic dominant-color palette extractor (`PaletteExtractor`) instead of 4-corner sampling.
- **Bounded on-device history:** successful exports are recorded locally (DataStore) and the entry screen offers a "Recent" list with one-tap "Set again". No new permissions, no network.
- **Store listing repositioned** toward honest no-crop/square-fit/blur terms and phone-shaped framing.

Deferred (documented, not implemented): subject-cutout finish, "enhance before applying" toggle, auto-rotate/widget/scheduling, batch fast-path.

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

- Candidate source SHA: `6fc6972a151a97d59dabcb20f02868ac1e151ca0` (embedded in the certified artifacts). Later certification commits change release records only; they do not change artifact source provenance.
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
