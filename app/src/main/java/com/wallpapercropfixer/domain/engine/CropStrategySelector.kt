package com.wallpapercropfixer.domain.engine

import com.wallpapercropfixer.core.math.CropMath
import com.wallpapercropfixer.domain.model.CropMode
import com.wallpapercropfixer.domain.model.CropRect
import javax.inject.Inject

class CropStrategySelector @Inject constructor() {

    /**
     * Determines whether the given [cropRemovalFraction] should trigger padding
     * for the specified [cropMode].
     *
     * SAFE_FIT: pad when removal > threshold
     * BALANCED: pad when removal > 2× threshold
     * FILL: never pad
     */
    fun shouldUsePadding(cropMode: CropMode, cropRemovalFraction: Float): Boolean {
        return when (cropMode) {
            CropMode.SAFE_FIT -> cropRemovalFraction > CropMath.SAFE_FIT_PADDING_THRESHOLD
            CropMode.BALANCED -> cropRemovalFraction > CropMath.SAFE_FIT_PADDING_THRESHOLD * 2f
            CropMode.FILL -> false
        }
    }

    /**
     * For SAFE_FIT with padding, we still want a minimal crop to remove
     * extreme black bars if the source is very different in ratio.
     * For FILL, we allow the full crop rect computed from focus bias.
     * For BALANCED, we use the standard crop rect.
     */
    fun selectCropRect(
        cropMode: CropMode,
        standardCropRect: CropRect,
        usePadding: Boolean
    ): CropRect {
        // When padding is used, the image is shown letterboxed — use the full source crop
        // (which in padding mode means we use the entire source, not a sub-rect).
        return if (usePadding && cropMode == CropMode.SAFE_FIT) {
            // Full source — no crop at all; renderer will fit-and-pad
            standardCropRect
        } else {
            standardCropRect
        }
    }
}
