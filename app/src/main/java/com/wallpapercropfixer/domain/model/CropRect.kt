package com.wallpapercropfixer.domain.model

data class CropRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = (right - left).coerceAtLeast(0f)
    val height: Float get() = (bottom - top).coerceAtLeast(0f)

    fun contains(other: CropRect): Boolean {
        return left <= other.left && top <= other.top && right >= other.right && bottom >= other.bottom
    }
}
