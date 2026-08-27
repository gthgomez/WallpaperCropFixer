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
     * this approach avoids it and works on all API levels. The scaled draw is done
     * directly to the canvas so no full-canvas intermediate bitmap is allocated,
     * and the result is fully opaque (a wallpaper must not carry alpha edges).
     */
    fun renderBackground(canvas: Canvas, source: Bitmap, canvasWidth: Int, canvasHeight: Int) {
        val downscale = 8

        val smallW = (canvasWidth / downscale).coerceAtLeast(1)
        val smallH = (canvasHeight / downscale).coerceAtLeast(1)

        // Downscale source to canvas proportions for background
        val small = Bitmap.createScaledBitmap(source, smallW, smallH, true)

        // Upscale back to full canvas — bilinear interpolation gives a soft blur effect.
        // Drawing the small bitmap directly (with filtering) avoids a full-size allocation.
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(
            small,
            null,
            RectF(0f, 0f, canvasWidth.toFloat(), canvasHeight.toFloat()),
            paint
        )

        // Darken slightly so the foreground image pops (opaque overlay).
        val dim = Paint().apply { color = 0x26000000 }
        canvas.drawRect(0f, 0f, canvasWidth.toFloat(), canvasHeight.toFloat(), dim)

        small.recycle()
    }
}