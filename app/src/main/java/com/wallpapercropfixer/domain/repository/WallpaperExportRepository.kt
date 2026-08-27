package com.wallpapercropfixer.domain.repository

import android.graphics.Bitmap

data class ExportResult(
    val destination: ExportDestination,
    val pathOrUri: String,
    val displayName: String
)

enum class ExportDestination {
    /** Saved via MediaStore to the shared Pictures/WallpaperCropFixer (API 29+). */
    MEDIA_STORE,

    /** Saved to app-specific external storage — not visible in the gallery. */
    APP_EXTERNAL_FILES,

    /** Saved to internal app storage as a last resort. */
    APP_INTERNAL_FILES
}

interface WallpaperExportRepository {
    suspend fun exportBitmap(
        bitmap: Bitmap,
        fileName: String,
        format: Bitmap.CompressFormat,
        quality: Int
    ): ExportResult
}