package com.wallpapercropfixer.core.math

import com.wallpapercropfixer.domain.model.CropRect
import com.wallpapercropfixer.domain.model.FaceBounds

/**
 * Pure geometry for two honesty guides the preview surfaces:
 *  - the panel-visible window inside a wider parallax canvas,
 *  - the lock-screen clock + inset safe area.
 *
 * No Android dependencies; fully unit-testable on the JVM.
 */
object WallpaperSafeAreaMath {

    /** Fraction of canvas height occupied by the lock clock band, below the top margin. */
    const val LOCK_CLOCK_HEIGHT_FRACTION = 0.18f

    /** Top margin above the clock band. */
    const val LOCK_CLOCK_TOP_FRACTION = 0.05f

    /** Horizontal inset so the clock band never spans edge to edge. */
    const val LOCK_CLOCK_SIDE_INSET_FRACTION = 0.08f

    /**
     * Region of a canvas that stays on screen at rest when the canvas is wider or
     * taller than the display (parallax). Centered, full available size. Returns the
     * whole canvas when it already fits the screen.
     */
    fun visibleWindow(
        canvasWidth: Int,
        canvasHeight: Int,
        screenWidth: Int,
        screenHeight: Int
    ): CropRect {
        if (canvasWidth <= 0 || canvasHeight <= 0) return CropRect(0f, 0f, 0f, 0f)
        val visibleWidth = if (screenWidth in 1 until canvasWidth) screenWidth else canvasWidth
        val visibleHeight = if (screenHeight in 1 until canvasHeight) screenHeight else canvasHeight
        val left = (canvasWidth - visibleWidth) / 2f
        val top = (canvasHeight - visibleHeight) / 2f
        return CropRect(left, top, left + visibleWidth, top + visibleHeight)
    }

    /** Conservative lock-screen clock + inset region, in canvas pixels. */
    fun lockClockSafeArea(canvasWidth: Int, canvasHeight: Int): CropRect {
        if (canvasWidth <= 0 || canvasHeight <= 0) return CropRect(0f, 0f, 0f, 0f)
        val sideInset = canvasWidth * LOCK_CLOCK_SIDE_INSET_FRACTION
        val top = canvasHeight * LOCK_CLOCK_TOP_FRACTION
        val bottom = (top + canvasHeight * LOCK_CLOCK_HEIGHT_FRACTION)
            .coerceAtMost(canvasHeight.toFloat())
        return CropRect(sideInset, top, canvasWidth - sideInset, bottom)
    }

    fun intersects(a: CropRect, b: CropRect): Boolean =
        a.left < b.right && b.left < a.right && a.top < b.bottom && b.top < a.bottom

    fun intersects(bounds: FaceBounds, area: CropRect): Boolean =
        bounds.left < area.right && area.left < bounds.right &&
            bounds.top < area.bottom && area.top < bounds.bottom

    /** True when any of [bounds] intersects [area]. */
    fun anyIntersects(bounds: List<FaceBounds>, area: CropRect): Boolean =
        bounds.any { intersects(it, area) }

    /**
     * True when [bounds] lies partly in the scroll-off margin of [visibleWindow]
     * inside [canvas] — the region a launcher parallax can scroll out of view.
     * Returns false when the canvas has no horizontal margin.
     */
    fun exposedToParallax(bounds: FaceBounds, canvas: CropRect, visibleWindow: CropRect): Boolean {
        val hasHorizontalMargin = visibleWindow.left > canvas.left + 0.5f ||
            visibleWindow.right < canvas.right - 0.5f
        if (!hasHorizontalMargin) return false
        if (!intersects(bounds, canvas)) return false
        val fullyVisible = bounds.left >= visibleWindow.left &&
            bounds.right <= visibleWindow.right &&
            bounds.top >= visibleWindow.top &&
            bounds.bottom <= visibleWindow.bottom
        return !fullyVisible
    }
}
