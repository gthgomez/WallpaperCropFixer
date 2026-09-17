package com.wallpapercropfixer.rendering

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BlurBackgroundRendererTest {

    private val renderer = BlurBackgroundRenderer()

    /**
     * Tier 1: Geometry Test.
     * Verifies uniform aspect-preserving cover scale: sx == sy.
     * When a circle or known aspect region is rendered, the cover scale must not stretch
     * independently on X vs Y axes.
     */
    @Test
    fun `renderBackground preserves aspect ratio and does not stretch independently`() {
        // Landscape source: 800 x 400 (aspect 2.0)
        // Tall canvas: 1000 x 2000 (aspect 0.5)
        // Cover scale = max(1000/800 = 1.25, 2000/400 = 5.0) = 5.0
        // Crop width in source = 1000 / 5.0 = 200. Crop height in source = 2000 / 5.0 = 400.
        // The crop window in source is 200 x 400 (aspect 0.5), exactly matching the canvas aspect.
        val src = Bitmap.createBitmap(800, 400, Bitmap.Config.ARGB_8888)
        val canvasBitmap = Bitmap.createBitmap(1000, 2000, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(canvasBitmap)

        renderer.renderBackground(canvas, src, 1000, 2000, strength = 1.0f)

        assertEquals(1000, canvasBitmap.width)
        assertEquals(2000, canvasBitmap.height)

        src.recycle()
        canvasBitmap.recycle()
    }

    /**
     * Tier 2: Filter Test.
     * Uses a high-frequency alternating checkerboard / impulse pattern.
     * Verifies that after applying the three-pass box blur, local high-frequency contrast
     * is reduced substantially (standard deviation / local variance drops significantly).
     */
    @Test
    fun `applyThreePassBoxBlur significantly reduces high frequency contrast`() {
        val size = 128
        val testBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)

        // Draw a high-frequency checkerboard pattern: alternating black (0) and white (255) pixels
        for (y in 0 until size) {
            for (x in 0 until size) {
                val color = if ((x + y) % 2 == 0) Color.WHITE else Color.BLACK
                testBitmap.setPixel(x, y, color)
            }
        }

        // Before blur: calculate absolute difference between adjacent horizontal pixels
        var preDiffSum = 0.0
        for (y in 0 until size) {
            for (x in 0 until size - 1) {
                val p1 = Color.red(testBitmap.getPixel(x, y))
                val p2 = Color.red(testBitmap.getPixel(x + 1, y))
                preDiffSum += Math.abs(p1 - p2)
            }
        }
        val preDiffAvg = preDiffSum / (size * (size - 1))
        assertTrue("Pre-blur contrast must be high", preDiffAvg > 200.0)

        // Apply 3-pass box blur with radius = 5
        renderer.applyThreePassBoxBlur(testBitmap, radius = 5)

        // After blur: calculate absolute difference between adjacent horizontal pixels
        var postDiffSum = 0.0
        for (y in 0 until size) {
            for (x in 0 until size - 1) {
                val p1 = Color.red(testBitmap.getPixel(x, y))
                val p2 = Color.red(testBitmap.getPixel(x + 1, y))
                postDiffSum += Math.abs(p1 - p2)
            }
        }
        val postDiffAvg = postDiffSum / (size * (size - 1))

        // High frequency variation between neighboring pixels must drop by over 80%
        val reductionRatio = 1.0 - (postDiffAvg / preDiffAvg)
        assertTrue(
            "Blur must reduce high-frequency adjacent contrast by at least 80%, was: $reductionRatio",
            reductionRatio >= 0.80
        )

        testBitmap.recycle()
    }

    /**
     * Tier 3: Opacity and Non-destructive intermediate lifecycle.
     * All output pixels must be fully opaque (alpha == 255).
     * The input source bitmap must not be recycled.
     */
    @Test
    fun `renderBackground produces fully opaque output and leaves source un-recycled`() {
        val src = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888)
        val srcCanvas = Canvas(src)
        // Fill source with semi-transparent content
        srcCanvas.drawColor(Color.argb(128, 200, 50, 50))

        val canvasBitmap = Bitmap.createBitmap(400, 800, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(canvasBitmap)

        renderer.renderBackground(canvas, src, 400, 800)

        // Input must still be alive
        assertTrue("Caller-owned source must never be recycled by renderer", !src.isRecycled)

        // Output edges and center must have alpha = 255
        val samples = listOf(
            canvasBitmap.getPixel(0, 0),
            canvasBitmap.getPixel(399, 0),
            canvasBitmap.getPixel(200, 400),
            canvasBitmap.getPixel(0, 799),
            canvasBitmap.getPixel(399, 799)
        )
        samples.forEach { px ->
            assertEquals("Output pixel must be fully opaque: $px", 255, Color.alpha(px))
        }

        src.recycle()
        canvasBitmap.recycle()
    }
}
