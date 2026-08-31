# WallpaperCropFixer

Wallpaper crop and adjustment utility. Uses on-device ML Kit face detection to position crops around faces and handles EXIF orientation correctly.

**Tech stack:** Kotlin, Jetpack Compose, Material3, Hilt, DataStore, ML Kit Face Detection (bundled model), ExifInterface.

**Permissions:** only `SET_WALLPAPER`. Photo selection uses the system Photo Picker with no photo-library permission. The merged manifest also carries `INTERNET`/`ACCESS_NETWORK_STATE` from the ML Kit SDK's telemetry transport — see [PRIVACY.md](PRIVACY.md) for the exact disclosure and the proposed Data Safety declaration.

**Privacy:** fully on-device photo processing; diagnostic telemetry may be sent by the ML Kit SDK per Google's ML Kit data-disclosure documentation. The app itself makes no network calls. [PRIVACY.md](PRIVACY.md) is published to GitHub Pages (`https://gthgomez.github.io/WallpaperCropFixer/PRIVACY.html`).

**Build:** `.\gradlew.bat :app:assembleDebug`

**Release verification:**

```text
.\gradlew.bat :app:lintRelease :app:testDebugUnitTest :app:assembleRelease :app:bundleRelease
zipalign -c -P 16 -v 4 app/build/outputs/apk/release/app-release.apk
```

**Detailed docs:** [AGENTS.md](AGENTS.md) | [PROJECT_CONTEXT.md](PROJECT_CONTEXT.md) | [STATUS.md](STATUS.md) | [PRIVACY.md](PRIVACY.md) | [QA_CHECKLIST.md](QA_CHECKLIST.md)

## License

Source code is publicly viewable for development and transparency purposes. This project is not open source. See [LICENSE.md](LICENSE.md) for permitted use. Third-party libraries and SDKs retain their own licenses.

