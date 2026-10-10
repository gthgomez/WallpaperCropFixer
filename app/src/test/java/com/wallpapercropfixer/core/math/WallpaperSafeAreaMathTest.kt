package com.wallpapercropfixer.core.math

import com.wallpapercropfixer.domain.model.CropRect
import com.wallpapercropfixer.domain.model.FaceBounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WallpaperSafeAreaMathTest {

    @Test
    fun `visible window is centered and screen-sized inside a wider canvas`() {
        val window = WallpaperSafeAreaMath.visibleWindow(
            canvasWidth = 2000, canvasHeight = 1000, screenWidth = 1000, screenHeight = 1000
        )
        assertEquals(500f, window.left, 0.001f)
        assertEquals(1500f, window.right, 0.001f)
        assertEquals(0f, window.top, 0.001f)
        assertEquals(1000f, window.bottom, 0.001f)
    }

    @Test
    fun `visible window equals the canvas when it fits the screen`() {
        val window = WallpaperSafeAreaMath.visibleWindow(
            canvasWidth = 1080, canvasHeight = 2400, screenWidth = 1080, screenHeight = 2400
        )
        assertEquals(CropRect(0f, 0f, 1080f, 2400f), window)
    }

    @Test
    fun `lock clock area is inset and in the upper band`() {
        val area = WallpaperSafeAreaMath.lockClockSafeArea(canvasWidth = 1000, canvasHeight = 2000)
        // sideInset = 80, top = 100, height = 360
        assertEquals(80f, area.left, 0.001f)
        assertEquals(100f, area.top, 0.001f)
        assertEquals(920f, area.right, 0.001f)
        assertEquals(460f, area.bottom, 0.001f)
    }

    @Test
    fun `intersects detects overlap and separation`() {
        val a = CropRect(0f, 0f, 100f, 100f)
        assertTrue(WallpaperSafeAreaMath.intersects(a, CropRect(50f, 50f, 150f, 150f)))
        assertFalse(WallpaperSafeAreaMath.intersects(a, CropRect(101f, 0f, 200f, 100f)))
        assertFalse(WallpaperSafeAreaMath.intersects(a, CropRect(100f, 0f, 200f, 100f)))
    }

    @Test
    fun `bound to area intersection`() {
        val area = CropRect(0f, 0f, 100f, 100f)
        assertTrue(WallpaperSafeAreaMath.intersects(FaceBounds(90f, 90f, 120f, 120f), area))
        assertFalse(WallpaperSafeAreaMath.intersects(FaceBounds(100f, 0f, 200f, 50f), area))
    }

    @Test
    fun `exposedToParallax is true only for bounds in the scroll margin`() {
        val canvas = CropRect(0f, 0f, 2000f, 1000f)
        val window = CropRect(500f, 0f, 1500f, 1000f)
        // Left margin subject
        assertTrue(WallpaperSafeAreaMath.exposedToParallax(FaceBounds(0f, 100f, 200f, 400f), canvas, window))
        // Fully within the visible window
        assertFalse(WallpaperSafeAreaMath.exposedToParallax(FaceBounds(600f, 100f, 800f, 400f), canvas, window))
        // Outside the canvas entirely
        assertFalse(WallpaperSafeAreaMath.exposedToParallax(FaceBounds(3000f, 0f, 3100f, 100f), canvas, window))
    }

    @Test
    fun `exposedToParallax is false when the canvas has no margin`() {
        val canvas = CropRect(0f, 0f, 1080f, 2400f)
        val window = CropRect(0f, 0f, 1080f, 2400f)
        assertFalse(WallpaperSafeAreaMath.exposedToParallax(FaceBounds(0f, 0f, 100f, 100f), canvas, window))
    }
}
