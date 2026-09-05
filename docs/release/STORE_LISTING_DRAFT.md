# STORE_LISTING_DRAFT.md — Google Play listing for WallpaperCropFixer

Limits verified 2026-09-05 (title 30 / short 80 / full 4000): https://support.google.com/googleplay/android-developer/answer/13393723
Asset specs verified (feature graphic 1024×500; ≥2 screenshots, PNG/JPEG, 16:9 or 9:16, sides 320–3840 px): https://support.google.com/googleplay/android-developer/answer/9866151

## 1. Titles

| Field | Text | Count |
|---|---|---|
| App name (30 max) | `Wallpaper Crop Fixer` | 20/30 |
| Short description (80 max) | `Preview how your photo fits home and lock screens before you set it. No ads.` | 76/80 |

## 2. Full description (4000 max) — copy below measured at 1708 chars, large headroom for owner edits

```
Android crops and zooms your photos in unpredictable ways when you set them as wallpapers. A picture that looked great in your gallery can end up framed badly, with faces or details cut off.

Wallpaper Crop Fixer shows you a true-proportion preview of exactly how your photo will fit before anything is applied. A phone-shaped frame displays the real crop — three taps from open to applied.

• Tap to position: move the crop focus where you want it. Manual positioning comes first, with automatic face-detected framing and center as defaults.
• Home, Lock, or Both: choose which screen to set, and preview each surface before you apply.
• Three crop modes: Safe Fit letterboxes when more than 20% of your photo would be cropped; Balanced allows a tighter crop up to 40%; Fill never letterboxes.
• Letterbox backgrounds: a blurred version of your photo (default), a gradient, or a solid color.
• Export: save the fitted image to Pictures/WallpaperCropFixer for reuse.

Privacy by design: your photos never leave your device. Everything runs on your phone — face detection is bundled in the app and executes on-device. No ads. No analytics. No tracking. No accounts. The app requests a single permission: setting your wallpaper.

A note on honesty: Android manufacturers handle wallpaper canvases differently (for example, home-screen parallax on Samsung and Pixel devices). Wallpaper Crop Fixer estimates this behavior as best it can, but exact scaling on every device is not guaranteed. Low-resolution images may appear pixelated, and the app warns you when that is likely.

If you have ever set a wallpaper and been surprised by the crop, try Wallpaper Crop Fixer — see it before you set it.
```

Guardrails honored: no superlatives ("perfectly", "best"), no unverifiable claims — every feature sentence maps to a code-audited fact; OEM limitation disclosed up front.

## 3. Category & tags
- Category: **Personalization** (OWNER_MUST_CONFIRM: confirm picker shows it).
- Suggested tags (Play tag picker availability varies): wallpaper, photo editing/customization, home screen. OWNER_MUST_CONFIRM exact selectable tags.

## 4. Screenshot plan (5 phone screenshots, 9:16 portrait, 1080×1920 or 1080×2400, PNG/JPEG)

| # | Screen shown | Headline (on-image) | Supporting copy | Benefit |
|---|---|---|---|---|
| 1 | Main preview: photo on phone-shaped frame | **See it before you set it** | The frame shows the real crop your wallpaper will get | No more surprises after Apply |
| 2 | Preview with a tap reticle moved off-center, face in frame | **Keep what matters in frame** | Tap anywhere to move the focus; face-aware framing starts you off | Your subject survives the crop |
| 3 | Target selector with Home / Lock / Both + per-surface toggle | **Home, Lock, or Both** | Preview each surface, then set one or both | One app for both screens |
| 4 | Crop-mode segmented control (Safe Fit / Balanced / Fill), letterbox visible | **Fit it your way** | Letterbox when you want it, fill when you don't — blurred, gradient, or solid backgrounds | Control over how much of the photo shows |
| 5 | Success state after Apply | **Private by design** | Photos stay on your device. No ads. No tracking. One permission. | Trust built into the flow |

Rules: device-frame screenshots captured on Galaxy S25 Ultra (primary certification device); no UI elements from unreleased features; text on images ≥ readable at thumbnail size.

## 5. Feature graphic concept (1024×500, JPEG or 24-bit PNG without alpha)
- **Composition (split):** left half — a photo badly cropped by default wallpaper behavior (subject's head cut, misaligned); right half — the same photo inside the app's phone-shaped preview frame, correctly framed, with a subtle "before → after" arrow.
- **Style:** app visual language — light background, photo-first (the imagery does the talking), minimal chrome, generous margins so Play's rounded-corner overlay never clips content; keep key elements inside the central ~80% safe area; no text besides the optional small wordmark (feature graphics often appear without any text support).
- **What to avoid:** promotional badges ("FREE!"), stars, awards, device buttons, or anything resembling Play UI.

## 6. Contact fields
- Email: OWNER_MUST_CONFIRM (personal gmail placeholder in repo docs must be replaced).
- Website: `https://github.com/gthgomez/WallpaperCropFixer` (public repo) or the Pages site once live.
- Privacy policy URL: `https://gthgomez.github.io/WallpaperCropFixer/PRIVACY.html` (must be live before submission).
