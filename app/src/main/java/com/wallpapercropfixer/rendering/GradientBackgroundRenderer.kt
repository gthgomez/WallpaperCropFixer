package com.wallpapercropfixer.rendering

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import javax.inject.Inject

class GradientBackgroundRenderer @Inject constructor() {

    fun renderBackground(canvas: Canvas, canvasWidth: Int, canvasHeight: Int, source: Bitmap? = null) {
        val dominant = if (source != null) ImageEdgeColorSampler.sample(source) else DEFAULT_DARK
        val dark = blendWithBlack(dominant, 0.35f)
        val darker = blendWithBlack(dominant, 0.55f)

        val paint = Paint()
        paint.shader = LinearGradient(
            0f, 0f,
            0f, canvasHeight.toFloat(),
            dark,
            darker,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(RectF(0f, 0f, canvasWidth.toFloat(), canvasHeight.toFloat()), paint)
    }

    private fun blendWithBlack(color: Int, blackFraction: Float): Int {
        val keep = 1f - blackFraction
        return Color.rgb(
            (color.red * keep).toInt(),
            (color.green * keep).toInt(),
            (color.blue * keep).toInt()
        )
    }

    companion object {
        private val DEFAULT_DARK = Color.rgb(26, 26, 46)
    }
}
