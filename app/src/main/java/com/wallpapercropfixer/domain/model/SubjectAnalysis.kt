package com.wallpapercropfixer.domain.model

/**
 * Result of on-device subject analysis.
 *
 * [faces] are detected face rectangles (ML Kit Face Detection). [subjects] are
 * broader subject rectangles (persons, pets, objects) from ML Kit Subject
 * Segmentation when available. Both are in upright source-image pixel space and
 * may be empty. Framing prefers [subjects] over [faces] because a subject that is
 * not a face still matters for a wallpaper crop.
 */
data class SubjectAnalysis(
    val faces: List<FaceBounds>,
    val suggestedFocusPoint: FocusPoint?,
    val subjects: List<FaceBounds> = emptyList()
)
