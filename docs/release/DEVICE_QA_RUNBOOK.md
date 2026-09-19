# DEVICE_QA_RUNBOOK.md — Physical-device QA (v1.0 release candidates)

App: WallpaperCropFixer (`com.wallpapercropfixer`) v1.0 — min SDK 26, target SDK 36.
**Primary certification device: Samsung Galaxy S25 Ultra** — Android wallpaper sizing hints and Samsung launcher crop/zoom behavior must be certified first-hand. OEM multipliers are fallback estimates only. **Secondary (recommended): any current Pixel-class phone.**
Results per check: **PASS / FAIL / BLOCK** (BLOCK = cannot test, e.g. no device).

> **WARNING: Do not mark PASS without executing the check on hardware.** Emulator output, code reading, or prior-session claims do NOT qualify. A false PASS here ships to Play review and to users.

## Setup (before check 1)
Install via `adb install -r app-release.apk`-equivalent from a local signed/debug build, or Android Studio run. Record Settings version name, version code and source commit, artifact SHA-256, device model, Android version and One UI/launcher version. Do not attribute screenshots from unidentified builds to the RC2 candidate.

## Checks (20)

| # | Check | Steps | Expect |
|---|-------|-------|--------|
| 1 | Clean install & launch | Uninstall any prior build → install → launch | App opens, no crash, preview frame renders empty/initial state |
| 2 | Picker opens; cancel is no-op | Tap pick-photo → cancel in system Photo Picker; repeat with back gesture | Returns to app cleanly; no crash; no stale state |
| 3 | Portrait photo preview | Pick a tall (9:16) photo | True-proportion render on phone-shaped frame; no skew/stretch |
| 4 | Landscape photo preview | Pick a wide photo | Safe Fit keeps the complete photo in the rendered canvas and adds background when aspect ratios differ; record any launcher crop separately |
| 5 | Large-image preview | Pick a 50 MP-class photo | No OOM/crash; preview stays responsive |
| 6 | Face-aware framing | Pick a photo with a clear face, face-aware ON | Crop focuses the face by default (on-device ML Kit) |
| 7 | Tap-to-reposition | Tap different frame regions | Crop focus follows taps; manual focus wins over face/center |
| 8 | Safe Fit mode | Use slightly mismatched and very wide/tall photos | Whole photo preserved for every mismatch; background fills remaining canvas |
| 9 | Balanced + Fill modes | Compare fixtures requiring 34.9%, 35.0%, 35.1% removal; move focus and place faces near edges | Balanced crops at most 35%, expanding partially with background above the budget; faces are preserved by further expansion. Fill covers the canvas and may crop |
| 10 | Background finishes | With padding, select Blur, Color and Gradient; then use a plan with no padding | Distinct visible photo-derived finishes; controls are disabled with explanation when current framing exposes no background |
| 11 | Apply to HOME | Target Home → Apply | Home wallpaper matches preview; success feedback shown |
| 12 | Apply to LOCK | Target Lock → Apply | Lock wallpaper matches preview |
| 13 | Apply BOTH + preview toggle | Target Both → Apply; toggle Home/Lock preview | Both surfaces change; per-surface preview toggle reflects each |
| 14 | Export | Check Pictures/WallpaperCropFixer | Fitted image saved at chosen JPEG quality |
| 15 | Rotation / background / restore | Rotate during render and during Save/Apply; edit immediately after completion; background and restore | Fresh canvas metrics eventually publish; stale geometry cannot be saved/applied; operation result is truthful |
| 16 | Operation ownership and partial failures | Save then Apply; try Back/reset/framing controls during each; test a BOTH export with one write failing where feasible | Saving belongs to Save, Applying to Apply; committed work blocks navigation/mutation, rendering does not. Partial exports identify the saved and failed target |
| 17 | Enlarged font + TalkBack smoke | Font scale 1.3–2.0; enable TalkBack | Layout intact; reposition reachable via accessibility actions; Apply reachable |
| 18 | Status-bar / inset layout | Edge-to-edge areas on both devices | Controls not under status bar or cutout |
| 19 | Privacy link opens live policy | Tap privacy link (About/settings) | Opens `https://gthgomez.github.io/WallpaperCropFixer/PRIVACY.html` in browser (Pages must be live first) |
| 20 | Privacy surface audit | Settings → Apps → WallpaperCropFixer: permissions; optional packet capture / ADB `dumpsys netpolicy` spot-check | No unexpected permissions; merged SDK network permissions and documented diagnostic traffic are assessed separately. Verify photo bytes and face results remain local; a quiet capture does not prove zero SDK collection |

## Results table (fill per device)

| Device / OS | #1 | #2 | #3 | #4 | #5 | #6 | #7 | #8 | #9 | #10 | #11 | #12 | #13 | #14 | #15 | #16 | #17 | #18 | #19 | #20 | Notes |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Galaxy S25 Ultra / One UI ____ | | | | | | | | | | | | | | | | | | | | | |
| Pixel-class / Android ____ | | | | | | | | | | | | | | | | | | | | | |

## Known honest limits to verify as "documented behavior", not bugs
- Low-resolution source shows a "may appear pixelated" warning (by design).
- HOME uses valid bounded Android desired minimum dimensions first, then OEM/generic estimates; LOCK uses physical screen dimensions. Record the actual canvas, launcher framing and S25 Ultra results separately.

**Gate: all 20 = PASS on S25 Ultra (and no FAIL/BLOCK elsewhere) before the signed AAB upload.**

RC2 evidence status: **all physical checks pending** until the result table is completed for the exact candidate. No RC2 tag before required physical evidence or explicit owner override.
