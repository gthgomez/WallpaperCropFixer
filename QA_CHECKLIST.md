# WallpaperCropFixer — Pre-Launch QA Checklist

Source: ExactUploadFixer QA format adapted for wallpaper crop workflow.
Run every item on a real device before hitting Publish.

---

## 1. EXIF Orientation

- [ ] **Rotated image (EXIF 90 degrees):** Final orientation is upright, no sideways output
- [ ] **Rotated image (EXIF 270 degrees):** Final orientation is upright
- [ ] **Flipped image (EXIF horizontal):** Orientation preserved correctly in output
- [ ] **No EXIF image:** Extracted normally, assumed 0 degrees
- [ ] **Orientation preserved in saved output:** Saved wallpaper has embedded correct orientation
- [ ] **EXIF read before crop math:** Bounds are swapped correctly for rotated images

**Pass criteria: 6/6 before publishing.**

---

## 2. Face Detection (ML Kit)

- [ ] **Single face:** Crop rectangle centers correctly on the face
- [ ] **Multiple faces:** Crop rectangle covers all detected faces
- [ ] **No faces detected:** Falls back to center crop, no crash
- [ ] **Partial face at edge:** Crop rectangle extends to include partial face
- [ ] **Face detection loading:** Progress indicator shown during ML Kit initialization
- [ ] **Face detection error:** Graceful fallback to center crop, clear error message

**Pass criteria: 6/6 before publishing.**

---

## 3. Crop Math

- [ ] **Aspect ratio correct:** Output matches selected aspect ratio exactly (no distortion)
- [ ] **No distortion:** Image is not stretched, squished, or warped
- [ ] **Crop handles work:** Dragging crop handles snaps/updates preview in real time
- [ ] **Crop rectangle constraints:** Cannot drag crop outside image bounds
- [ ] **Aspect ratio switch:** Changing aspect ratio recalculates crop correctly
- [ ] **Landscape vs. portrait:** Both orientations work correctly

**Pass criteria: 6/6 before publishing.**

---

## 4. Export Quality

- [ ] **Output resolution:** Resolution matches device wallpaper requirements
- [ ] **Output format:** JPEG or PNG as expected, no quality loss beyond original
- [ ] **Wallpaper set directly:** Set as wallpaper action works (if implemented)
- [ ] **Save to device:** File saved with correct dimensions and format
- [ ] **Large photo (12+ MP):** Processes without OOM or ANR

**Pass criteria: 5/5 before publishing.**

---

## 5. Permissions & Privacy

- [ ] **Manifest audit:** `READ_MEDIA_IMAGES` **not** declared; `READ_EXTERNAL_STORAGE` has `maxSdkVersion="32"` only
- [ ] **Android 13+ device:** Photo Picker opens with **no** permission dialog
- [ ] **Android 14 device:** Photo Picker works, no permission dialog
- [ ] **Android 15 device:** Photo Picker works, no permission dialog
- [ ] **API 32 or below:** Legacy picker may prompt for `READ_EXTERNAL_STORAGE`; deny → clear error, grant → picker works
- [ ] **Export path:** MediaStore write to `Pictures/WallpaperCropFixer` on API 29+ (no storage permission)
- [ ] **Set wallpaper:** `SET_WALLPAPER` applies cropped bitmap via `WallpaperManager` (on-device only)
- [ ] **No `WRITE_EXTERNAL_STORAGE`:** Not declared in manifest
- [ ] **Settings privacy link:** Opens placeholder `https://example.invalid/wallpaper-crop-fixer-privacy`

**Pass criteria: 9/9 before publishing.**

---

## 6. UI and Free Flow

- [ ] **Pick photo → Crop screen:** Photo loads with correct aspect ratio
- [ ] **Crop preview is accurate:** What you see in preview matches output exactly
- [ ] **Drag crop handles:** Smooth, responsive, follows touch
- [ ] **Aspect ratio selector:** Tapping preset correctly recalculates
- [ ] **Reset crop:** Returns to initial crop state
- [ ] **Set wallpaper / Save action:** Works correctly
- [ ] **Dark mode:** UI renders correctly
- [ ] **Landscape orientation:** No layout overflow or crash

**Pass criteria: 8/8 before publishing.**

---

## 7. Go / No-Go Gate

**Ship when all boxes are checked.**

If face detection fails on a common photo, the app must fall back to center crop — not crash or produce a blank wallpaper.

The one sentence that governs every decision:
> "If this does not directly help a user set the perfect wallpaper right now, cut it."
