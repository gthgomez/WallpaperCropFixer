# STORE_LISTING_DRAFT.md — Google Play listing for WallpaperCropFixer

Limits verified 2026-09-05 (title 30 / short 80 / full 4000): https://support.google.com/googleplay/android-developer/answer/13393723
Asset specs verified (feature graphic 1024×500; ≥2 screenshots, PNG/JPEG, 16:9 or 9:16, sides 320–3840 px): https://support.google.com/googleplay/android-developer/answer/9866151

## 1. Titles

| Field | Text | Count |
|---|---|---|
| App name (30 max) | `Wallpaper Crop Fixer: No Crop` | 29/30 |
| Short description (80 max) | `No crop, square fit, blur background — preview before you set it. No ads.` | 73/80 |

## 2. Full description (4000 max) — copy below measured at 3043 chars, large headroom for owner edits

```
Android crops and zooms your photos in unpredictable ways when you set them as wallpapers. A picture that looked great in your gallery can end up framed badly, with faces or details cut off. If you have looked for a "no crop" wallpaper app, a square-fit tool, or a way to fit a photo to your screen, this is built for that: keep the whole photo, preview the real framing, and only then set it.

Wallpaper Crop Fixer previews the rendered framing before you apply it, using a phone-shaped frame so you can see how the image will sit on your home and lock screens. The launcher may still crop or zoom the final wallpaper differently, but you decide with the picture in front of you instead of guessing.

• No crop, no stretching: Safe Fit keeps the entire photo and adds a background only where the aspect ratios differ — the whole photo, not a stretched or chopped crop.
• Square fit and fit to screen: place a tall or wide photo onto a differently shaped screen without losing content. The letterbox area can be filled with a Blur, a photo-derived Color, or a Gradient.
• Three crop modes: Safe Fit keeps the whole photo; Balanced removes at most 35% of the source area for tighter framing and pads when needed; Fill covers the canvas and may crop edges.
• Blur background wallpaper: when a mode adds padding, fill it with a blurred version of your photo, a Color drawn from the photo, or a Gradient — a soft blur background instead of empty bars.
• Tap to position: move the crop focus where you want it. Manual positioning comes first, with automatic face-detected framing and center as defaults.
• Face-aware crop: on-device ML Kit face detection suggests a starting frame so faces stay in view, and you can always adjust it.
• Home, Lock, or Both: choose which screen to set, and preview each surface before you apply.
• Set photo as wallpaper in one flow: pick a photo, preview it, choose Home/Lock/Both, and apply. Export the fitted image to Pictures/WallpaperCropFixer on Android 10+, or an app-specific folder on older versions.

Privacy by design: your photos never leave your device. Everything runs on your phone — face detection is bundled in the app and executes on-device. No ads or accounts. Google ML Kit may collect diagnostic and performance metadata, as disclosed in the privacy policy. The app uses the wallpaper permission and SDK network permissions; it does not request broad photo-library access.

A note on honesty: Android manufacturers handle wallpaper canvases differently (for example, home-screen parallax on Samsung and Pixel devices). Wallpaper Crop Fixer uses Android system sizing hints when valid and bounded, with device-profile estimates as fallback. Exact launcher scaling on every device is not guaranteed. Low-resolution images may appear pixelated, and the app warns you when that is likely.

If you have ever set a wallpaper and been surprised by the crop — or wanted the whole photo, a square fit, or a blur background instead of a stretched crop — try Wallpaper Crop Fixer: see it before you set it.
```

Guardrails honored: no superlatives ("perfectly", "best"), no unverifiable optimization or exact-launcher promises. The no-crop / square-fit / fit-to-screen wording is truthful because Safe Fit keeps the entire source photo and pads with a letterbox background; Balanced and Fill are still described as cropping-capable. Draft remains subject to owner approval and physical QA.

## 3. Category & tags
- Category: **Personalization** (OWNER_MUST_CONFIRM: confirm picker shows it).
- Suggested tags (Play tag picker availability varies): wallpaper, photo editing/customization, home screen. OWNER_MUST_CONFIRM exact selectable tags.

## Store keywords & positioning

Discovery mismatch found in competitive research: the listing used "wallpaper" language only, while users search for "no crop", "square fit", "fit photo to screen", "blur background wallpaper", "set photo as wallpaper", and "wallpaper without cropping". Two close "Square Fit / No Crop" apps were pulled from Google Play, and "Image 2 Wallpaper" (11M historical installs, removed 2024) left an audience whose top complaint was "no lock screen / no true fill" — our exact pitch.

Targeted search terms, honest rationale:

| Search term | Why we can claim it (feature that already exists) |
|---|---|
| no crop | Safe Fit keeps the entire source photo and pads the gap; nothing is chopped, so "no crop" is accurate for that mode. |
| square fit | Safe Fit letterboxes a non-square photo against a filled background — the square-fit behavior users want, without a stretched crop. |
| fit photo to screen | Safe Fit + fit-to-screen preview show the whole photo on a differently shaped screen before applying. |
| blur background wallpaper | Letterbox background finish offers Blur (plus photo-derived Color and Gradient). |
| set photo as wallpaper | Core function: Home/Lock/Both apply flow with a preview. |
| wallpaper without cropping | Same as "no crop" — Safe Fit preserves the whole photo with a letterbox background. |

- Rationale note: these terms describe behavior that already ships; no new feature is implied, and Balanced/Fill remain honestly labeled as cropping modes.
- ⚠ We must NOT claim we can edit or repair the *currently set* wallpaper. Android API 34+ exposes no supported way to read or modify an already-applied wallpaper, so all copy is phrased around previewing and setting a NEW wallpaper (or exporting the fitted image). Do not add "fixes your existing wallpaper" phrasing.

## 4. Screenshot plan (5 phone screenshots, 9:16 portrait, 1080×1920 or 1080×2400, PNG/JPEG)

| # | Screen shown | Headline (on-image) | Supporting copy | Benefit |
|---|---|---|---|---|
| 1 | Before/after preview: a photo cropped by default wallpaper behavior beside the same photo shown whole in the app's phone-shaped frame (Safe Fit) | **See it before you set it** | Left: cropped by default. Right: whole photo, letterboxed in-app. Launcher framing may vary | No more surprises after Apply |
| 2 | Preview with a tap reticle moved off-center, face in frame | **Keep what matters in frame** | Tap anywhere to move the focus; face-aware framing starts you off | Adjust subject placement |
| 3 | Target selector with Home / Lock / Both + per-surface toggle | **Home, Lock, or Both** | Preview each surface, then set one or both | One app for both screens |
| 4 | Crop-mode segmented control (Safe Fit / Balanced / Fill) with the letterbox background clearly visible (Blur, Color and Gradient options shown) | **Whole photo, not a stretched crop** | Safe Fit keeps the whole photo; choose a Blur, Color or Gradient background | No-crop fit with a blur background |
| 5 | Success state after Apply | **Private by design** | Photos processed on-device. No ads. SDK diagnostics disclosed in the privacy policy. | Trust built into the flow |

Rules: device-frame screenshots captured on Galaxy S25 Ultra (primary certification device); no UI elements from unreleased features; text on images ≥ readable at thumbnail size. Screenshot 1 must make the no-crop benefit visible at thumbnail size — the "after" frame shows the complete photo (Safe Fit), clearly un-cropped, next to the badly cropped "before".

## 5. Feature graphic concept (1024×500, JPEG or 24-bit PNG without alpha)
- **Composition (split):** left half — a photo badly cropped by default wallpaper behavior (subject's head cut, misaligned); right half — the same photo inside the app's phone-shaped preview frame, correctly framed, with a subtle "before → after" arrow.
- **Style:** app visual language — light background, photo-first (the imagery does the talking), minimal chrome, generous margins so Play's rounded-corner overlay never clips content; keep key elements inside the central ~80% safe area; no text besides the optional small wordmark (feature graphics often appear without any text support).
- **What to avoid:** promotional badges ("FREE!"), stars, awards, device buttons, or anything resembling Play UI.

## 6. Contact fields
- Email: OWNER_MUST_CONFIRM (supply the real public support address).
- Website: `https://github.com/gthgomez/WallpaperCropFixer` (public repo) or the Pages site (live).
- Privacy policy URL: `https://gthgomez.github.io/WallpaperCropFixer/PRIVACY.html` (live, verified 2026-09-05).
