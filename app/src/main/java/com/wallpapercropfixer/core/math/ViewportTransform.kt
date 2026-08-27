package com.wallpapercropfixer.core.math

/**
 * Models the transform between a rendered wallpaper bitmap and the visible,
 * center-cropped viewport that displays it (Compose `ContentScale.Crop`).
 *
 * When the bitmap aspect ratio differs from the viewport aspect ratio, only a
 * centered window of the bitmap is visible. Both directions of the mapping are
 * required so that the focus overlay aligns with the subject and taps map back
 * to the correct source position. All coordinates are normalized 0..1.
 */
object ViewportTransform {

    data class Window(val start: Float, val end: Float) {
        val span: Float get() = (end - start).coerceAtLeast(0f)
    }

    data class Point(val x: Float, val y: Float)

    /**
     * Visible window (in the horizontally-clipped axis) of a bitmap of aspect
     * [wider] displayed in a viewport of aspect [narrower]. Both must be > 0.
     */
    fun visibleWindow(widerAspect: Float, narrowerAspect: Float): Window {
        require(widerAspect > 0f && narrowerAspect > 0f) { "Aspects must be positive" }
        val fraction = (narrowerAspect / widerAspect).coerceAtMost(1f)
        val start = (1f - fraction) / 2f
        return Window(start, start + fraction)
    }

    /**
     * Maps a point in bitmap normalized space into viewport normalized space.
     * The viewport shows the center of the bitmap when aspects differ.
     */
    fun bitmapToViewport(x: Float, y: Float, bitmapAspect: Float, viewportAspect: Float): Point {
        require(bitmapAspect > 0f && viewportAspect > 0f) { "Aspects must be positive" }
        return when {
            bitmapAspect > viewportAspect -> {
                val w = visibleWindow(bitmapAspect, viewportAspect)
                Point(((x - w.start) / w.span).coerceIn(0f, 1f), y.coerceIn(0f, 1f))
            }
            bitmapAspect < viewportAspect -> {
                val w = visibleWindow(viewportAspect, bitmapAspect)
                Point(x.coerceIn(0f, 1f), ((y - w.start) / w.span).coerceIn(0f, 1f))
            }
            else -> Point(x.coerceIn(0f, 1f), y.coerceIn(0f, 1f))
        }
    }

    /**
     * Maps a point in viewport normalized space back into bitmap normalized space.
     * Inverse of [bitmapToViewport].
     */
    fun viewportToBitmap(x: Float, y: Float, bitmapAspect: Float, viewportAspect: Float): Point {
        require(bitmapAspect > 0f && viewportAspect > 0f) { "Aspects must be positive" }
        val cx = x.coerceIn(0f, 1f)
        val cy = y.coerceIn(0f, 1f)
        return when {
            bitmapAspect > viewportAspect -> {
                val w = visibleWindow(bitmapAspect, viewportAspect)
                Point((w.start + cx * w.span).coerceIn(0f, 1f), cy)
            }
            bitmapAspect < viewportAspect -> {
                val w = visibleWindow(viewportAspect, bitmapAspect)
                Point(cx, (w.start + cy * w.span).coerceIn(0f, 1f))
            }
            else -> Point(cx, cy)
        }
    }
}