package com.wallpapercropfixer.core.math

import com.wallpapercropfixer.domain.model.CropRect
import com.wallpapercropfixer.domain.model.FocusPoint
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Property-style fuzz tests over the crop invariants the release depends on:
 * crop rects must always be in-bounds and have positive dimensions.
 */
class CropMathPropertyTest {

    @Test
    fun `computeFocusBiasedCropRect invariants hold across random inputs`() {
        val random = Random(12345)
        val targetRatios = listOf(9f / 16f, 16f / 9f, 1f, 0.42f, 2.4f, 19.5f / 9f)
        repeat(1000) {
            val sw = random.nextInt(50, 30000)
            val sh = random.nextInt(50, 30000)
            val targetRatio = targetRatios[random.nextInt(targetRatios.size)]
            val fx = random.nextFloat()
            val fy = random.nextFloat()

            val rect = CropMath.computeFocusBiasedCropRect(sw, sh, targetRatio, FocusPoint(fx, fy))

            assertTrue("left>=0 (sw=$sw sh=$sh r=$targetRatio f=($fx,$fy))", rect.left >= 0f)
            assertTrue("top>=0 (sw=$sw sh=$sh r=$targetRatio f=($fx,$fy))", rect.top >= 0f)
            assertTrue("right<=sw (sw=$sw sh=$sh r=$targetRatio f=($fx,$fy))", rect.right <= sw + 0.001f)
            assertTrue("bottom<=sh (sw=$sw sh=$sh r=$targetRatio f=($fx,$fy))", rect.bottom <= sh + 0.001f)
            assertTrue("width>0 (sw=$sw sh=$sh r=$targetRatio f=($fx,$fy))", rect.width > 0f)
            assertTrue("height>0 (sw=$sw sh=$sh r=$targetRatio f=($fx,$fy))", rect.height > 0f)
        }
    }

    @Test
    fun `computePaddedPlacementRect invariants hold across random inputs`() {
        val random = Random(98765)
        repeat(1000) {
            val cw = random.nextInt(100, 4000)
            val ch = random.nextInt(100, 8000)
            val cropRect = CropRect(
                left = random.nextFloat() * 100f,
                top = random.nextFloat() * 100f,
                right = 500f + random.nextFloat() * 2000f,
                bottom = 500f + random.nextFloat() * 4000f
            )
            val focus = FocusPoint(random.nextFloat(), random.nextFloat())

            val rect = CropMath.computePaddedPlacementRect(cw, ch, cropRect, focus)

            assertTrue("left>=0", rect.left >= 0f)
            assertTrue("top>=0", rect.top >= 0f)
            assertTrue("right<=cw", rect.right <= cw + 0.001f)
            assertTrue("bottom<=ch", rect.bottom <= ch + 0.001f)
            assertTrue("width>0", rect.width > 0f)
            assertTrue("height>0", rect.height > 0f)
            // placement must not exceed the canvas
            assertTrue("placement within canvas", rect.right - rect.left <= cw + 0.001f)
            assertTrue("placement within canvas h", rect.bottom - rect.top <= ch + 0.001f)
        }
    }

    @Test
    fun `focus coordinate transforms round-trip across random values`() {
        val random = Random(24680)
        repeat(500) {
            val sw = random.nextInt(100, 20000)
            val sh = random.nextInt(100, 20000)
            val cw = random.nextInt(100, 4000)
            val ch = random.nextInt(100, 8000)
            val cropRect = CropRect(
                left = random.nextFloat() * sw * 0.3f,
                top = random.nextFloat() * sh * 0.3f,
                right = sw * 0.7f + random.nextFloat() * sw * 0.3f,
                bottom = sh * 0.7f + random.nextFloat() * sh * 0.3f
            )
            val placement = CropRect(
                left = random.nextFloat() * cw * 0.3f,
                top = random.nextFloat() * ch * 0.3f,
                right = cw * 0.7f + random.nextFloat() * cw * 0.3f,
                bottom = ch * 0.7f + random.nextFloat() * ch * 0.3f
            )
            val sourceFocus = FocusPoint(random.nextFloat(), random.nextFloat())

            val canvasFocus = CropMath.sourceFocusToCanvasFocus(
                sourceFocus, sw, sh, cropRect, placement, cw, ch
            )
            val roundTrip = CropMath.canvasFocusToSourceFocus(
                canvasFocus, sw, sh, cropRect, placement, cw, ch
            )
            assertTrue("x in range", canvasFocus.xNormalized in 0f..1f)
            assertTrue("y in range", canvasFocus.yNormalized in 0f..1f)
            // Points that fall inside the crop should round-trip (clamped edges may differ).
            val srcX = sourceFocus.xNormalized * sw
            val srcY = sourceFocus.yNormalized * sh
            if (srcX >= cropRect.left && srcX <= cropRect.right &&
                srcY >= cropRect.top && srcY <= cropRect.bottom
            ) {
                assertTrue("x round trip", kotlin.math.abs(roundTrip.xNormalized - sourceFocus.xNormalized) < 0.02f)
                assertTrue("y round trip", kotlin.math.abs(roundTrip.yNormalized - sourceFocus.yNormalized) < 0.02f)
            }
        }
    }
}