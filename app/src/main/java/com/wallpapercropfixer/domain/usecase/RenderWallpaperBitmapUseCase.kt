package com.wallpapercropfixer.domain.usecase

import android.graphics.Bitmap
import com.wallpapercropfixer.domain.model.WallpaperRenderPlan
import com.wallpapercropfixer.domain.model.WallpaperRenderRequest
import com.wallpapercropfixer.rendering.WallpaperBitmapRenderer
import javax.inject.Inject

class RenderWallpaperBitmapUseCase @Inject constructor(
    private val renderer: WallpaperBitmapRenderer
) {
    suspend operator fun invoke(
        request: WallpaperRenderRequest,
        plan: WallpaperRenderPlan
    ): Bitmap = renderer.render(request, plan)
}
