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
        val multiplier = when (target) {
            WallpaperTarget.HOME -> behaviorProfile.homeWidthMultiplier
            WallpaperTarget.LOCK -> behaviorProfile.lockWidthMultiplier
            WallpaperTarget.BOTH -> behaviorProfile.homeWidthMultiplier // HOME is primary
        }

        val canvasWidth = (deviceProfile.screenWidthPx * multiplier).toInt()
        val canvasHeight = deviceProfile.screenHeightPx

        return TargetCanvasSpec(
            widthPx = canvasWidth,
            heightPx = canvasHeight,
            target = target
        )
    }
}
