package com.wallpapercropfixer.domain.repository

import android.graphics.Bitmap
import com.wallpapercropfixer.domain.model.WallpaperTarget

interface WallpaperApplyRepository {
    suspend fun applyWallpaper(bitmap: Bitmap, target: WallpaperTarget): Result<Unit>
}
