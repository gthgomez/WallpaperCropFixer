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
        aspectRatio = 1440f / 3120f,
        supportsSeparateLockScreen = true
    )

    private val behavior = WallpaperBehaviorProfile(
        profileId = "test",
        brand = "test",
        launcherHint = null,
        homeWidthMultiplier = 1.12f,
        lockWidthMultiplier = 1.0f,
        cropBiasX = 0.5f,
        cropBiasY = 0.5f,
        separateTargetsSupported = true
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
}
