package com.wallpapercropfixer.domain.engine

import com.wallpapercropfixer.core.math.AspectRatioUtils
import com.wallpapercropfixer.core.math.CropMath
import com.wallpapercropfixer.domain.model.CropRect
import com.wallpapercropfixer.domain.model.SubjectAnalysis
import com.wallpapercropfixer.domain.model.WallpaperRenderPlan
import com.wallpapercropfixer.domain.model.WallpaperRenderRequest
import com.wallpapercropfixer.domain.model.WallpaperTarget
import javax.inject.Inject

class WallpaperCropEngineImpl @Inject constructor(
    private val canvasSpecFactory: TargetCanvasSpecFactory,
    private val focusPointResolver: FocusPointResolver,
    private val strategySelector: CropStrategySelector
) : WallpaperCropEngine {

    override fun buildPlan(
        request: WallpaperRenderRequest,
        subjectAnalysis: SubjectAnalysis?
    ): WallpaperRenderPlan {
        // Use HOME canvas as primary for BOTH target
        val effectiveTarget = if (request.target == WallpaperTarget.BOTH) WallpaperTarget.HOME
                              else request.target

        val canvasSpec = canvasSpecFactory.create(
            request.deviceProfile,
            request.behaviorProfile,
            effectiveTarget
        )

        val focusPoint = focusPointResolver.resolve(
            manual = request.manualFocusPoint,
            faceAwareEnabled = request.enableFaceAwareFocus,
            subjectAnalysis = subjectAnalysis,
            sourceWidth = request.source.width,
            sourceHeight = request.source.height
        )

        val targetRatio = AspectRatioUtils.ratio(canvasSpec.widthPx, canvasSpec.heightPx)

        val standardCropRect = CropMath.computeFocusBiasedCropRect(
            sourceWidth = request.source.width,
            sourceHeight = request.source.height,
            targetRatio = targetRatio,
            focusPoint = focusPoint
        )

        val removalFraction = CropMath.cropRemovalFraction(
            request.source.width,
            request.source.height,
            standardCropRect
        )

        val usePadding = strategySelector.shouldUsePadding(request.cropMode, removalFraction)
        val chosenCropRect = strategySelector.selectCropRect(request.cropMode, standardCropRect, usePadding)

        // When padding is used, the image occupies a sub-region of the canvas.
        // When not padding, the image fills the entire canvas after crop.
        val outputPlacement: CropRect = if (usePadding) {
            CropMath.computePaddedPlacementRect(
                canvasWidth = canvasSpec.widthPx,
                canvasHeight = canvasSpec.heightPx,
                cropRect = chosenCropRect,
                focusPoint = focusPoint
            )
        } else {
            CropRect(0f, 0f, canvasSpec.widthPx.toFloat(), canvasSpec.heightPx.toFloat())
        }

        return WallpaperRenderPlan(
            sourceCropRect = chosenCropRect,
            targetCanvasSpec = canvasSpec,
            outputImagePlacement = outputPlacement,
            usePadding = usePadding,
            backgroundFillMode = request.backgroundFillMode,
            finalFocusPoint = focusPoint
        )
    }
}
