package com.wallpapercropfixer.rendering

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import javax.inject.Inject

class BlurBackgroundRenderer @Inject constructor() {

    /**
     * Creates a full-canvas blurred background from [source] and draws it onto [canvas].
     *
     * Uses a downscale + upscale trick for blur — RenderScript is deprecated;
     * this approach avoids it and works on all API levels.
     */
    fun renderBackground(canvas: Canvas, source: Bitmap, canvasWidth: Int, canvasHeight: Int) {
        val blurRadius = 24
        val downscale = 8

        val smallW = (canvasWidth / downscale).coerceAtLeast(1)
        val smallH = (canvasHeight / downscale).coerceAtLeast(1)

        // Downscale source to canvas proportions for background
        val small = Bitmap.createScaledBitmap(source, smallW, smallH, true)

        // Upscale back to full canvas — bilinear interpolation gives a soft blur effect
        val blurred = Bitmap.createScaledBitmap(small, canvasWidth, canvasHeight, true)
        small.recycle()

        // Darken slightly so the foreground image pops
        val paint = Paint().apply { alpha = 220 }
        canvas.drawBitmap(blurred, null, RectF(0f, 0f, canvasWidth.toFloat(), canvasHeight.toFloat()), paint)
        blurred.recycle()
    }
}
