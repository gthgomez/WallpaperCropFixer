package com.wallpapercropfixer.domain.engine

import com.wallpapercropfixer.domain.model.DeviceProfile
import com.wallpapercropfixer.domain.model.TargetCanvasSpec
import com.wallpapercropfixer.domain.model.WallpaperBehaviorProfile
import com.wallpapercropfixer.domain.model.WallpaperTarget
import javax.inject.Inject

class TargetCanvasSpecFactory @Inject constructor() {
    fun create(
        deviceProfile: DeviceProfile,
        behaviorProfile: WallpaperBehaviorProfile,
        target: WallpaperTarget
    ): TargetCanvasSpec {
        val home = target != WallpaperTarget.LOCK
        val desiredWidth = deviceProfile.desiredWallpaperWidthPx
        val desiredHeight = deviceProfile.desiredWallpaperHeightPx
        // Reject undersized, absent and pathological hints as a pair. 16 MP bounds
        // one ARGB canvas to 64 MB; multiplication uses Long to avoid overflow.
        val validHint = desiredWidth >= deviceProfile.screenWidthPx &&
            desiredHeight >= deviceProfile.screenHeightPx &&
            desiredWidth in 1..8192 && desiredHeight in 1..8192 &&
            desiredWidth.toLong() * desiredHeight <= 16_000_000L
        val width = if (home && validHint) desiredWidth else if (home) {
            (deviceProfile.screenWidthPx * behaviorProfile.homeWidthMultiplier).toInt()
        } else deviceProfile.screenWidthPx
        val height = if (home && validHint) desiredHeight else deviceProfile.screenHeightPx
        return TargetCanvasSpec(widthPx = width, heightPx = height, target = target)
    }
}
