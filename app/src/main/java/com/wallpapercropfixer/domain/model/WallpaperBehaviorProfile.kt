package com.wallpapercropfixer.domain.model

data class WallpaperBehaviorProfile(
    val profileId: String,
    val brand: String,
    val launcherHint: String?,
    val homeWidthMultiplier: Float,
    val lockWidthMultiplier: Float,
    val cropBiasX: Float,
    val cropBiasY: Float,
    val separateTargetsSupported: Boolean
)
