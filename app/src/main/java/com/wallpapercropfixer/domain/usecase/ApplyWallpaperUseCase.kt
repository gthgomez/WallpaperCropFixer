package com.wallpapercropfixer.domain.usecase

import android.graphics.Bitmap
import com.wallpapercropfixer.domain.model.WallpaperTarget
import com.wallpapercropfixer.domain.repository.WallpaperApplyRepository
import javax.inject.Inject

class ApplyWallpaperUseCase @Inject constructor(
    private val wallpaperApplyRepository: WallpaperApplyRepository
) {
    suspend operator fun invoke(
        bitmap: Bitmap,
        target: WallpaperTarget
    ): Result<Unit> = wallpaperApplyRepository.applyWallpaper(bitmap, target)
}
