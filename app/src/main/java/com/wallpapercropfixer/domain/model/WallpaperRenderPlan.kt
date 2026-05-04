package com.wallpapercropfixer.domain.model

data class WallpaperRenderPlan(
    val sourceCropRect: CropRect,
    val targetCanvasSpec: TargetCanvasSpec,
    val outputImagePlacement: CropRect,
    val usePadding: Boolean,
    val backgroundFillMode: BackgroundFillMode,
    val finalFocusPoint: FocusPoint?
)
