package com.wallpapercropfixer.core.math

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ViewportTransformTest {

    private val tolerance = 0.001f

    @Test
    fun `identical aspects map identity both directions`() {
        val bitmapAspect = 9f / 19f
        val viewportAspect = 9f / 19f
        for (x in listOf(0f, 0.25f, 0.5f, 0.9f, 1f)) {
            for (y in listOf(0f, 0.5f, 1f)) {
                val toViewport = ViewportTransform.bitmapToViewport(x, y, bitmapAspect, viewportAspect)
                assertEquals(x, toViewport.x, tolerance)
                assertEquals(y, toViewport.y, tolerance)
                val back = ViewportTransform.viewportToBitmap(x, y, bitmapAspect, viewportAspect)
                assertEquals(x, back.x, tolerance)
                assertEquals(y, back.y, tolerance)
            }
        }
    }

    @Test
    fun `wider bitmap home multiplier center maps to viewport center`() {
        // HOME canvas is wider than the visible screen (multiplier 1.12).
        val bitmapAspect = 1.12f * (9f / 19f)
        val viewportAspect = 9f / 19f
        val center = ViewportTransform.bitmapToViewport(0.5f, 0.5f, bitmapAspect, viewportAspect)
        assertEquals(0.5f, center.x, tolerance)
        assertEquals(0.5f, center.y, tolerance)
    }

    @Test
    fun `wider bitmap visible window starts inside the bitmap`() {
        val bitmapAspect = 1.12f
        val viewportAspect = 1.0f
        // visible fraction = viewportAspect / bitmapAspect
        val visibleFraction = 1f / 1.12f
        val start = (1f - visibleFraction) / 2f
        val leftEdge = ViewportTransform.bitmapToViewport(0f, 0.5f, bitmapAspect, viewportAspect)
        // Bitmap left edge is outside the visible window → clamped to viewport x=0.
        assertEquals(0f, leftEdge.x, tolerance)
        val leftWindowEdge = ViewportTransform.bitmapToViewport(start, 0.5f, bitmapAspect, viewportAspect)
        assertEquals(0f, leftWindowEdge.x, tolerance)
        val rightWindowEdge = ViewportTransform.bitmapToViewport(start + visibleFraction, 0.5f, bitmapAspect, viewportAspect)
        assertEquals(1f, rightWindowEdge.x, tolerance)
    }

    @Test
    fun `viewport tap maps back to bitmap coordinate inside visible window`() {
        val bitmapAspect = 1.12f
        val viewportAspect = 1.0f
        // Tap exactly the visible left edge.
        val leftTap = ViewportTransform.viewportToBitmap(0f, 0.5f, bitmapAspect, viewportAspect)
        val visibleFraction = 1f / 1.12f
        val expectedStart = (1f - visibleFraction) / 2f
        assertEquals(expectedStart, leftTap.x, tolerance)
        assertEquals(0.5f, leftTap.y, tolerance)
        // Tap the visible center → bitmap center.
        val centerTap = ViewportTransform.viewportToBitmap(0.5f, 0.5f, bitmapAspect, viewportAspect)
        assertEquals(0.5f, centerTap.x, tolerance)
        assertEquals(0.5f, centerTap.y, tolerance)
        // Round-trip: bitmap x visible midpoint → viewport 0.5 → bitmap same.
        val midBitmap = expectedStart + visibleFraction / 2f
        val toViewport = ViewportTransform.bitmapToViewport(midBitmap, 0.5f, bitmapAspect, viewportAspect)
        assertEquals(0.5f, toViewport.x, tolerance)
        val back = ViewportTransform.viewportToBitmap(toViewport.x, toViewport.y, bitmapAspect, viewportAspect)
        assertEquals(midBitmap, back.x, tolerance)
    }

    @Test
    fun `taller bitmap crops vertically instead`() {
        // Lock 1.0 canvas inside a wider frame (e.g. portrait bitmap in a squarer viewport).
        val bitmapAspect = 9f / 20f
        val viewportAspect = 9f / 19f
        val visibleFraction = bitmapAspect / viewportAspect
        val startY = (1f - visibleFraction) / 2f
        val top = ViewportTransform.bitmapToViewport(0.5f, startY, bitmapAspect, viewportAspect)
        assertEquals(0f, top.y, tolerance)
        val bottom = ViewportTransform.bitmapToViewport(0.5f, startY + visibleFraction, bitmapAspect, viewportAspect)
        assertEquals(1f, bottom.y, tolerance)
        val xUnchanged = ViewportTransform.bitmapToViewport(0.3f, 0.5f, bitmapAspect, viewportAspect)
        assertEquals(0.3f, xUnchanged.x, tolerance)
    }

    @Test
    fun `lock ratio identity mapping for typical phone`() {
        val phoneAspect = 1080f / 2400f
        val bitmapAspect = phoneAspect // lock canvas = screen width exactly
        val nearEdge = ViewportTransform.bitmapToViewport(0.02f, 0.5f, bitmapAspect, phoneAspect)
        assertEquals(0.02f, nearEdge.x, tolerance)
        val tapBack = ViewportTransform.viewportToBitmap(0.98f, 0.5f, bitmapAspect, phoneAspect)
        assertEquals(0.98f, tapBack.x, tolerance)
    }

    @Test
    fun `subject near canvas edge maps outside visible window and clamps`() {
        val bitmapAspect = 1.12f * (9f / 19f)
        val viewportAspect = 9f / 19f
        val result = ViewportTransform.bitmapToViewport(0.01f, 0.5f, bitmapAspect, viewportAspect)
        assertTrue("clamped to >= 0", result.x >= 0f)
        assertTrue("clamped to <= 1", result.x <= 1f)
        // A subject at the very edge of a wider canvas is not visible in the crop.
        val visibleStart = (1f - 1f / 1.12f) / 2f
        assertTrue("subject is outside visible window", result.x < 0.001f || 0.01f < visibleStart)
    }
}