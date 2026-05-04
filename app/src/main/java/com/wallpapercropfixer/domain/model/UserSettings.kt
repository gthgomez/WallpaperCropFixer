package com.wallpapercropfixer.domain.model

data class UserSettings(
    val defaultCropMode: CropMode = CropMode.BALANCED,
    val defaultWallpaperTarget: WallpaperTarget = WallpaperTarget.HOME,
    val defaultBackgroundFillMode: BackgroundFillMode = BackgroundFillMode.BLUR,
    val defaultFaceAwareEnabled: Boolean = true,
    val exportJpegQuality: Int = 92
)
