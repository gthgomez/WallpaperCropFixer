# WallpaperCropFixer

Wallpaper crop and adjustment utility. Uses on-device ML Kit face detection to position crops around faces and handles EXIF orientation correctly.

> **Status: proprietary.** This repository is public for source visibility and
> transparency. It is **not open source** — there is no license grant to reuse,
> modify, or redistribute this code. See [LICENSE](LICENSE).

## Showcase — Fit a photo to your wallpaper

Select a photo with the system Photo Picker, adjust its wallpaper framing, and preview the result before saving or applying it. EXIF handling and on-device face detection help position the crop.

| Explore | What the app provides |
| --- | --- |
| Pick | System Photo Picker without photo-library permission |
| Frame | Crop/fill controls and device previews |
| Export | Save or apply the currently published preview |

**Demo status:** runtime screenshots and video are pending. The README showcase pass
on 2026-10-30 inspected source at [`101c48017324`](https://github.com/gthgomez/WallpaperCropFixer/tree/101c480173244af7c491ef48d287b8bc41f75b18);
this cloud environment has no Android SDK, emulator, or attached Android device.
See the [capture checklist and evidence limits](docs/showcase/README.md).

### Try it locally

JDK 17, Android SDK platform 36, and an Android 8.0+ device or emulator. Wallpaper application and OEM behavior need a physical device.

From the repository root (Windows: `gradlew.bat`):

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug
./gradlew :app:installDebug
```

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

**Project docs:** [docs/](docs/) | [STATUS.md](STATUS.md) | [PRIVACY.md](PRIVACY.md) | [QA_CHECKLIST.md](QA_CHECKLIST.md)

**Agent instructions:** [AGENTS.md](AGENTS.md); technical context in [docs/PROJECT_CONTEXT.md](docs/PROJECT_CONTEXT.md) (task data).

## License

License: Proprietary - source available for viewing; this project is not open source.

Copyright is retained by the project owner. No permission is granted to
redistribute, modify, sublicense, sell, commercially exploit, or create
derivative works from this project except where required by applicable law.
Third-party dependencies, including ML Kit, remain under their own licenses.
See [`LICENSE`](LICENSE).
