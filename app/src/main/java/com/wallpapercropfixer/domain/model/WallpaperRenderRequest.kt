package com.wallpapercropfixer.domain.model

data class WallpaperRenderRequest(
    val source: SourceImageMeta,
    val deviceProfile: DeviceProfile,
    val behaviorProfile: WallpaperBehaviorProfile,
    val target: WallpaperTarget,
    val cropMode: CropMode,
    val backgroundFillMode: BackgroundFillMode,
    val manualFocusPoint: FocusPoint?,
    val enableFaceAwareFocus: Boolean
)
