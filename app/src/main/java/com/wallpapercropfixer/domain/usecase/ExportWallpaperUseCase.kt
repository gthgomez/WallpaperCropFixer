package com.wallpapercropfixer.domain.usecase

import android.graphics.Bitmap
import com.wallpapercropfixer.domain.repository.WallpaperExportRepository
import javax.inject.Inject

class ExportWallpaperUseCase @Inject constructor(
    private val exportRepository: WallpaperExportRepository
) {
    suspend operator fun invoke(
        bitmap: Bitmap,
        fileName: String,
        quality: Int
    ): String = exportRepository.exportBitmap(
        bitmap = bitmap,
        fileName = fileName,
        format = Bitmap.CompressFormat.JPEG,
        quality = quality
    )
}
