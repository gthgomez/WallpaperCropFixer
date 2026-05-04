package com.wallpapercropfixer.domain.engine

import com.wallpapercropfixer.core.math.CropMath
import com.wallpapercropfixer.domain.model.FaceBounds
import com.wallpapercropfixer.domain.model.FocusPoint
import com.wallpapercropfixer.domain.model.SubjectAnalysis
import javax.inject.Inject

class FocusPointResolver @Inject constructor() {

    /**
     * Priority: manual > face cluster center > image center.
     *
     * All resolved points are clamped to a [SAFE_MARGIN] inset so that subjects at the very
     * edge of the frame are not sheared off by JPEG block artifacts or rounding during crop.
     */
    fun resolve(
        manual: FocusPoint?,
        faceAwareEnabled: Boolean,
        subjectAnalysis: SubjectAnalysis?,
        sourceWidth: Int,
        sourceHeight: Int
    ): FocusPoint {
        if (manual != null) return manual.withSafeMargin()

        if (faceAwareEnabled && subjectAnalysis != null && subjectAnalysis.faces.isNotEmpty()) {
            subjectAnalysis.suggestedFocusPoint?.let { return it.withSafeMargin() }
            return clusterCenter(subjectAnalysis.faces, sourceWidth, sourceHeight).withSafeMargin()
        }

        return CropMath.CENTER_FOCUS
    }

    private fun FocusPoint.withSafeMargin(margin: Float = SAFE_MARGIN) = FocusPoint(
        xNormalized = xNormalized.coerceIn(margin, 1f - margin),
        yNormalized = yNormalized.coerceIn(margin, 1f - margin)
    )

    companion object {
        private const val SAFE_MARGIN = 0.05f
    }

    private fun clusterCenter(faces: List<FaceBounds>, sourceWidth: Int, sourceHeight: Int): FocusPoint {
        val unionLeft = faces.minOf { it.left }
        val unionTop = faces.minOf { it.top }
        val unionRight = faces.maxOf { it.right }
        val unionBottom = faces.maxOf { it.bottom }

        return FocusPoint(
            xNormalized = ((unionLeft + unionRight) / 2f) / sourceWidth,
            yNormalized = ((unionTop + unionBottom) / 2f) / sourceHeight
        )
    }
}
