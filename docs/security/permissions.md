# Permission report — Wallpaper Crop Fixer

**Variant audited:** `releaseVerification` (merged manifest)
**As of:** 2026-09-29, source revision `ce002089d3ff9aabcacffbf9cb5ed7a2121eadcc` (main), branch `audit/WC01-20260929`
**Source:** `app/src/main/AndroidManifest.xml` compared against the merged manifest produced by
`./gradlew :app:assembleReleaseVerification` (see "Verification" below), plus the
declared Gradle dependency set (`gradle/libs.versions.toml`, `app/build.gradle.kts`).

## App-owned permissions

Declared directly in `app/src/main/AndroidManifest.xml`:

| Permission | Reason |
| --- | --- |
| `android.permission.SET_WALLPAPER` | Applying a cropped bitmap via the system `WallpaperManager`. |

That is the complete app-owned set. The app requests no storage, camera, or
photo-library permission; image selection goes through the system Photo Picker
(`PickVisualMedia`).

## Merged-manifest permissions (dependency-contributed)

The merged `releaseVerification` manifest carries permissions the app does not
declare itself. They come from the bundled ML Kit Face Detection SDK
(`com.google.mlkit:face-detection:16.1.7`, pulled in via
`gradle/libs.versions.toml` → `app/build.gradle.kts`) and its transport
dependencies:

| Permission | Source | Effect |
| --- | --- | --- |
| `android.permission.INTERNET` | ML Kit SDK transport | Allows the SDK to transmit diagnostic/performance telemetry to Google, as disclosed in [PRIVACY.md](../../PRIVACY.md). |
| `android.permission.ACCESS_NETWORK_STATE` | ML Kit SDK transport | Lets the SDK check connectivity before sending diagnostics. |

The merged manifest may also declare a signature-level custom permission of the
form `com.wallpapercropfixer.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`
(API 33+ dynamic-receiver protection added by the manifest merger). It is not a
user-visible permission and grants no external access.

## Interpretation

- `INTERNET`/`ACCESS_NETWORK_STATE` are **not** used by app-owned code. The
  application itself makes no network calls; the only network capability in the
  merged manifest exists to serve ML Kit SDK telemetry, which is disclosed —
  not denied — in PRIVACY.md. No claim of "no network permission" or "no
  telemetry" is made anywhere in the documentation set.
- `tools/release-preflight.ps1` enforces exactly this allow-list
  (`SET_WALLPAPER`, `INTERNET`, `ACCESS_NETWORK_STATE`, the dynamic-receiver
  permission) against the merged `releaseVerification` manifest and fails on
  any unexpected permission.

## Backup and data extraction exclusions

- `app/src/main/res/xml/backup_rules.xml` (Android ≤11,
  `fullBackupContent`) is an **include-only** rule set: only `sharedpref` and
  the DataStore directory are included. Everything else — including
  `cacheDir`, where the session photo copy lives
  (`AppEntryScreen.kt`, `File(context.cacheDir, "wcf_pick_…")`) — is excluded
  from Auto Backup. Additionally, Android never backs up cache directories.
- `app/src/main/res/xml/data_extraction_rules.xml` (Android 12+) mirrors the
  same include-only intent for `cloud-backup` and `device-transfer`.
- Selected photos are therefore **not** present in cloud backups or device
  transfers, as claimed in PRIVACY.md. Preferences (crop mode, wallpaper
  target, face-aware preference, export quality) intentionally are backed up.

## Verification

Commands and results are recorded in the WC01 acceptance receipt for this
branch. The `tools/release-preflight.ps1` merged-manifest permission check and
privacy-disclosure checks run against the `releaseVerification` build output
(`app/build/intermediates/merged_manifests/releaseVerification/`).
