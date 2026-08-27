package com.wallpapercropfixer.data.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import com.wallpapercropfixer.domain.model.WallpaperTarget
import com.wallpapercropfixer.domain.repository.WallpaperApplyRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** The device or profile does not support changing wallpapers. */
class WallpaperUnsupportedException(cause: Throwable? = null) :
    Exception("Wallpapers are not supported on this device or profile", cause)

/** Device policy (e.g. a work profile) forbids setting wallpapers. */
class WallpaperPolicyDisallowedException(cause: Throwable? = null) :
    Exception("Setting wallpaper is disallowed by device policy", cause)

/** WallpaperManager refused or failed to apply the bitmap. */
class WallpaperSetFailedException(detail: String) : Exception(detail)

/** Only the home-screen half of a BOTH apply failed. */
class HomeScreenApplyFailedException : Exception()

/** Only the lock-screen half of a BOTH apply failed. */
class LockScreenApplyFailedException : Exception()

/** Both halves of a BOTH apply failed. */
class BothScreensApplyFailedException : Exception()

class AndroidWallpaperApplyRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) : WallpaperApplyRepository {

    override suspend fun applyWallpaper(bitmap: Bitmap, target: WallpaperTarget): Result<Unit> =
        runCatching {
            val wallpaperManager = WallpaperManager.getInstance(context)

            if (!wallpaperManager.isWallpaperSupported) {
                throw WallpaperUnsupportedException()
            }
            if (!wallpaperManager.isSetWallpaperAllowed) {
                throw WallpaperPolicyDisallowedException()
            }
            val flag = when (target) {
                WallpaperTarget.HOME -> WallpaperManager.FLAG_SYSTEM
                WallpaperTarget.LOCK -> WallpaperManager.FLAG_LOCK
                WallpaperTarget.BOTH -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
            }
            val resultId = wallpaperManager.setBitmap(bitmap, null, true, flag)
            if (resultId <= 0) {
                throw WallpaperSetFailedException("WallpaperManager returned failure code $resultId")
            }
        }
}