# Wallpaper Crop Fixer — Play Release Packet

Status: engineering packet prepared. This document is not evidence that a Play
upload or physical-device test occurred.

## App identity

| Field | Value / status |
|---|---|
| Application ID | `com.wallpapercropfixer` |
| App name | Wallpaper Crop Fixer |
| Version code / name | `1` / `1.0` |
| Min SDK | 26 |
| Compile / target SDK | 36 / 36 |
| Category | Photography or Tools; owner selects the Play category |
| Ads | No; dependency inspection shows no advertising SDK |
| Account / app access | No account, login, or special access required |
| Privacy URL | `https://gthgomez.github.io/WallpaperCropFixer/PRIVACY.html`; OWNER ACTION: enable Pages and verify unauthenticated HTTP success |
| Developer/entity | OWNER ACTION: match the Play listing and replace the field in `PRIVACY.md` |
| Privacy contact | OWNER ACTION: replace `OWNER_PROVIDE_PUBLIC_PRIVACY_CONTACT` with a durable public mechanism |

## Store listing draft

### Title

Wallpaper Crop Fixer

### Short description

Preview and adjust how your photos fit your Android home and lock screens.

### Full description

Wallpaper Crop Fixer helps you prepare a photo before applying or saving it as
wallpaper. Choose a photo with Android's system Photo Picker, preview the
framing for your device, and adjust the crop with Safe Fit, Balanced, or Fill.

Use face-aware framing when useful, tap the preview to reposition the focus,
choose a blur, gradient, or solid background, and target the home screen, lock
screen, or both. Save an export to the device or apply the rendered wallpaper
through Android's wallpaper service.

Photo processing and face detection are performed on the device. The bundled
Google ML Kit SDK may send diagnostic telemetry described in the privacy policy.
Launcher crop, zoom, and parallax behavior can differ from the preview.

## Data Safety worksheet (proposed; not submitted)

Evidence: `app/build.gradle.kts`, the merged verification manifest, bundled
`com.google.mlkit:face-detection:16.1.7`, `PRIVACY.md`, and Google's ML Kit
disclosure. Runtime traffic and final artifact behavior still require physical
verification.

| Play taxonomy | Collected | Shared | Ephemeral | Required | Purpose | Evidence |
|---|---|---|---|---|---|---|
| Device or other IDs — per-installation identifiers | Yes, SDK diagnostics | No, per ML Kit disclosure | Unknown until runtime/SDK retention verification | Optional to core workflow | Analytics/diagnostics | Bundled ML Kit |
| App info and performance — diagnostics, latency, API configuration, sizes, feature version, error codes | Yes, SDK diagnostics | No, per ML Kit disclosure | Unknown until runtime/SDK retention verification | Optional to core workflow | Analytics/diagnostics | Bundled ML Kit |
| Photos or videos / face data | Accessed for user-requested processing | No | Local memory/cache processing | Optional after photo selection | App functionality | App source; physical traffic capture |

Encryption in transit: ML Kit documents HTTPS for its listed collected data.
The app has no own backend, analytics, ads, account system, or photo upload.
Confirm final answers in Play Console against physical traffic capture.

## Content rating and audience preparation

Expected conservative answers from the current implementation: no violence, no
sexual content, no profanity, no gambling, no location, no account/social
features, and no user interaction with other users. The app displays user-
selected photos and may detect faces locally; the owner must complete the
official questionnaire. Recommended target audience is general audience, with
no child-directed design or child-specific claims.

## Testing instructions

1. Install the explicit `releaseVerification` artifact or the Play internal-test
   artifact supplied by the dedicated release system.
2. Tap Choose a Photo and select a normal portrait or landscape image.
3. Try Safe Fit, Balanced, and Fill; switch HOME, LOCK, and BOTH.
4. Toggle face-aware framing and tap the preview to adjust focus.
5. Save and confirm the reported destination; apply wallpaper and inspect the
   actual launcher result.
6. Repeat with EXIF-rotated, HEIC, no-face, multi-face, and large images.
7. Report device, Android API, OEM launcher, options, expected/actual result,
   and reproduction steps. Do not attach private photos.

For personal developer accounts created after 13 November 2023, Google requires
at least 12 opted-in closed testers continuously for 14 days before production
access; this is conditional on account type and creation date.

## Store assets checklist

- [ ] Owner captures real screenshots from a verified running artifact; no
      functionality screenshot is fabricated.
- [ ] Recommended sequence: entry/photo picker, editor preview, crop modes,
      face-aware/tap focus, BOTH target, Save confirmation, Apply result.
- [ ] Factual captions: “Preview your framing”, “Adjust crop mode”, “Position
      focus”, “Prepare home and lock wallpapers”, “Save the rendered image”,
      and “Apply through Android wallpaper settings”.
- [ ] Feature graphic: 1024×500 px; owner creates/reviews final artwork.
- [ ] Owner validates checked-in adaptive icon resources in Play Console.
- [ ] Owner supplies phone/tablet captures; none are completed by this change.

## Release commands and signing boundary

Verification only:

```text
.\gradlew.bat :app:lintDebug :app:lintReleaseVerification :app:testDebugUnitTest :app:assembleDebug :app:assembleReleaseVerification :app:bundleReleaseVerification
pwsh -File tools/release-preflight.ps1 -RepoRoot . -BundletoolPath <pinned bundletool jar> -GitleaksPath <pinned gitleaks binary>
```

Future upload-ready artifact, only in the dedicated release domain:

```text
.\gradlew.bat :app:bundleRelease
```

The production-semantic task fails closed until runtime-only `RELEASE_*`
credentials are injected. No key, password, certificate fingerprint, or
upload-ready artifact is stored here.

## Official references

- [Target API level requirements](https://support.google.com/googleplay/android-developer/answer/11926878)
- [16 KB page-size support](https://developer.android.com/guide/practices/page-sizes)
- [Google Play privacy policy requirements](https://support.google.com/googleplay/android-developer/answer/15402193)
- [Google Play Data Safety](https://support.google.com/googleplay/android-developer/answer/10787469)
- [ML Kit data disclosure](https://developers.google.com/ml-kit/android-data-disclosure)
- [Personal-account testing requirements](https://support.google.com/googleplay/android-developer/answer/14151465)
- [Play App Signing](https://support.google.com/googleplay/android-developer/answer/9842756)

## Remaining owner/device gates

- OWNER ACTION — supply public developer/entity and privacy contact.
- OWNER ACTION — enable Pages or choose another public non-geofenced HTML host;
  verify the URL without authentication.
- OWNER ACTION — enable repository dependency graph/Advanced Security if the
  account plan supports the vulnerability review gate.
- PHYSICAL DEVICE REQUIRED — privacy traffic capture, memory/OOM, TalkBack,
  launcher behavior, rotation, large-image, HEIC, and OEM matrix.
- SECURE SIGNING SYSTEM REQUIRED — create/store/use the per-app upload key in
  the separate release domain and configure Play App Signing.
- PLAY CONSOLE OWNER ACTION — listing, Data Safety, rating, audience, testing,
  and production access decisions.
