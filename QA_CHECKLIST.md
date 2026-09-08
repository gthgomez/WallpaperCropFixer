# WallpaperCropFixer — Release and Play QA Checklist

Every item below is executable and has an explicit evidence owner. An unchecked
physical or Play item is not an automated PASS.

Physical-device certification uses the condensed runbook in
`docs/release/DEVICE_QA_RUNBOOK.md`; Play Console steps live in
`docs/release/PLAY_SUBMISSION_CHECKLIST.md`.

## AUTOMATED — VERIFIED

- [x] Local: `:app:testDebugUnitTest`; 57 tests passed, including crop math/property tests, viewport transforms, renderer opacity, image decode bounds, export destinations, Compose semantics, and deterministic ViewModel revision races.
- [x] Local: `:app:lintDebug` and `:app:lintReleaseVerification`; zero errors.
- [x] Local: `:app:assembleReleaseVerification` and `:app:bundleReleaseVerification`; R8 and resource shrinking complete.
- [x] Local: `pwsh -File tools/release-preflight.ps1 ...`; automated checks PASS, with explicit OWNER ACTION and PHYSICAL DEVICE REQUIRED results.
- [x] Local: `:app:bundleRelease` fails closed when the four `RELEASE_*` signing inputs are absent.
- [x] Local: merged verification manifest contains only expected permissions and no exported debug/provider component.
- [x] Local: APK ZIP alignment, AAB `PAGE_ALIGNMENT_16K`, and every packaged native ELF LOAD segment independently pass.
- [x] Local: no tracked `local.properties`, keystore, certificate private key, signing password, `.env`, or test personal data.
- [x] Source/config review: backup/data-extraction rules include only DataStore preferences and exclude cache/source photos.

## EMULATOR — VERIFIED / BLOCKED

- [ ] Launch the verification APK on an API 35/36 emulator.
- [ ] Exercise entry screen, mocked/test image selection path, editor render, crop mode change, target change, focus tap, Save, and Apply where the emulator supports wallpaper APIs.
- [ ] Recreate the activity/process and record whether navigation returns to the editor with the selected image. Current implementation persists editor options only; it does not claim automatic editor-route reconstruction after a cold app restart.
- [ ] Run large-image fixtures where the emulator has sufficient memory; record failures rather than inferring physical-device behavior.
- [ ] Instrumented Compose semantics smoke is not a required CI matrix because no emulator is provisioned in the inexpensive deterministic workflow.

## PHYSICAL DEVICE — REQUIRED

Minimum matrix:

| Device class | Required checks |
|---|---|
| Modern Android 15/16-class phone | Photo Picker, EXIF-rotated image, face/no-face/multi-face, Safe Fit/Balanced/Fill, blur/gradient/solid, HOME/LOCK/BOTH, Save, Apply, rotation, large and very large photos, HEIC |
| Legacy API 26–28 device | Picker without storage permission, rendering, Apply where supported, Save to app-specific external folder and truthful message |
| Samsung One UI | HOME/LOCK/BOTH wallpaper behavior, launcher crop/zoom/parallax, face-aware framing |
| Google Pixel | HOME/LOCK/BOTH behavior, launcher crop/zoom/parallax, face-aware framing |
| Available Motorola/OnePlus/Xiaomi | Repeat HOME/LOCK/BOTH and launcher behavior checks; record only devices actually tested |

Also verify with TalkBack, large font/display scaling, dark/system mode, landscape,
split-screen, tablet/foldable-sized windows, long OEM labels, revoked URI,
corrupt/unsupported image, insufficient storage, wallpaper policy restriction,
unsupported lock screen, cancellation during load/render/apply/save, and repeated
rapid Apply/Save taps. Capture memory/OOM behavior for 50 MP, 100 MP, extreme
panorama/tall images, blur backgrounds, HOME+LOCK, and export/apply after render.

Privacy traffic capture must separately verify the app's own network behavior,
ML Kit destinations, that selected photo bytes do not leave the device, and that
only expected diagnostic data is emitted:

**BLOCKED — PHYSICAL DEVICE PRIVACY VERIFICATION REQUIRED** until executed.

## PLAY CONSOLE — OWNER ACTION

- [ ] Replace `OWNER_PROVIDE_PLAY_DEVELOPER_ENTITY` and `OWNER_PROVIDE_PUBLIC_PRIVACY_CONTACT` in `PRIVACY.md`.
- [x] Agent (2026-09-05): GitHub Pages enabled and the "Publish Privacy Policy" workflow deployed; `https://gthgomez.github.io/WallpaperCropFixer/PRIVACY.html` verified with an unauthenticated HTTP 200.
- [ ] Enter the privacy URL in Play Console and link the same policy from the app.
- [ ] Complete Data Safety using the advisory worksheet and physical traffic evidence.
- [ ] Complete content rating and target-audience questionnaires using the packet; do not claim unsupported features.
- [ ] Upload only the dedicated release-domain artifact signed with the future upload key. Verification artifacts are never Play-upload artifacts.
- [ ] For personal accounts created after 13 November 2023, if applicable, run a closed test with at least 12 opted-in testers continuously for at least 14 days before requesting production access.
- [ ] Collect tester feedback on the core workflow: choose photo → adjust framing → preview → Save/Apply; record device/OS/OEM and failures.

## SIGNING — SEPARATE RELEASE SYSTEM

- [ ] Dedicated release domain injects `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, and `RELEASE_KEY_PASSWORD` at runtime only.
- [ ] Verify the upload certificate against the Play Console upload-key record in that domain; record only the public fingerprint in the owner-controlled release record.
- [ ] Do not generate, access, persist, or request key material in this repository or ordinary CI.

## Go / no-go interpretation

Engineering automation may be green while owner/device sections remain open. The
minimum internal-testing verdict requires the verification build and packet plus
owner-provided public privacy identity/contact and a working unauthenticated
privacy URL. Production readiness additionally requires the secure signing
system, physical-device evidence, and applicable Play testing/access actions.
