package com.wallpapercropfixer.domain.usecase

import com.wallpapercropfixer.domain.engine.WallpaperCropEngine
import com.wallpapercropfixer.domain.model.SubjectAnalysis
import com.wallpapercropfixer.domain.model.WallpaperRenderPlan
import com.wallpapercropfixer.domain.model.WallpaperRenderRequest
import javax.inject.Inject

class BuildWallpaperRenderPlanUseCase @Inject constructor(
    private val cropEngine: WallpaperCropEngine
) {
    operator fun invoke(
        request: WallpaperRenderRequest,
        subjectAnalysis: SubjectAnalysis?
    ): WallpaperRenderPlan = cropEngine.buildPlan(request, subjectAnalysis)
}
