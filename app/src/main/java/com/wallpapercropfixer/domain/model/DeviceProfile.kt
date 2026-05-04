package com.wallpapercropfixer.domain.model

data class DeviceProfile(
    val manufacturer: String,
    val model: String,
    val androidVersion: Int,
    val screenWidthPx: Int,
    val screenHeightPx: Int,
    val density: Float,
    val aspectRatio: Float,
    val supportsSeparateLockScreen: Boolean
)
