# WallpaperCropFixer Status

**Last verified:** 2026-08-27
**Status:** release-candidate engineering complete / awaiting device QA + Play Console owner actions
**Confidence:** high (build, lint, unit/Robolectric/Compose tests, dependency verification all green)

## Purpose

Wallpaper crop and adjustment utility using on-device ML Kit Face Detection to assist positioning crops around faces, handling EXIF orientation mapping, and preventing aggressive system wallpaper crops.

## Verified Capabilities

- Release AAB builds (R8/minify + resource shrinking enabled) and passes lint (0 errors).
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
- 16 KB page alignment verified on the release APK (`zipalign -P 16`: Verification successful, all ABIs).
- Dependency supply chain: exact pinned versions (deterministic resolution) + CI dependency-review job
  (activates once the owner enables Dependency Graph + GitHub Advanced Security) + gitleaks secrets scan.
- 53 unit/Robolectric/Compose tests green, including concurrency, viewport-mapping, renderer-opacity, and fuzz invariant suites.

## In Progress / Remaining (non-code)

- **Physical-device QA matrix** (Samsung One UI, Pixel, API 26–28 legacy storage) — see QA_CHECKLIST.md.
- **GitHub Pages enablement** for the privacy-policy URL (OWNER ACTION).
- **Play Console Data Safety form** based on the PRIVACY.md worksheet (OWNER ACTION).
- **Upload-key signing / Play App Signing** configuration (OWNER ACTION; env-var signing is wired in `app/build.gradle.kts`).
- Closed testing (12+ testers / 14 days) and production access application.

## Blockers

- None in code. All WCF-001…WCF-027 findings addressed; remaining items are owner/device/Play Console actions documented in the release report.

## Verification

- `.\gradlew.bat :app:testDebugUnitTest :app:lintDebug`
- `.\gradlew.bat :app:lintRelease :app:assembleRelease :app:bundleRelease`
- `zipalign -c -P 16 -v 4 app-release.apk` → "Verification successful"

## Evidence Sources

- [README.md](file:///C:/Workspace/Project_Android/WallpaperCropFixer/README.md)
- [PRIVACY.md](file:///C:/Workspace/Project_Android/WallpaperCropFixer/PRIVACY.md)
- [QA_CHECKLIST.md](file:///C:/Workspace/Project_Android/WallpaperCropFixer/QA_CHECKLIST.md)