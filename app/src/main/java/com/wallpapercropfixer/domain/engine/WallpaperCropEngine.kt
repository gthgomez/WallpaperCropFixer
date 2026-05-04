package com.wallpapercropfixer.domain.engine

import com.wallpapercropfixer.domain.model.SubjectAnalysis
import com.wallpapercropfixer.domain.model.WallpaperRenderPlan
import com.wallpapercropfixer.domain.model.WallpaperRenderRequest

interface WallpaperCropEngine {
    fun buildPlan(
        request: WallpaperRenderRequest,
        subjectAnalysis: SubjectAnalysis?
    ): WallpaperRenderPlan
}
