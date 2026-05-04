package com.wallpapercropfixer.domain.model

data class RenderedWallpaper(
    val bitmapWidth: Int,
    val bitmapHeight: Int,
    val outputUri: String?,
    val target: WallpaperTarget
)
