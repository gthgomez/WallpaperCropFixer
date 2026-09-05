# Store assets — capture and export plan

Nothing in this directory is published. Final PNG/JPEG exports are produced by
the owner (or agent on request) from the sources here plus screenshots captured
on a real device.

## Feature graphic (1024×500)

- Source: `feature-graphic-source.svg` (this directory).
- Export: render at exactly 1024×500, 24-bit PNG or JPEG, **no alpha**
  (Google Play rejects alpha in feature graphics).
- Keep the visual language of the app: light surface, photo-first, minimal
  chrome; no decorative AI-style artwork.

## Screenshots (minimum 2, maximum 8 per device type; PNG/JPEG; 9:16 or 16:9; 320–3840 px sides)

Capture the debug build on a 1080×2400-class device/emulator with the fixture
images below on screen. Recommended sequence (full copy per shot lives in
`../release/STORE_LISTING_DRAFT.md`):

| # | Screen shown | Headline |
|---|---|---|
| 1 | Entry/preview with a portrait photo loaded | See it before you set it |
| 2 | Editor with focus crosshair mid-reposition | Keep what matters in frame |
| 3 | Apply-to selector (Home / Lock / Both) | Home, Lock, or Both |
| 4 | Crop-mode chips (Safe Fit / Balanced / Fill) | Fit it your way |
| 5 | Success state | Private by design |

Capture rules:

- Use photos with no identifiable real people unless they are owned by the
  project; avoid stock watermarks.
- Clean status bar (full battery/wifi, no debug icons) for a trustworthy look.
- Frame screenshots in a device template only if consistent across all shots;
  Play also accepts raw captures.
