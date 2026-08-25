package com.wallpapercropfixer.core.math

import com.wallpapercropfixer.domain.model.CropRect
import com.wallpapercropfixer.domain.model.FocusPoint

/**
 * Pure, stateless crop math. All functions operate on normalized or pixel coordinates.
 * No Android dependencies — fully unit-testable on JVM.
 */
object CropMath {

    /**
     * Computes a crop rect on the source image (in pixels) that matches [targetRatio],
     * biased toward [focusPoint] (normalized 0..1).
     *
     * Returns a CropRect with pixel coordinates clamped to [0, sourceWidth] x [0, sourceHeight].
     */
    fun computeFocusBiasedCropRect(
        sourceWidth: Int,
        sourceHeight: Int,
        targetRatio: Float,
        focusPoint: FocusPoint
    ): CropRect {
        require(sourceWidth > 0 && sourceHeight > 0) { "Source dimensions must be positive" }
        require(targetRatio > 0f) { "Target ratio must be positive" }

        val sourceRatio = sourceWidth.toFloat() / sourceHeight.toFloat()

        val cropW: Float
        val cropH: Float

        if (sourceRatio > targetRatio) {
            // Source is wider than target — crop left/right
            cropH = sourceHeight.toFloat()
            cropW = cropH * targetRatio
        } else {
            // Source is taller than target — crop top/bottom
            cropW = sourceWidth.toFloat()
            cropH = cropW / targetRatio
        }

        val focusPx = focusPoint.xNormalized * sourceWidth
        val focusPy = focusPoint.yNormalized * sourceHeight

        var left = focusPx - cropW / 2f
        var top = focusPy - cropH / 2f

        // Clamp so rect stays within source bounds
        left = left.coerceIn(0f, sourceWidth - cropW)
        top = top.coerceIn(0f, sourceHeight - cropH)

        return CropRect(
            left = left,
            top = top,
            right = left + cropW,
            bottom = top + cropH
        )
    }

    /**
     * Computes the fraction of source image area removed by [cropRect].
     * Returns 0f (no crop) to 1f (entire image cropped away).
     */
    fun cropRemovalFraction(sourceWidth: Int, sourceHeight: Int, cropRect: CropRect): Float {
        val sourceArea = sourceWidth.toFloat() * sourceHeight.toFloat()
        val cropArea = cropRect.width * cropRect.height
        return 1f - (cropArea / sourceArea)
    }

    /**
     * Returns the placement rect (in canvas pixels) where the source image (after crop)
     * should be drawn when the canvas is padded.
     *
     * The cropped image is fitted inside the canvas while preserving its aspect ratio,
     * centered around [focusPoint] (normalized).
     */
    fun computePaddedPlacementRect(
        canvasWidth: Int,
        canvasHeight: Int,
        cropRect: CropRect,
        focusPoint: FocusPoint
    ): CropRect {
        val cropAspect = cropRect.width / cropRect.height
        val canvasAspect = canvasWidth.toFloat() / canvasHeight.toFloat()

        val drawW: Float
        val drawH: Float

        if (cropAspect > canvasAspect) {
            drawW = canvasWidth.toFloat()
            drawH = drawW / cropAspect
        } else {
            drawH = canvasHeight.toFloat()
            drawW = drawH * cropAspect
        }

        val centerX = focusPoint.xNormalized * canvasWidth
        val centerY = focusPoint.yNormalized * canvasHeight

        var left = centerX - drawW / 2f
        var top = centerY - drawH / 2f

        left = left.coerceIn(0f, canvasWidth - drawW)
        top = top.coerceIn(0f, canvasHeight - drawH)

        return CropRect(left, top, left + drawW, top + drawH)
    }

    /**
     * Maps a focus point from source-image normalized space [0..1] to canvas normalized space [0..1],
     * taking into account the plan's sourceCropRect and outputImagePlacement.
     */
    fun sourceFocusToCanvasFocus(
        sourceFocus: FocusPoint,
        sourceWidth: Int,
        sourceHeight: Int,
        sourceCropRect: CropRect,
        outputImagePlacement: CropRect,
        canvasWidth: Int,
        canvasHeight: Int
    ): FocusPoint {
        if (sourceWidth <= 0 || sourceHeight <= 0 || canvasWidth <= 0 || canvasHeight <= 0) {
            return sourceFocus
        }
        val sourcePxX = sourceFocus.xNormalized * sourceWidth
        val sourcePxY = sourceFocus.yNormalized * sourceHeight

        val cropW = sourceCropRect.width
        val cropH = sourceCropRect.height
        if (cropW <= 0f || cropH <= 0f) return sourceFocus

        val normInCropX = ((sourcePxX - sourceCropRect.left) / cropW).coerceIn(0f, 1f)
        val normInCropY = ((sourcePxY - sourceCropRect.top) / cropH).coerceIn(0f, 1f)

        val canvasPxX = outputImagePlacement.left + normInCropX * outputImagePlacement.width
        val canvasPxY = outputImagePlacement.top + normInCropY * outputImagePlacement.height

        return FocusPoint(
            xNormalized = (canvasPxX / canvasWidth).coerceIn(0f, 1f),
            yNormalized = (canvasPxY / canvasHeight).coerceIn(0f, 1f)
        )
    }

    /**
     * Maps a focus tap from canvas normalized space [0..1] back to source-image normalized space [0..1],
     * taking into account the plan's sourceCropRect and outputImagePlacement.
     */
    fun canvasFocusToSourceFocus(
        canvasFocus: FocusPoint,
        sourceWidth: Int,
        sourceHeight: Int,
        sourceCropRect: CropRect,
        outputImagePlacement: CropRect,
        canvasWidth: Int,
        canvasHeight: Int
    ): FocusPoint {
        if (sourceWidth <= 0 || sourceHeight <= 0 || canvasWidth <= 0 || canvasHeight <= 0) {
            return canvasFocus
        }
        val canvasPxX = canvasFocus.xNormalized * canvasWidth
        val canvasPxY = canvasFocus.yNormalized * canvasHeight

        val placeW = outputImagePlacement.width
        val placeH = outputImagePlacement.height
        if (placeW <= 0f || placeH <= 0f) return canvasFocus

        val normInPlaceX = ((canvasPxX - outputImagePlacement.left) / placeW).coerceIn(0f, 1f)
        val normInPlaceY = ((canvasPxY - outputImagePlacement.top) / placeH).coerceIn(0f, 1f)

        val sourcePxX = sourceCropRect.left + normInPlaceX * sourceCropRect.width
        val sourcePxY = sourceCropRect.top + normInPlaceY * sourceCropRect.height

        return FocusPoint(
            xNormalized = (sourcePxX / sourceWidth).coerceIn(0f, 1f),
            yNormalized = (sourcePxY / sourceHeight).coerceIn(0f, 1f)
        )
    }

    /**
     * Computes the crop fraction threshold above which Safe Fit mode prefers padding.
     * Exposed as a constant so tests can assert against it.
     */
    const val SAFE_FIT_PADDING_THRESHOLD = 0.20f

    /**
     * Center FocusPoint convenience.
     */
    val CENTER_FOCUS = FocusPoint(0.5f, 0.5f)
}
