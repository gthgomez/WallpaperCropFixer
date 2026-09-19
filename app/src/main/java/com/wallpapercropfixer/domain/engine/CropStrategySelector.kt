package com.wallpapercropfixer.domain.engine

import com.wallpapercropfixer.domain.model.CropMode
import com.wallpapercropfixer.domain.model.CropRect
import com.wallpapercropfixer.domain.model.FaceBounds
import javax.inject.Inject

class CropStrategySelector @Inject constructor() {
    companion object {
        const val BALANCED_MAX_CROP_FRACTION = 0.35f
    }

    fun shouldUsePadding(
        cropMode: CropMode,
        cropRemovalFraction: Float,
        hasClippedFaces: Boolean = false
    ): Boolean = when (cropMode) {
        CropMode.SAFE_FIT -> cropRemovalFraction > 0f || hasClippedFaces
        CropMode.BALANCED -> cropRemovalFraction > BALANCED_MAX_CROP_FRACTION || hasClippedFaces
        CropMode.FILL -> false
    }

    /** Expand the cropped axis only as far as the area budget requires, retaining
     * the focus-biased center where possible. Face inclusion can only add source area. */
    fun selectCropRect(
        cropMode: CropMode,
        standardCropRect: CropRect,
        fullSourceRect: CropRect,
        usePadding: Boolean,
        faces: List<FaceBounds> = emptyList()
    ): CropRect {
        if (cropMode == CropMode.SAFE_FIT) return fullSourceRect
        if (cropMode == CropMode.FILL || !usePadding) return standardCropRect
        val minArea = fullSourceRect.width * fullSourceRect.height * (1f - BALANCED_MAX_CROP_FRACTION)
        val width = if (standardCropRect.height == fullSourceRect.height) {
            maxOf(standardCropRect.width, minArea / fullSourceRect.height)
        } else standardCropRect.width
        val height = if (standardCropRect.width == fullSourceRect.width) {
            maxOf(standardCropRect.height, minArea / fullSourceRect.width)
        } else standardCropRect.height
        val left = ((standardCropRect.left + standardCropRect.right - width) / 2f)
            .coerceIn(fullSourceRect.left, fullSourceRect.right - width)
        val top = ((standardCropRect.top + standardCropRect.bottom - height) / 2f)
            .coerceIn(fullSourceRect.top, fullSourceRect.bottom - height)
        var crop = CropRect(left, top, left + width, top + height)
        for (face in faces) {
            crop = CropRect(
                minOf(crop.left, face.left.coerceIn(fullSourceRect.left, fullSourceRect.right)),
                minOf(crop.top, face.top.coerceIn(fullSourceRect.top, fullSourceRect.bottom)),
                maxOf(crop.right, face.right.coerceIn(fullSourceRect.left, fullSourceRect.right)),
                maxOf(crop.bottom, face.bottom.coerceIn(fullSourceRect.top, fullSourceRect.bottom))
            )
        }
        return crop
    }
}
