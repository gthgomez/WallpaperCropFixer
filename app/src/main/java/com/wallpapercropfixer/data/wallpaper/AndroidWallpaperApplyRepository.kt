package com.wallpapercropfixer.data.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import com.wallpapercropfixer.domain.model.WallpaperTarget
import com.wallpapercropfixer.domain.repository.WallpaperApplyRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AndroidWallpaperApplyRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) : WallpaperApplyRepository {

    override suspend fun applyWallpaper(bitmap: Bitmap, target: WallpaperTarget): Result<Unit> =
        runCatching {
            val wallpaperManager = WallpaperManager.getInstance(context)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                if (!wallpaperManager.isWallpaperSupported) {
                    error("Wallpapers are not supported on this device or profile")
                }
                if (!wallpaperManager.isSetWallpaperAllowed) {
                    error("Setting wallpaper is disallowed by device policy")
                }
                val flag = when (target) {
                    WallpaperTarget.HOME -> WallpaperManager.FLAG_SYSTEM
                    WallpaperTarget.LOCK -> WallpaperManager.FLAG_LOCK
                    WallpaperTarget.BOTH -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                }
                val resultId = wallpaperManager.setBitmap(bitmap, null, true, flag)
                if (resultId <= 0) {
                    error("WallpaperManager returned failure code $resultId")
                }
            } else {
                @Suppress("DEPRECATION")
                wallpaperManager.setBitmap(bitmap)
            }
        }
}
