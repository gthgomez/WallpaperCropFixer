package com.wallpapercropfixer.domain.model

data class FocusPoint(
    val xNormalized: Float,
    val yNormalized: Float
) {
    init {
        require(!xNormalized.isNaN() && !yNormalized.isNaN()) {
            "FocusPoint coordinates must not be NaN"
        }
        require(!xNormalized.isInfinite() && !yNormalized.isInfinite()) {
            "FocusPoint coordinates must be finite"
        }
    }
}
