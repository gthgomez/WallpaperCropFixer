package com.wallpapercropfixer.rendering

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.roundToInt

class BlurBackgroundRenderer @Inject constructor() {

    companion object {
        const val MAX_WORK_BUFFER_EDGE = 512
        const val DEFAULT_STRENGTH = 1.0f
        const val DEFAULT_DIM_ALPHA = 0x26 // ~15% dimming
    }

    /**
     * Creates a full-canvas blurred background from [source] and draws it onto [canvas].
     *
     * Pipeline:
     * 1. Aspect-preserving cover crop: compute uniform scale max(canvasW/srcW, canvasH/srcH).
     * 2. Sample/scale into a bounded work buffer (longest edge <= 512px).
     * 3. Apply a deterministic CPU three-pass box blur (fast, O(N) approximation of Gaussian).
     * 4. Draw to [canvas] scaled to (canvasWidth x canvasHeight) with bilinear filtering.
     * 5. Apply subtle dimming overlay.
     * 6. Clean up temporary scratch buffer; never recycle caller's [source].
     */
    fun renderBackground(
        canvas: Canvas,
        source: Bitmap,
        canvasWidth: Int,
        canvasHeight: Int,
        strength: Float = DEFAULT_STRENGTH,
        dimAlpha: Int = DEFAULT_DIM_ALPHA
    ) {
        if (source.width <= 0 || source.height <= 0 || canvasWidth <= 0 || canvasHeight <= 0) {
            return
        }

        // 1. Aspect-preserving cover geometry
        val srcW = source.width.toFloat()
        val srcH = source.height.toFloat()
        val cW = canvasWidth.toFloat()
        val cH = canvasHeight.toFloat()

        val coverScale = max(cW / srcW, cH / srcH)

        // Center the crop window in source coordinates
        val cropSrcW = cW / coverScale
        val cropSrcH = cH / coverScale
        val cropLeft = ((srcW - cropSrcW) / 2f).coerceAtLeast(0f)
        val cropTop = ((srcH - cropSrcH) / 2f).coerceAtLeast(0f)

        val srcRect = Rect(
            cropLeft.toInt(),
            cropTop.toInt(),
            (cropLeft + cropSrcW).toInt().coerceAtMost(source.width),
            (cropTop + cropSrcH).toInt().coerceAtMost(source.height)
        )

        // 2. Bounded working buffer (aspect matches canvas exactly)
        val canvasAspect = cW / cH
        val workW: Int
        val workH: Int
        if (canvasAspect >= 1f) {
            workW = MAX_WORK_BUFFER_EDGE
            workH = (MAX_WORK_BUFFER_EDGE / canvasAspect).roundToInt().coerceAtLeast(1)
        } else {
            workH = MAX_WORK_BUFFER_EDGE
            workW = (MAX_WORK_BUFFER_EDGE * canvasAspect).roundToInt().coerceAtLeast(1)
        }

        val workBitmap = Bitmap.createBitmap(workW, workH, Bitmap.Config.ARGB_8888)
        try {
            val workCanvas = Canvas(workBitmap)
            val dstRect = Rect(0, 0, workW, workH)
            val samplePaint = Paint(Paint.FILTER_BITMAP_FLAG)
            workCanvas.drawBitmap(source, srcRect, dstRect, samplePaint)

            // 3. Fast 3-pass sliding-window box blur
            val minDim = kotlin.math.min(workW, workH)
            val rawRadius = (strength * minDim * 0.05f).roundToInt()
            val radius = rawRadius.coerceIn(1, minDim / 4)

            applyThreePassBoxBlur(workBitmap, radius)

            // 4. Draw blurred working bitmap to target canvas
            val drawPaint = Paint(Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(
                workBitmap,
                null,
                RectF(0f, 0f, cW, cH),
                drawPaint
            )

            // 5. Dimming overlay for subject contrast
            if (dimAlpha > 0) {
                val dimPaint = Paint().apply {
                    color = (dimAlpha shl 24) and 0xFF000000.toInt()
                }
                canvas.drawRect(0f, 0f, cW, cH, dimPaint)
            }
        } finally {
            // Strictly clean up own intermediate buffer; never caller's source
            workBitmap.recycle()
        }
    }

    /**
     * Performs a 3-pass horizontal and vertical box blur in-place on [bitmap].
     * Three successive box blurs approximate a Gaussian convolution via the Central Limit Theorem.
     */
    fun applyThreePassBoxBlur(bitmap: Bitmap, radius: Int) {
        if (radius < 1) return
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val temp = IntArray(w * h)

        // 3 iterations of horizontal + vertical 1D sliding box blur
        for (pass in 0 until 3) {
            boxBlurHorizontal(pixels, temp, w, h, radius)
            boxBlurVertical(temp, pixels, w, h, radius)
        }

        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
    }

    private fun boxBlurHorizontal(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int) {
        val div = 2 * r + 1
        for (y in 0 until h) {
            val rowOffset = y * w
            var rSum = 0
            var gSum = 0
            var bSum = 0

            for (i in -r..r) {
                val clampedX = i.coerceIn(0, w - 1)
                val px = src[rowOffset + clampedX]
                rSum += (px shr 16) and 0xFF
                gSum += (px shr 8) and 0xFF
                bSum += px and 0xFF
            }

            for (x in 0 until w) {
                val rAvg = rSum / div
                val gAvg = gSum / div
                val bAvg = bSum / div
                dst[rowOffset + x] = (0xFF shl 24) or (rAvg shl 16) or (gAvg shl 8) or bAvg

                val leftX = (x - r).coerceIn(0, w - 1)
                val rightX = (x + r + 1).coerceIn(0, w - 1)
                val leftPx = src[rowOffset + leftX]
                val rightPx = src[rowOffset + rightX]

                rSum += ((rightPx shr 16) and 0xFF) - ((leftPx shr 16) and 0xFF)
                gSum += ((rightPx shr 8) and 0xFF) - ((leftPx shr 8) and 0xFF)
                bSum += (rightPx and 0xFF) - (leftPx and 0xFF)
            }
        }
    }

    private fun boxBlurVertical(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int) {
        val div = 2 * r + 1
        for (x in 0 until w) {
            var rSum = 0
            var gSum = 0
            var bSum = 0

            for (i in -r..r) {
                val clampedY = i.coerceIn(0, h - 1)
                val px = src[clampedY * w + x]
                rSum += (px shr 16) and 0xFF
                gSum += (px shr 8) and 0xFF
                bSum += px and 0xFF
            }

            for (y in 0 until h) {
                val rAvg = rSum / div
                val gAvg = gSum / div
                val bAvg = bSum / div
                dst[y * w + x] = (0xFF shl 24) or (rAvg shl 16) or (gAvg shl 8) or bAvg

                val topY = (y - r).coerceIn(0, h - 1)
                val bottomY = (y + r + 1).coerceIn(0, h - 1)
                val topPx = src[topY * w + x]
                val bottomPx = src[bottomY * w + x]

                rSum += ((bottomPx shr 16) and 0xFF) - ((topPx shr 16) and 0xFF)
                gSum += ((bottomPx shr 8) and 0xFF) - ((topPx shr 8) and 0xFF)
                bSum += (bottomPx and 0xFF) - (topPx and 0xFF)
            }
        }
    }
}