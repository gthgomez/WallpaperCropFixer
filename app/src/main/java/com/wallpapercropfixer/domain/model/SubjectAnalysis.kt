package com.wallpapercropfixer.domain.model

data class SubjectAnalysis(
    val faces: List<FaceBounds>,
    val suggestedFocusPoint: FocusPoint?
)
