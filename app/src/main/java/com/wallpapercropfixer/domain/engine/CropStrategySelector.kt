package com.wallpapercropfixer.domain.engine

import com.wallpapercropfixer.core.math.CropMath
import com.wallpapercropfixer.domain.model.CropMode
import com.wallpapercropfixer.domain.model.CropRect
import javax.inject.Inject

class CropStrategySelector @Inject constructor() {

    companion object {
        /** Maximum area fraction of source image allowed to be cropped in BALANCED mode. */
        const val BALANCED_MAX_CROP_FRACTION = 0.35f
    }

    /**
     * Determines whether padding should be used.
     *
     * - SAFE_FIT: Always pads when aspect ratio differs, guaranteeing whole photo preservation.
     * - BALANCED: Pads if standard crop removal exceeds the 35% crop budget OR if faces are clipped.
     * - FILL: Never pads (fills screen).
     */
    fun shouldUsePadding(
        cropMode: CropMode,
        cropRemovalFraction: Float,
        hasClippedFaces: Boolean = false
    ): Boolean {
        return when (cropMode) {
            CropMode.SAFE_FIT -> cropRemovalFraction > 0.001f || hasClippedFaces
            CropMode.BALANCED -> (cropRemovalFraction > BALANCED_MAX_CROP_FRACTION) || hasClippedFaces
            CropMode.FILL -> false
        }
    }

    /**
     * Selects the source crop rectangle to be drawn.
     *
     * - SAFE_FIT: With padding, returns [fullSourceRect] so 100% of source pixels are preserved.
     * - BALANCED: When padding is requested, shrinks crop to [fullSourceRect] (or partial bounds)
     *   so that background padding is actually exposed, rather than fitting a same-aspect crop
     *   that fills 100% of the canvas.
     * - FILL: Always returns [standardCropRect].
     */
    fun selectCropRect(
        cropMode: CropMode,
        standardCropRect: CropRect,
        fullSourceRect: CropRect,
        usePadding: Boolean
    ): CropRect {
        return if (usePadding) {
            when (cropMode) {
                CropMode.SAFE_FIT -> fullSourceRect
                CropMode.BALANCED -> fullSourceRect
                CropMode.FILL -> standardCropRect
            }
        } else {
            standardCropRect
        }
    }
}

