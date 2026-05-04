package com.wallpapercropfixer.domain.engine

import com.wallpapercropfixer.core.math.CropMath
import com.wallpapercropfixer.domain.model.FocusPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CropMathTest {

    private val tolerance = 0.01f

    // ── computeFocusBiasedCropRect ─────────────────────────────────────────────

    @Test
    fun `center focus on square source produces centered crop`() {
        val rect = CropMath.computeFocusBiasedCropRect(
            sourceWidth = 1000,
            sourceHeight = 1000,
            targetRatio = 9f / 16f,
            focusPoint = CropMath.CENTER_FOCUS
        )
        // Source is wider than the portrait target, so we crop left/right.
        assertEquals(1000f * (9f / 16f), rect.width, tolerance)
        assertEquals(1000f, rect.height, tolerance)
        // Centered horizontally
        val expectedLeft = (1000f - rect.width) / 2f
        assertEquals(expectedLeft, rect.left, tolerance)
    }

    @Test
    fun `wide source with center focus crops left and right equally`() {
        val rect = CropMath.computeFocusBiasedCropRect(
            sourceWidth = 4000,
            sourceHeight = 1000,
            targetRatio = 1f,
            focusPoint = CropMath.CENTER_FOCUS
        )
        // Source is wider → crop left/right; cropH = sourceHeight, cropW = cropH * 1.0
        assertEquals(1000f, rect.width, tolerance)
        assertEquals(1000f, rect.height, tolerance)
        // Center crop: left = (4000 - 1000) / 2 = 1500
        assertEquals(1500f, rect.left, tolerance)
    }

    @Test
    fun `focus bias shifts crop window toward focus point`() {
        // Focus on far right (x = 0.9)
        val rect = CropMath.computeFocusBiasedCropRect(
            sourceWidth = 4000,
            sourceHeight = 1000,
            targetRatio = 1f,
            focusPoint = FocusPoint(xNormalized = 0.9f, yNormalized = 0.5f)
        )
        // Focus pixel x = 0.9 * 4000 = 3600; crop is 1000 wide
        // Ideal left = 3600 - 500 = 3100, clamped to 4000 - 1000 = 3000
        assertEquals(3000f, rect.left, tolerance)
        assertEquals(4000f, rect.right, tolerance)
    }

    @Test
    fun `crop rect is always within source bounds`() {
        val rect = CropMath.computeFocusBiasedCropRect(
            sourceWidth = 500,
            sourceHeight = 500,
            targetRatio = 16f / 9f,
            focusPoint = FocusPoint(xNormalized = 0.01f, yNormalized = 0.99f)
        )
        assertTrue(rect.left >= 0f)
        assertTrue(rect.top >= 0f)
        assertTrue(rect.right <= 500f)
        assertTrue(rect.bottom <= 500f)
    }

    // ── cropRemovalFraction ───────────────────────────────────────────────────

    @Test
    fun `no crop returns zero removal fraction`() {
        val rect = com.wallpapercropfixer.domain.model.CropRect(0f, 0f, 100f, 100f)
        val fraction = CropMath.cropRemovalFraction(100, 100, rect)
        assertEquals(0f, fraction, tolerance)
    }

    @Test
    fun `half area crop returns 0_5 removal fraction`() {
        val rect = com.wallpapercropfixer.domain.model.CropRect(0f, 0f, 50f, 100f)
        val fraction = CropMath.cropRemovalFraction(100, 100, rect)
        assertEquals(0.5f, fraction, tolerance)
    }

    // ── computePaddedPlacementRect ────────────────────────────────────────────

    @Test
    fun `tall crop on square canvas places image vertically centered`() {
        val cropRect = com.wallpapercropfixer.domain.model.CropRect(0f, 0f, 100f, 200f) // ratio 0.5
        val placement = CropMath.computePaddedPlacementRect(
            canvasWidth = 100,
            canvasHeight = 100,
            cropRect = cropRect,
            focusPoint = CropMath.CENTER_FOCUS
        )
        // cropAspect = 0.5; canvasAspect = 1.0 → drawW = drawH * 0.5 → drawH = 100, drawW = 50
        assertEquals(50f, placement.width, tolerance)
        assertEquals(100f, placement.height, tolerance)
    }

    @Test
    fun `wide crop on square canvas places image horizontally`() {
        val cropRect = com.wallpapercropfixer.domain.model.CropRect(0f, 0f, 200f, 100f) // ratio 2.0
        val placement = CropMath.computePaddedPlacementRect(
            canvasWidth = 100,
            canvasHeight = 100,
            cropRect = cropRect,
            focusPoint = CropMath.CENTER_FOCUS
        )
        // cropAspect = 2.0; canvasAspect = 1.0 → drawW = 100, drawH = 50
        assertEquals(100f, placement.width, tolerance)
        assertEquals(50f, placement.height, tolerance)
    }

    // ── SAFE_FIT_PADDING_THRESHOLD ────────────────────────────────────────────

    @Test
    fun `safe fit threshold is 0_20`() {
        assertEquals(0.20f, CropMath.SAFE_FIT_PADDING_THRESHOLD, tolerance)
    }
}
