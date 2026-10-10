package com.wallpapercropfixer.domain.model

/**
 * The immutable, resolved plan for one render: where the source is cropped, where
 * the cropped image lands on the target canvas, whether padding (letterbox) is
 * used, and the honesty-guide geometry derived from it.
 *
 * The guide fields are advisory only; they never change the rendered pixels.
 */
data class WallpaperRenderPlan(
    val sourceCropRect: CropRect,
    val targetCanvasSpec: TargetCanvasSpec,
    val outputImagePlacement: CropRect,
    val usePadding: Boolean,
    val backgroundFillMode: BackgroundFillMode,
    val finalFocusPoint: FocusPoint?,
    /** Region of the canvas visible at rest inside a wider parallax canvas (HOME). */
    val visibleWindow: CropRect? = null,
    /** Lock-screen clock + inset region (LOCK target only). */
    val lockClockSafeArea: CropRect? = null,
    /** True when an analyzed subject overlaps the lock clock region. */
    val subjectInClockZone: Boolean = false,
    /** True when an analyzed subject sits in the scroll-off parallax margin. */
    val subjectExposedToParallax: Boolean = false
)
