package com.wallpapercropfixer.domain.engine

import com.wallpapercropfixer.core.math.CropMath
import com.wallpapercropfixer.domain.model.CropMode
import com.wallpapercropfixer.domain.model.CropRect
import javax.inject.Inject

class CropStrategySelector @Inject constructor() {

    /**
     * Determines whether the given [cropRemovalFraction] or clipped faces should trigger padding
     * for the specified [cropMode].
     *
     * SAFE_FIT: pad when removal > threshold or when detected faces would be clipped
     * BALANCED: pad when removal > 2× threshold
     * FILL: never pad
     */
    fun shouldUsePadding(
        cropMode: CropMode,
        cropRemovalFraction: Float,
        hasClippedFaces: Boolean = false
    ): Boolean {
        return when (cropMode) {
            CropMode.SAFE_FIT -> (cropRemovalFraction > CropMath.SAFE_FIT_PADDING_THRESHOLD) || hasClippedFaces
            CropMode.BALANCED -> cropRemovalFraction > CropMath.SAFE_FIT_PADDING_THRESHOLD * 2f
            CropMode.FILL -> false
        }
    }

    /**
     * For SAFE_FIT with padding, return the full source crop rect so the entire photograph
     * is preserved and padded on the canvas.
     * For other modes, return the standard crop rect.
     */
    fun selectCropRect(
        cropMode: CropMode,
        standardCropRect: CropRect,
        fullSourceRect: CropRect,
        usePadding: Boolean
    ): CropRect {
        return if (usePadding && cropMode == CropMode.SAFE_FIT) {
            fullSourceRect
        } else {
            standardCropRect
        }
    }
}
