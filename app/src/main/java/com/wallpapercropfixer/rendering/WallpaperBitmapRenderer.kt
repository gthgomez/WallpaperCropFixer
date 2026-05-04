package com.wallpapercropfixer.rendering

import android.graphics.Bitmap
import com.wallpapercropfixer.domain.model.WallpaperRenderPlan
import com.wallpapercropfixer.domain.model.WallpaperRenderRequest

interface WallpaperBitmapRenderer {
    suspend fun render(request: WallpaperRenderRequest, plan: WallpaperRenderPlan): Bitmap
}
