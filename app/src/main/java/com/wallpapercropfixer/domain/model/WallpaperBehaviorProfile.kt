package com.wallpapercropfixer.domain.model

data class WallpaperBehaviorProfile(
    val profileId: String,
    val brand: String,
    val homeWidthMultiplier: Float,
    val lockWidthMultiplier: Float
)