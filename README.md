# WallpaperCropFixer

Wallpaper crop and adjustment utility. Uses on-device ML Kit face detection to position crops around faces and handles EXIF orientation correctly.

**Tech stack:** Kotlin, Jetpack Compose, Material3, Hilt, DataStore, ML Kit Face Detection (bundled model), ExifInterface.

**Permissions:** only `SET_WALLPAPER`. Photo selection uses the system Photo Picker with no photo-library permission. The merged manifest also carries `INTERNET`/`ACCESS_NETWORK_STATE` from the ML Kit SDK's telemetry transport — see [PRIVACY.md](PRIVACY.md) for the exact disclosure and the proposed Data Safety declaration.

**Privacy:** fully on-device photo processing; diagnostic telemetry may be sent by the ML Kit SDK per Google's ML Kit data-disclosure documentation. The app itself makes no network calls. [PRIVACY.md](PRIVACY.md) is published to GitHub Pages (`https://gthgomez.github.io/WallpaperCropFixer/PRIVACY.html`).

**Development build:** `.\gradlew.bat :app:assembleDebug`

**CI/release verification (not upload-ready; explicitly debug-signed):**

```text
.\gradlew.bat :app:lintDebug :app:lintReleaseVerification :app:testDebugUnitTest :app:assembleDebug :app:assembleReleaseVerification :app:bundleReleaseVerification
pwsh -File tools/release-preflight.ps1 -RepoRoot . -BundletoolPath <pinned bundletool jar> -GitleaksPath <pinned gitleaks binary>
```

**Future Play upload artifact (dedicated release domain only):**

```text
.\gradlew.bat :app:bundleRelease
```

That task fails closed unless the isolated signing system injects
`RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, and
`RELEASE_KEY_PASSWORD` at runtime. Never use the verification AAB for Play
upload and never store those values in this repository or ordinary CI.

**Detailed docs:** [AGENTS.md](AGENTS.md) | [PROJECT_CONTEXT.md](PROJECT_CONTEXT.md) | [STATUS.md](STATUS.md) | [PRIVACY.md](PRIVACY.md) | [QA_CHECKLIST.md](QA_CHECKLIST.md)
