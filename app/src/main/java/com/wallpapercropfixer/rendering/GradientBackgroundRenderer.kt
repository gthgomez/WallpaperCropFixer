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
        val dominant = if (source != null) sampleDominantEdgeColor(source) else DEFAULT_DARK
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

    /**
     * Average the four corner pixels of [source] to derive a representative dark hue.
     * Corners are sampled at 1/8 inset to avoid edge artifacts from JPEG compression.
     */
    private fun sampleDominantEdgeColor(source: Bitmap): Int {
        val w = source.width
        val h = source.height
        val insetX = (w * 0.125f).toInt().coerceAtLeast(0)
        val insetY = (h * 0.125f).toInt().coerceAtLeast(0)

        val samples = listOf(
            source.getPixel(insetX, insetY),
            source.getPixel(w - 1 - insetX, insetY),
            source.getPixel(insetX, h - 1 - insetY),
            source.getPixel(w - 1 - insetX, h - 1 - insetY)
        )

        val r = samples.map { it.red }.average().toInt()
        val g = samples.map { it.green }.average().toInt()
        val b = samples.map { it.blue }.average().toInt()
        return Color.rgb(r, g, b)
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
