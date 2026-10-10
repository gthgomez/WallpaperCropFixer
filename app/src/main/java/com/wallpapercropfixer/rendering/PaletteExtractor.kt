package com.wallpapercropfixer.rendering

import android.graphics.Bitmap
import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * Deterministic dominant-color extraction for background rendering.
 *
 * The core algorithm [extract] is pure Kotlin — it touches no `android.*` type, so it runs on
 * the JVM without Robolectric. Only the convenience bridge [extractFrom] reads a [Bitmap].
 */
internal object PaletteExtractor {

    /** Bits kept per channel when bucketing colors: 5 bits → 32 levels per channel. */
    private const val BITS_PER_CHANNEL = 5

    /** Upper bound on sampled pixels; larger images are subsampled on a fixed stride grid. */
    private const val MAX_SAMPLES = 10_000

    private const val CHANNEL_SHIFT = 8 - BITS_PER_CHANNEL

    /**
     * Returns up to [maxColors] dominant ARGB colors of [pixels], most frequent first.
     *
     * Colors are quantized to 5 bits per channel and fully transparent pixels (alpha == 0) are
     * ignored. Large inputs are subsampled on a grid so work stays bounded. Returns an empty
     * list for empty/zero-sized input or when no opaque pixel exists.
     */
    fun extract(pixels: IntArray, width: Int, height: Int, maxColors: Int = 4): List<Int> {
        if (maxColors <= 0 || width <= 0 || height <= 0 || pixels.isEmpty()) return emptyList()

        val totalPixels = width.toLong() * height.toLong()
        val step = gridStep(totalPixels)

        val counts = HashMap<Int, Int>()
        var y = 0
        while (y < height) {
            val rowStart = y.toLong() * width.toLong()
            var x = 0
            while (x < width) {
                val index = rowStart + x
                if (index >= pixels.size) break // Defensive: never read past the buffer.
                val color = pixels[index.toInt()]
                if ((color ushr 24) != 0) { // Skip fully transparent (alpha == 0).
                    val key = bucketKey(color)
                    counts[key] = (counts[key] ?: 0) + 1
                }
                x += step
            }
            y += step
        }

        if (counts.isEmpty()) return emptyList()

        return counts.entries
            .sortedWith(compareByDescending<Map.Entry<Int, Int>> { it.value }.thenBy { it.key })
            .take(maxColors)
            .map { representativeArgb(it.key) }
    }

    /** Sampling stride so at most [MAX_SAMPLES] pixels are visited. */
    private fun gridStep(totalPixels: Long): Int {
        if (totalPixels <= MAX_SAMPLES) return 1
        val perAxis = ceil(sqrt(totalPixels.toDouble() / MAX_SAMPLES)).toInt()
        return perAxis.coerceAtLeast(1)
    }

    /** Packs a color into a 15-bit bucket key holding 5 bits each for R, G and B. */
    private fun bucketKey(color: Int): Int {
        val r = ((color shr 16) and 0xFF) ushr CHANNEL_SHIFT
        val g = ((color shr 8) and 0xFF) ushr CHANNEL_SHIFT
        val b = (color and 0xFF) ushr CHANNEL_SHIFT
        return (r shl (2 * BITS_PER_CHANNEL)) or (g shl BITS_PER_CHANNEL) or b
    }

    /** Expands a bucket key back into an opaque ARGB color, replicating the high bits. */
    private fun representativeArgb(key: Int): Int {
        val r = expand((key shr (2 * BITS_PER_CHANNEL)) and 0x1F)
        val g = expand((key shr BITS_PER_CHANNEL) and 0x1F)
        val b = expand(key and 0x1F)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    /** Maps a 5-bit channel value to 8 bits (e.g. 31 → 255) for a smoother representative. */
    private fun expand(value5: Int): Int =
        (value5 shl CHANNEL_SHIFT) or (value5 shr (BITS_PER_CHANNEL - CHANNEL_SHIFT))
}

/**
 * Reads [source]'s ARGB_8888 pixels row-major and delegates to [PaletteExtractor.extract].
 *
 * Android-only convenience bridge; the extraction core stays JVM-testable.
 */
internal fun extractFrom(source: Bitmap, maxColors: Int = 4): List<Int> {
    val width = source.width
    val height = source.height
    if (width <= 0 || height <= 0) return emptyList()
    val pixels = IntArray(width * height)
    source.getPixels(pixels, 0, width, 0, 0, width, height)
    return PaletteExtractor.extract(pixels, width, height, maxColors)
}
