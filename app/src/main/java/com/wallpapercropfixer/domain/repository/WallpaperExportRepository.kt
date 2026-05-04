package com.wallpapercropfixer.domain.repository

import android.graphics.Bitmap

interface WallpaperExportRepository {
    suspend fun exportBitmap(
        bitmap: Bitmap,
        fileName: String,
        format: Bitmap.CompressFormat,
        quality: Int
    ): String
}
