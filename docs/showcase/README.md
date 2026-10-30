# Showcase capture status

Source inspected: [`101c480173244af7c491ef48d287b8bc41f75b18`](https://github.com/gthgomez/WallpaperCropFixer/tree/101c480173244af7c491ef48d287b8bc41f75b18).
Inspection date: 2026-10-30. This is a source/documentation pass, not a recorded app run.

## Evidence available

No verified runtime screenshots or clips are checked into this repository at the
inspected revision. No new runtime media was created for this pass.

## Execution blocker

The selected cloud environment has JDK 21 but no Android SDK installation,
`ANDROID_HOME`/`ANDROID_SDK_ROOT`, `adb`, `emulator`, `sdkmanager`, `/dev/kvm`,
or attached Android device. This repository’s existing JDK 17 setup and Android
prerequisites were inspected rather than replaced with a large toolchain download.

Required next setup: JDK 17, Android SDK platform 36, and an Android 8.0+ device or emulator. Wallpaper application and OEM behavior need a physical device.

No Android build, face-detection, export, wallpaper-application, or physical-device check was executed for this showcase.

## Capture a genuine demonstration

Use a synthetic landscape/portrait image with no personal metadata. Show selection, crop/fill changes, preview, and Save. Test Apply only on a dedicated test device where changing wallpaper is intended; retain separate evidence for OEM and physical-device behavior.

After installing and manually opening the debug build on the test device, record
screens using Android's actual display output (requires Android platform tools):

```sh
mkdir -p docs/showcase
adb exec-out screencap -p > docs/showcase/screen.png
# Stop recording with Ctrl-C after the short walkthrough.
adb shell screenrecord --time-limit 20 /sdcard/showcase.mp4
adb pull /sdcard/showcase.mp4 docs/showcase/demo.mp4
adb shell rm /sdcard/showcase.mp4
```

Inspect every frame before publication. Keep the clip short and compress it for
GitHub; do not add large model/APK/build/ROM/save files. Add captions identifying
source commit, build variant, device/Android version, fixture origin, and which
steps were really exercised. Scripted UI states and design references must stay
labeled separately from device and end-to-end evidence.

## Source and verification references

[Project context](../PROJECT_CONTEXT.md), [QA checklist](../../QA_CHECKLIST.md), and [privacy](../../PRIVACY.md).

This pass changes README/capture documentation only. Local paths and links are
checked; Android Gradle, connected, release, and hardware gates remain unrun.
