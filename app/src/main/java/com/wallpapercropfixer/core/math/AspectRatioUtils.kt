package com.wallpapercropfixer.core.math

object AspectRatioUtils {

    fun ratio(width: Int, height: Int): Float = width.toFloat() / height.toFloat()

    /** True when source needs left/right cropping to match target ratio. */
    fun sourceIsWider(sourceWidth: Int, sourceHeight: Int, targetRatio: Float): Boolean =
        ratio(sourceWidth, sourceHeight) > targetRatio
}
