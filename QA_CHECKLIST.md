# WallpaperCropFixer — Pre-Release QA Checklist

This checklist describes the **actual** application (focus-tap preview, crop modes,
Home/Lock/Both targets, blur/gradient/solid backgrounds, Save/Apply). Every item is
physically executable. Run each item on a real device before publishing.

Legend: `[ ]` unchecked · `[x]` checked · device icon means "physical device required".

---

## 1. Installation / startup

- [ ] Clean install from a release build (debug builds are not a substitute for release QA)
- [ ] App opens on the entry screen with title, subtitle, and "Choose a Photo" button
- [ ] No storage/photo permission dialog is ever shown
- [ ] Settings gear opens the settings screen and Back returns

## 2. Pick image

- [ ] System Photo Picker opens when tapping "Choose a Photo"
- [ ] Selecting a photo shows "Preparing photo…" then navigates to the editor
- [ ] Choosing a GIF/PNG/WebP/JPEG/HEIC all load successfully
- [ ] Cancelling the picker returns to the entry screen with no error

## 3. Replace / reselect image

- [ ] From the editor, going Back then choosing a different photo loads the new photo
- [ ] **Rapid reselection:** choosing photo B while photo A is still processing never shows
      photo A's preview or face analysis against photo B (stale results are discarded)
- [ ] Rapidly changing crop mode / target / background never leaves a "failed" error
- [ ] No crash when toggling options faster than the preview can render

## 4. Face-aware crop (on-device ML Kit)

- [ ] Portrait selfie: crop centers on the face
- [ ] Group photo: crop covers all detected faces (Safe Fit) or reports them
- [ ] No-face landscape: shows the "center framing was used" notice, no crash
- [ ] Face near top edge: Safe Fit keeps the face in frame
- [ ] Partially cropped face / sunglasses / profile face: no crash, sensible framing
- [ ] Rotated (EXIF 90°/270°) photo: faces detected on the upright image
- [ ] Mirrored selfie: orientation handled, no sideways result
- [ ] Face-aware toggle OFF then ON re-runs detection on the current photo
- [ ] Detection failure on a corrupt image: falls back to center framing with a notice

## 5. Crop modes & background

- [ ] Safe Fit ("Full photo · adds padding") preserves the whole photo with padding
- [ ] Balanced covers most of the screen while keeping the subject
- [ ] Fill covers the entire canvas (edges may be cropped)
- [ ] Blur / Gradient / Solid backgrounds each render and apply correctly
- [ ] Final wallpaper is opaque — no transparent or black-blended edges
- [ ] Changing background mode updates the preview

## 6. Focus / tap interaction

- [ ] Tapping the preview relocates the focus crosshair to the tapped subject
- [ ] Crosshair aligns with the actual subject on the preview (no offset for HOME target)
- [ ] Tapping outside the image region is clamped safely
- [ ] Reset (top-right icon) returns to defaults

## 7. Targets: Home / Lock / Both

- [ ] Home: preview reflects the wider home canvas
- [ ] Lock: preview uses screen-width canvas
- [ ] Both: "Showing home screen" / "Showing lock screen" toggle switches the preview
- [ ] Launcher disclaimer ("Your launcher may crop or zoom wallpapers differently…")
      is visible under the preview

## 8. Apply wallpaper

- [ ] Apply (Home) sets the home screen wallpaper
- [ ] Apply (Lock) sets the lock screen wallpaper (on devices that support it)
- [ ] Apply (Both) sets both; success message says so
- [ ] Applying twice rapidly does not double-fire or crash
- [ ] Device-policy-restricted profile shows the policy error message
- [ ] Device without wallpaper support shows the unsupported message

## 9. Save / export

- [ ] Save (API 29+) writes to `Pictures/WallpaperCropFixer` and reports exactly that
- [ ] Save (API 26–28) writes to the app's folder and reports the accurate location
      (it must NOT claim the gallery path)
- [ ] Saved file opens correctly in the gallery / Files app
- [ ] Export quality slider (Settings) is honored
- [ ] No available storage: friendly error, no silent failure

## 10. Failure handling

- [ ] Corrupt/truncated image: "Couldn't read that photo" — no crash
- [ ] Unsupported image type: same friendly error
- [ ] Unreadable URI (permission revoked): friendly error
- [ ] Processing failure: "Couldn't generate the preview" — no internal exception text
- [ ] Cancel/reselect during processing never shows "Job was cancelled" to the user

## 11. Process recreation / lifecycle

- [ ] Rotating the device keeps the edited image and re-frames for the new orientation
- [ ] Backgrounding the app during face detection and returning does not crash
- [ ] App killed and reopened restores the editor image and in-session options
- [ ] Photo cache does not grow unboundedly across many picks

## 12. Theme / accessibility

- [ ] UI is legible at large font scaling (text not clipped, buttons usable)
- [ ] TalkBack: every button/switch/chip has a meaningful label; preview has a description
- [ ] Icon-only buttons have content descriptions; decorative icons are hidden
- [ ] Focus/crosshair interaction is reachable with accessibility tools
- [ ] Touch targets meet the 48dp minimum

## 13. Version / OS coverage

- [ ] API 26/28: picker works without permissions; export reports the app-folder path
- [ ] API 35/36 (current Android): picker, preview, apply, save all work
- [ ] 16 KB page-size compatibility verified on the release artifact (see release notes)
- [ ] Foldable / multi-window: canvas uses the full display size, not the split window
- [ ] Different aspect ratios (tall phones, tablets) render a sensible preview

## 14. OEM wallpaper behavior (physical devices)

- [ ] Samsung One UI: home/lock wallpaper applied correctly; note launcher cropping
- [ ] Google Pixel: wallpaper applied; parallax/scroll behavior matches expectations
- [ ] Motorola / OnePlus / Xiaomi: applied correctly, note any launcher cropping
- [ ] Preview disclaimer wording matches observed launcher behavior

## 15. Privacy

- [ ] No `READ_EXTERNAL_STORAGE` / `READ_MEDIA_IMAGES` in the installed app
- [ ] Settings → "Privacy policy" opens the public policy page (no login wall)
- [ ] Data Safety answers match verified runtime behavior (capture traffic if possible)

## 16. Release build

- [ ] `bundleRelease` produces an AAB
- [ ] `lintRelease` reports 0 errors
- [ ] Unit/Robolectric test suite passes (including concurrency + Compose tests)
- [ ] Release AAB installed via bundletool or Play Internal Testing runs the core journey

---

## Go / No-Go gate

Ship when every checkbox above is checked on at least one modern device and one
API 26–28 device. The governing sentence:

> "If the preview, the applied wallpaper, and the saved file ever disagree about
> what the user configured, we are not ready."

Physical-device items that cannot be run in your environment must be explicitly
marked `BLOCKED — PHYSICAL DEVICE QA REQUIRED`, not assumed passing.