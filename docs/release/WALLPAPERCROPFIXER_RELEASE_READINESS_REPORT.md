# WallpaperCropFixer — Release Readiness Report (1.0 RC1)

**Campaign date:** 2026-09-05
**Baseline main SHA:** 97f4ba4 · **Final main SHA:** see §Record · **RC SHA:** see §Record

## 1. Gate results

```text
GATE_1_CANONICAL_ENGINEERING=PASS   (PR #2 merged: hardening canonicalized, CI green)
GATE_2_RELEASE_TRUST=PASS           (privacy URL live; fail-closed signing; SHA-pinned CI)
GATE_3_RC1=PASS                     (bounded polish; frozen RC; recorded artifact hashes)
GATE_4_DEVICE_AND_PLAY=PARTIAL      (runbook + assets ready; physical QA + Console = owner)
```

## 2. Verification record

All commands executed locally on the RC commit (Windows, JDK 17, AGP 9.x):

| Check | Command | Result |
|---|---|---|
| Unit tests (fresh) | `./gradlew :app:testDebugUnitTest --rerun-tasks` | 53/53 PASS |
| Lint | `./gradlew :app:lintDebug :app:lintReleaseVerification` | PASS (advisory findings only) |
| Debug APK | `./gradlew :app:assembleDebug` | PASS |
| Verification APK/AAB | `./gradlew :app:assembleReleaseVerification :app:bundleReleaseVerification` | PASS |
| Upload build fail-closed | `./gradlew :app:bundleRelease` without `RELEASE_*` env | FAILS (exit 1, explicit GradleException) — intended |
| Release preflight | `pwsh tools/release-preflight.ps1 -RepoRoot . -SkipBuild -BundletoolPath <jar>` | PASS (16 KB ZIP/AAB/native-ELF alignment, permissions, signing contract) |
| Dependency policy negative test | planted `"2.7.0+"` / `"1.14.0-SNAPSHOT"` | FAIL exit 1 (rule works) |
| Hosted CI | PR #2 + PR #5 checks | PASS (verify, dependency-policy, secrets-scan) |
| Privacy URL | `curl https://gthgomez.github.io/WallpaperCropFixer/PRIVACY.html` | HTTP 200 |

Play Console validation has NOT occurred (owner-only); no claim is made about Play acceptance.

## 3. RC record — WallpaperCropFixer 1.0 RC1

```text
BASELINE_MAIN_SHA=97f4ba4 (campaign start)
RC_TAG=v1.0.0-rc1 (annotated; points at the freeze commit containing this record)
RC_CODE_STATE=f972983 (PR #5 merge — final code commit; the freeze commit changes docs only)
VERSION_NAME=1.0
VERSION_CODE=1
APPLICATION_ID=com.wallpapercropfixer
AAB_PATH=app/build/outputs/bundle/releaseVerification/app-releaseVerification.aab (15,304,878 bytes)
AAB_SHA256=f5e8b6a46a4b85c07bd577874fe88bcd70c217c40838c28979463ccbbe2ff80e
APK_PATH=app/build/outputs/apk/releaseVerification/app-releaseVerification.apk (24,599,012 bytes)
APK_SHA256=0a1347b243fc3a09bc739ee597f3f98f25fedc40d705f9e34ec8ca150438dbfe
BUILD_TIMESTAMP=2026-09-05T20:02Z (artifacts are debug-signed verification builds — NOT upload-ready)
UNIT_TESTS=57/57 PASS (fresh --rerun-tasks on f972983)
LINT=PASS (lintDebug + lintReleaseVerification)
RELEASE_VERIFICATION=PASS (assembleDebug, assembleReleaseVerification, bundleReleaseVerification)
RELEASE_PREFLIGHT=PASS (exit 0, 2026-09-05T20:03Z)
PAGE_ALIGNMENT=PASS (zipalign -c -P 16 4; bundletool PAGE_ALIGNMENT_16K; 6/6 native ELFs 16 KB LOAD-aligned)
PRIVACY_URL=https://gthgomez.github.io/WallpaperCropFixer/PRIVACY.html
PRIVACY_URL_HTTP_STATUS=200 (verified 2026-09-05)
SIGNING_FAIL_CLOSED=YES (:app:bundleRelease exits 1 with explicit GradleException without RELEASE_* inputs)
PHYSICAL_QA=NOT_EXECUTED (owner; runbook ready in docs/release/DEVICE_QA_RUNBOOK.md)
PLAY_ASSETS=PREPARED (docs/store/; screenshots require a device)
PLAY_DECLARATIONS=DRAFTED (docs/release/DATA_SAFETY_DRAFT.md, PLAY_SUBMISSION_CHECKLIST.md; owner must submit)
```

The uploaded Play AAB is a DIFFERENT artifact: built by the owner locally with
`RELEASE_*` credentials (see docs/release/SIGNING_RUNBOOK.md), from the RC tag
state. The hashes above pin the verification boundary this campaign validated.

## 4. What changed in this campaign

### PR #2 — canonical hardening (merged)
- Immutable-published-preview editor state, operation tokens, generation guards (with deterministic concurrency-test synchronization)
- Fail-closed upload signing (`RELEASE_*` env; `bundleRelease` throws without credentials) + debug-signed `releaseVerification` boundary
- `tools/release-preflight.ps1` (SDK/permissions/signing/dependency/16 KB/secret checks) + `docs/PLAY_RELEASE_PACKET.md` + `docs/signing-policy.json`
- Fully SHA-pinned CI; checksum-verified bundletool + gitleaks; single-source dependency policy (`-DependencyOnly`)

### Polish PR — RC1 bounded change set (merged)
- Copy: "Save" → "Save image"; honest internal-storage export message; hedged entry subtitle; de-jargoned device info; title line-height fix for large font scale
- Accessibility: TalkBack-reachable focus reposition (4 custom accessibility actions on the preview)
- Layout: removed double status-bar / navigation-bar insets on Editor and Settings
- Cleanup: removed unused Coil dependency, dead `FocusPointOverlay.kt`, dead `Destinations.PREVIEW` route

### Trust chain
- GitHub Pages enabled; privacy policy live and verified (HTTP 200) at the URL embedded in the app

## 5. Remaining owner actions
See `docs/release/OWNER_ACTIONS.md` (device QA, keystore custody, Play Console, declarations, submission).

## 6. Explicitly deferred (POST_LAUNCH)
Background-fill UI, dark/dynamic theme, localization, splash screen, androidTest expansion, versionCode automation, release-domain automation, lock-preview focus model redesign (no release-critical defect reproduced).
