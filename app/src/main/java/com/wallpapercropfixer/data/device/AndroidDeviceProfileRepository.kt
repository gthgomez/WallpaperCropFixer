package com.wallpapercropfixer.data.device

import android.app.WallpaperManager
import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import com.wallpapercropfixer.domain.model.DeviceProfile
import com.wallpapercropfixer.domain.repository.DeviceProfileRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AndroidDeviceProfileRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) : DeviceProfileRepository {

    override suspend fun getCurrentDeviceProfile(): DeviceProfile {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val width: Int
        val height: Int
        val density: Float

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = wm.currentWindowMetrics
            width = windowMetrics.bounds.width()
            height = windowMetrics.bounds.height()
            density = context.resources.displayMetrics.density
        } else {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(metrics)
            width = metrics.widthPixels
            height = metrics.heightPixels
            density = metrics.density
        }

        val supportsSeparate = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
            WallpaperManager.getInstance(context).isWallpaperSupported

        return DeviceProfile(
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            androidVersion = Build.VERSION.SDK_INT,
            screenWidthPx = width,
            screenHeightPx = height,
            density = density,
            aspectRatio = width.toFloat() / height.toFloat(),
            supportsSeparateLockScreen = supportsSeparate
        )
    }
}
