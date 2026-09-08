# DEVICE_QA_RUNBOOK.md — Physical-device QA (v1.0 release candidates)

App: WallpaperCropFixer (`com.wallpapercropfixer`) v1.0 — min SDK 26, target SDK 36.
**Primary certification device: Samsung Galaxy S25 Ultra** — per-manufacturer wallpaper canvas behavior (Samsung/Pixel parallax multipliers) is the app's core estimate and differentiator, so Samsung must be certified first-hand. **Secondary (recommended): any current Pixel-class phone.**
Results per check: **PASS / FAIL / BLOCK** (BLOCK = cannot test, e.g. no device).

> **WARNING: Do not mark PASS without executing the check on hardware.** Emulator output, code reading, or prior-session claims do NOT qualify. A false PASS here ships to Play review and to users.

## Setup (before check 1)
Install via `adb install -r app-release.apk`-equivalent from a local signed/debug build, or Android Studio run. Note device model, Android version, One UI/version in the results table.

## Checks (20)

| # | Check | Steps | Expect |
|---|-------|-------|--------|
| 1 | Clean install & launch | Uninstall any prior build → install → launch | App opens, no crash, preview frame renders empty/initial state |
| 2 | Picker opens; cancel is no-op | Tap pick-photo → cancel in system Photo Picker; repeat with back gesture | Returns to app cleanly; no crash; no stale state |
| 3 | Portrait photo preview | Pick a tall (9:16) photo | True-proportion render on phone-shaped frame; no skew/stretch |
| 4 | Landscape photo preview | Pick a wide photo | Frame shows honest crop; Safe Fit letterboxes if >20% would be cropped |
| 5 | Large-image preview | Pick a 50 MP-class photo | No OOM/crash; preview stays responsive |
| 6 | Face-aware framing | Pick a photo with a clear face, face-aware ON | Crop focuses the face by default (on-device ML Kit) |
| 7 | Tap-to-reposition | Tap different frame regions | Crop focus follows taps; manual focus wins over face/center |
| 8 | Safe Fit mode | Wide photo, >20% would crop | Letterbox appears; background = blurred image (default) |
| 9 | Balanced + Fill modes | Toggle >40% threshold; Fill | Balanced letterboxes; Fill never letterboxes |
| 10 | Letterbox background (blur) | Wide photo in Safe Fit | Letterbox areas show the blurred-image background (the shipped default; gradient/solid options are post-launch — no UI exists yet) |
| 11 | Apply to HOME | Target Home → Apply | Home wallpaper matches preview; success feedback shown |
| 12 | Apply to LOCK | Target Lock → Apply | Lock wallpaper matches preview |
| 13 | Apply BOTH + preview toggle | Target Both → Apply; toggle Home/Lock preview | Both surfaces change; per-surface preview toggle reflects each |
| 14 | Export | Check Pictures/WallpaperCropFixer | Fitted image saved at chosen JPEG quality |
| 15 | Rotation / background / restore | Rotate mid-preview; background app; restore | State and crop focus preserved; no crash |
| 16 | Success + error feedback | Valid apply; then force an error (e.g. revoke wallpaper path where possible / low storage sim) | Clear success toast/UI; error path is user-readable, not silent |
| 17 | Enlarged font + TalkBack smoke | Font scale 1.3–2.0; enable TalkBack | Layout intact; reposition reachable via accessibility actions; Apply reachable |
| 18 | Status-bar / inset layout | Edge-to-edge areas on both devices | Controls not under status bar or cutout |
| 19 | Privacy link opens live policy | Tap privacy link (About/settings) | Opens `https://gthgomez.github.io/WallpaperCropFixer/PRIVACY.html` in browser (Pages must be live first) |
| 20 | Privacy surface audit | Settings → Apps → WallpaperCropFixer: permissions; optional packet capture / ADB `dumpsys netpolicy` spot-check | Only **SET_WALLPAPER** listed; no unexpected network traffic during a normal session |

## Results table (fill per device)

| Device / OS | #1 | #2 | #3 | #4 | #5 | #6 | #7 | #8 | #9 | #10 | #11 | #12 | #13 | #14 | #15 | #16 | #17 | #18 | #19 | #20 | Notes |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Galaxy S25 Ultra / One UI ____ | | | | | | | | | | | | | | | | | | | | | |
| Pixel-class / Android ____ | | | | | | | | | | | | | | | | | | | | | |

## Known honest limits to verify as "documented behavior", not bugs
- Low-resolution source shows a "may appear pixelated" warning (by design).
- Home-screen parallax/canvas estimates vary per manufacturer — record S25 Ultra actuals in Notes to calibrate future releases.

**Gate: all 20 = PASS on S25 Ultra (and no FAIL/BLOCK elsewhere) before the signed AAB upload.**
