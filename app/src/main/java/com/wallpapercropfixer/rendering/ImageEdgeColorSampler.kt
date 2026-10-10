package com.wallpapercropfixer.rendering

import android.graphics.Bitmap
import android.graphics.Color

/**
 * Deterministic photo-derived hue shared by the Color and Gradient backgrounds.
 * Uses the dominant palette color (frequency-weighted quantization) so the finish
 * reflects the whole photo rather than only its corners.
 */
internal object ImageEdgeColorSampler {
    fun sample(source: Bitmap): Int {
        if (source.width <= 0 || source.height <= 0) return FALLBACK
        return extractFrom(source, maxColors = 1).firstOrNull() ?: FALLBACK
    }

    /** Warm neutral used only when the source has no opaque pixels. */
    private val FALLBACK = Color.rgb(74, 68, 64)
}
