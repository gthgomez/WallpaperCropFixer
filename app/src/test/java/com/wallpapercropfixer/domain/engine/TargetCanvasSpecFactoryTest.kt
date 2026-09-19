package com.wallpapercropfixer.domain.engine

import com.wallpapercropfixer.domain.model.DeviceProfile
import com.wallpapercropfixer.domain.model.WallpaperBehaviorProfile
import com.wallpapercropfixer.domain.model.WallpaperTarget
import org.junit.Assert.assertEquals
import org.junit.Test

class TargetCanvasSpecFactoryTest {

    private val factory = TargetCanvasSpecFactory()

    private val device = DeviceProfile(
        manufacturer = "Test",
        model = "Phone",
        androidVersion = 34,
        screenWidthPx = 1440,
        screenHeightPx = 3120,
        density = 3.0f,
        aspectRatio = 1440f / 3120f
    )

    private val behavior = WallpaperBehaviorProfile(
        profileId = "test",
        brand = "test",
        homeWidthMultiplier = 1.12f,
        lockWidthMultiplier = 1.0f
    )

    @Test
    fun `home canvas applies homeWidthMultiplier`() {
        val spec = factory.create(device, behavior, WallpaperTarget.HOME)
        assertEquals((1440 * 1.12f).toInt(), spec.widthPx)
        assertEquals(3120, spec.heightPx)
    }

    @Test
    fun `lock canvas uses lockWidthMultiplier (1_0 = screen width)`() {
        val spec = factory.create(device, behavior, WallpaperTarget.LOCK)
        assertEquals(1440, spec.widthPx)
        assertEquals(3120, spec.heightPx)
    }

    @Test
    fun `both target uses home multiplier as primary`() {
        val specBoth = factory.create(device, behavior, WallpaperTarget.BOTH)
        val specHome = factory.create(device, behavior, WallpaperTarget.HOME)
        assertEquals(specHome.widthPx, specBoth.widthPx)
    }

    @Test
    fun `valid Android hints override OEM HOME sizing including height`() {
        val hinted = device.copy(desiredWallpaperWidthPx = 2880, desiredWallpaperHeightPx = 3200)
        for (target in listOf(WallpaperTarget.HOME, WallpaperTarget.BOTH)) {
            val canvas = factory.create(hinted, behavior, target)
            assertEquals(2880, canvas.widthPx)
            assertEquals(3200, canvas.heightPx)
        }
        val lock = factory.create(hinted, behavior.copy(lockWidthMultiplier = 2f), WallpaperTarget.LOCK)
        assertEquals(1440, lock.widthPx)
        assertEquals(3120, lock.heightPx)
    }

    @Test
    fun `absent undersized overflowing and excessive hints fall back to OEM`() {
        for ((w, h) in listOf(0 to 0, -1 to 3120, 2880 to 0, 1000 to 3120,
            2880 to 2000, Int.MAX_VALUE to Int.MAX_VALUE, 8192 to 8192, 9000 to 3120)) {
            val canvas = factory.create(device.copy(desiredWallpaperWidthPx = w,
                desiredWallpaperHeightPx = h), behavior, WallpaperTarget.HOME)
            assertEquals((1440 * 1.12f).toInt(), canvas.widthPx)
            assertEquals(3120, canvas.heightPx)
        }
    }
    @Test
    fun `unknown OEM without valid hints uses generic fallback`() {
        val generic = com.wallpapercropfixer.data.behavior.KnownWallpaperProfiles.matchBrand("unknown")
        val canvas = factory.create(device, generic, WallpaperTarget.HOME)
        assertEquals((1440 * 1.10f).toInt(), canvas.widthPx)
        assertEquals(3120, canvas.heightPx)
    }
}
