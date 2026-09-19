package com.wallpapercropfixer.rendering

import android.graphics.Bitmap
import android.graphics.Color

/** Deterministic photo-edge hue shared by Color and Gradient backgrounds. */
internal object ImageEdgeColorSampler {
    fun sample(source: Bitmap): Int {
        val insetX = source.width / 8
        val insetY = source.height / 8
        val pixels = intArrayOf(
            source.getPixel(insetX, insetY),
            source.getPixel(source.width - 1 - insetX, insetY),
            source.getPixel(insetX, source.height - 1 - insetY),
            source.getPixel(source.width - 1 - insetX, source.height - 1 - insetY)
        )
        // Composite transparent samples over black, just like the opaque wallpaper.
        fun average(channel: (Int) -> Int): Int =
            pixels.sumOf { channel(it) * Color.alpha(it) / 255 } / pixels.size
        return Color.rgb(average(Color::red), average(Color::green), average(Color::blue))
    }
}
