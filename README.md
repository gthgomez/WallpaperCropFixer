# WallpaperCropFixer

Wallpaper crop and adjustment utility. Uses on-device ML Kit face detection to position crops around faces and handles EXIF orientation correctly.

**Tech stack:** Kotlin, Jetpack Compose, Material3, Hilt, DataStore, Coil, ML Kit Face Detection (bundled model), ExifInterface.

**Permissions:** `SET_WALLPAPER`. Photo selection uses the system Photo Picker without `READ_MEDIA_IMAGES`. `READ_EXTERNAL_STORAGE` is declared with `maxSdkVersion="32"` for legacy gallery access on older Android versions.

**Build:** `.\gradlew.bat :app:assembleDebug`

**Detailed docs:** [AGENTS.md](AGENTS.md) | [PROJECT_CONTEXT.md](PROJECT_CONTEXT.md) | [STATUS.md](STATUS.md) | [PRIVACY.md](PRIVACY.md) | [QA_CHECKLIST.md](QA_CHECKLIST.md)
