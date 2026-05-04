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
     * Computes the crop fraction threshold above which Safe Fit mode prefers padding.
     * Exposed as a constant so tests can assert against it.
     */
    const val SAFE_FIT_PADDING_THRESHOLD = 0.20f

    /**
     * Center FocusPoint convenience.
     */
    val CENTER_FOCUS = FocusPoint(0.5f, 0.5f)
}
