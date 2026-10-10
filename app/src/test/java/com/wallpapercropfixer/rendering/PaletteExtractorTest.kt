package com.wallpapercropfixer.rendering

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaletteExtractorTest {

    // Colors chosen to be exactly representable in the 5-bit-per-channel quantization, so
    // extraction reproduces them byte-for-byte instead of a nearby rounded value.
    private val red = 0xFFFF0000.toInt()
    private val green = 0xFF00FF00.toInt()
    private val blue = 0xFF0000FF.toInt()
    private val teal = 0xFF21A5C6.toInt()
    private val white = 0xFFFFFFFF.toInt()
    private val transparentMagenta = 0x00FF00FF.toInt()

    @Test
    fun `solid color image returns that color first`() {
        val width = 40
        val height = 25
        val pixels = IntArray(width * height) { teal }

        val palette = PaletteExtractor.extract(pixels, width, height)

        assertEquals(1, palette.size)
        assertEquals(teal, palette.first())
    }

    @Test
    fun `dominant color is returned first`() {
        // 80% red, 20% blue.
        val width = 10
        val height = 10
        val pixels = IntArray(width * height) { if (it < 80) red else blue }

        val palette = PaletteExtractor.extract(pixels, width, height)

        assertEquals(2, palette.size)
        assertEquals(red, palette[0])
        assertEquals(blue, palette[1])
    }

    @Test
    fun `fully transparent pixels are ignored`() {
        val pixels = intArrayOf(
            transparentMagenta, // alpha == 0 → skipped even though magenta is rare
            0x00000000, // fully transparent black → skipped
            red,
            red,
            red,
            0x80FFFFFF.toInt() // alpha != 0 → still counted
        )

        val palette = PaletteExtractor.extract(pixels, 3, 2)

        assertEquals(red, palette.first())
        assertTrue("transparent color must never appear: $palette", palette.none { it == transparentMagenta })
        assertEquals(listOf(red, white), palette)
    }

    @Test
    fun `empty zero size and fully transparent inputs return empty list`() {
        assertTrue(PaletteExtractor.extract(IntArray(0), 0, 0).isEmpty())
        assertTrue(PaletteExtractor.extract(IntArray(0), 10, 10).isEmpty())
        assertTrue(PaletteExtractor.extract(IntArray(100) { red }, 0, 10).isEmpty())
        assertTrue(PaletteExtractor.extract(IntArray(100) { red }, 10, 0).isEmpty())
        // No opaque pixels at all.
        assertTrue(PaletteExtractor.extract(IntArray(16), 4, 4).isEmpty())
    }

    @Test
    fun `maxColors limits the palette size`() {
        // Five distinct buckets with strictly decreasing counts: red > green > blue > teal > white.
        val pixels = buildList {
            repeat(5) { add(red) }
            repeat(4) { add(green) }
            repeat(3) { add(blue) }
            repeat(2) { add(teal) }
            repeat(1) { add(white) }
        }.toIntArray()
        val width = pixels.size

        val capped = PaletteExtractor.extract(pixels, width, 1, maxColors = 2)
        assertEquals(2, capped.size)
        assertEquals(listOf(red, green), capped)

        // Default cap is 4 → four most frequent buckets.
        assertEquals(4, PaletteExtractor.extract(pixels, width, 1).size)

        // A larger cap returns every distinct bucket.
        assertEquals(5, PaletteExtractor.extract(pixels, width, 1, maxColors = 8).size)
    }

    @Test
    fun `extraction is deterministic for the same input`() {
        val width = 64
        val height = 64
        // Fixed index math — no randomness, no clock.
        val pixels = IntArray(width * height) { index ->
            when (index % 3) {
                0 -> red
                1 -> blue
                else -> teal
            }
        }

        val first = PaletteExtractor.extract(pixels, width, height, maxColors = 3)
        val second = PaletteExtractor.extract(pixels, width, height, maxColors = 3)

        assertEquals(first, second)
        assertEquals(3, first.size)
    }
}
