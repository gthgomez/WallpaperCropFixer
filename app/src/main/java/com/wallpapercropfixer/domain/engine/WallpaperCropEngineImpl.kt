package com.wallpapercropfixer.domain.engine

import com.wallpapercropfixer.core.math.AspectRatioUtils
import com.wallpapercropfixer.core.math.CropMath
import com.wallpapercropfixer.core.math.WallpaperSafeAreaMath
import com.wallpapercropfixer.domain.model.CropRect
import com.wallpapercropfixer.domain.model.FaceBounds
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

        val hasClippedFaces = if (request.enableFaceAwareFocus && subjectAnalysis != null && subjectAnalysis.faces.isNotEmpty()) {
            subjectAnalysis.faces.any { face ->
                face.left < standardCropRect.left ||
                face.right > standardCropRect.right ||
                face.top < standardCropRect.top ||
                face.bottom > standardCropRect.bottom
            }
        } else false

        val fullSourceRect = CropRect(0f, 0f, request.source.width.toFloat(), request.source.height.toFloat())
        val paddingRequested = strategySelector.shouldUsePadding(request.cropMode, removalFraction, hasClippedFaces)
        val chosenCropRect = strategySelector.selectCropRect(
            request.cropMode, standardCropRect, fullSourceRect, paddingRequested,
            if (request.enableFaceAwareFocus) subjectAnalysis?.faces.orEmpty() else emptyList()
        )
        // Compare aspect ratios with Double cross-products. A coarse float
        // epsilon can classify a genuine near-match as equal and stretch the
        // complete Safe Fit source instead of preserving its aspect ratio.
        val usePadding = paddingRequested &&
            chosenCropRect.width.toDouble() * canvasSpec.heightPx.toDouble() !=
            chosenCropRect.height.toDouble() * canvasSpec.widthPx.toDouble()

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

        val canvasRect = CropRect(0f, 0f, canvasSpec.widthPx.toFloat(), canvasSpec.heightPx.toFloat())
        val visibleWindow = WallpaperSafeAreaMath.visibleWindow(
            canvasWidth = canvasSpec.widthPx,
            canvasHeight = canvasSpec.heightPx,
            screenWidth = request.deviceProfile.screenWidthPx,
            screenHeight = request.deviceProfile.screenHeightPx
        )
        val lockClockSafeArea = if (effectiveTarget == WallpaperTarget.LOCK) {
            WallpaperSafeAreaMath.lockClockSafeArea(canvasSpec.widthPx, canvasSpec.heightPx)
        } else {
            null
        }

        // Guide advisories use the analyzed anchor rects (subjects preferred, else faces)
        // mapped into canvas space through the same transform as the focus overlay.
        val anchorBounds = if (request.enableFaceAwareFocus && subjectAnalysis != null) {
            subjectAnalysis.subjects.ifEmpty { subjectAnalysis.faces }
        } else {
            emptyList()
        }
        val canvasAnchors = anchorBounds.map { bounds ->
            CropMath.sourceRectToCanvasRect(
                sourceRect = CropMath.faceBoundsToCropRect(bounds),
                sourceWidth = request.source.width,
                sourceHeight = request.source.height,
                sourceCropRect = chosenCropRect,
                outputImagePlacement = outputPlacement,
                canvasWidth = canvasSpec.widthPx,
                canvasHeight = canvasSpec.heightPx
            )
        }
        val subjectInClockZone = lockClockSafeArea != null &&
            canvasAnchors.any { WallpaperSafeAreaMath.intersects(it, lockClockSafeArea) }
        val subjectExposedToParallax = canvasAnchors.any { anchor ->
            WallpaperSafeAreaMath.exposedToParallax(
                bounds = FaceBounds(anchor.left, anchor.top, anchor.right, anchor.bottom),
                canvas = canvasRect,
                visibleWindow = visibleWindow
            )
        }

        return WallpaperRenderPlan(
            sourceCropRect = chosenCropRect,
            targetCanvasSpec = canvasSpec,
            outputImagePlacement = outputPlacement,
            usePadding = usePadding,
            backgroundFillMode = request.backgroundFillMode,
            finalFocusPoint = focusPoint,
            visibleWindow = visibleWindow,
            lockClockSafeArea = lockClockSafeArea,
            subjectInClockZone = subjectInClockZone,
            subjectExposedToParallax = subjectExposedToParallax
        )
    }
}
