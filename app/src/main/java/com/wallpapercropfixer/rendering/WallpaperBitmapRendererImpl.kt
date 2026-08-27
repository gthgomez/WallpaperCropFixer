package com.wallpapercropfixer.rendering

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import com.wallpapercropfixer.domain.model.BackgroundFillMode
import com.wallpapercropfixer.domain.model.CropRect
import com.wallpapercropfixer.domain.model.WallpaperRenderPlan
import com.wallpapercropfixer.domain.model.WallpaperRenderRequest
import com.wallpapercropfixer.domain.repository.ImageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sqrt
import javax.inject.Inject

class WallpaperBitmapRendererImpl @Inject constructor(
    private val imageRepository: ImageRepository,
    private val blurBackgroundRenderer: BlurBackgroundRenderer,
    private val gradientBackgroundRenderer: GradientBackgroundRenderer
) : WallpaperBitmapRenderer {

    override suspend fun render(
        request: WallpaperRenderRequest,
        plan: WallpaperRenderPlan
    ): Bitmap = withContext(Dispatchers.Default) {
        val canvasW = plan.targetCanvasSpec.widthPx
        val canvasH = plan.targetCanvasSpec.heightPx

        // Decode at up to 2x canvas resolution, bounded by an explicit pixel budget so
        // very large source photos never allocate memory proportional to their original
        // megapixel count (a 50-100 MP photo decodes to at most ~14 MP here).
        val (decodeMaxW, decodeMaxH) = decodeMaxDimensions(canvasW, canvasH)
        val sourceBitmap = imageRepository.decodeBitmapSampled(
            uri = request.source.uri,
            maxWidth = decodeMaxW,
            maxHeight = decodeMaxH
        )

        // The sourceCropRect coordinates are in original image pixel space.
        // The decoded bitmap may be smaller than the original (due to inSampleSize).
        // Scale the crop rect to match the decoded bitmap dimensions.
        val scaleX = sourceBitmap.width.toFloat() / request.source.width.toFloat()
        val scaleY = sourceBitmap.height.toFloat() / request.source.height.toFloat()

        val scaledCropRect = CropRect(
            left = plan.sourceCropRect.left * scaleX,
            top = plan.sourceCropRect.top * scaleY,
            right = plan.sourceCropRect.right * scaleX,
            bottom = plan.sourceCropRect.bottom * scaleY
        )

        val output = Bitmap.createBitmap(canvasW, canvasH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // Wallpapers must be visually opaque. Fill the canvas first so any source
        // transparency or background alpha composites as black, matching wallpaper
        // behavior on real devices rather than preview-only compositing.
        canvas.drawColor(Color.BLACK)

        if (plan.usePadding) {
            drawBackground(canvas, sourceBitmap, canvasW, canvasH, plan.backgroundFillMode)
        }

        drawForeground(canvas, sourceBitmap, scaledCropRect, plan.outputImagePlacement)

        sourceBitmap.recycle()
        output
    }

    private fun drawBackground(
        canvas: Canvas,
        source: Bitmap,
        canvasW: Int,
        canvasH: Int,
        fillMode: BackgroundFillMode
    ) {
        when (fillMode) {
            BackgroundFillMode.BLUR -> blurBackgroundRenderer.renderBackground(canvas, source, canvasW, canvasH)
            BackgroundFillMode.GRADIENT -> gradientBackgroundRenderer.renderBackground(canvas, canvasW, canvasH, source)
            BackgroundFillMode.SOLID -> canvas.drawColor(Color.BLACK)
        }
    }

    private fun drawForeground(
        canvas: Canvas,
        source: Bitmap,
        scaledCropRect: CropRect,
        placement: CropRect
    ) {
        val left = scaledCropRect.left.toInt().coerceIn(0, source.width - 1)
        val top = scaledCropRect.top.toInt().coerceIn(0, source.height - 1)
        // Ensure right > left and bottom > top to avoid invalid/inverted Rect
        val right = scaledCropRect.right.toInt().coerceIn(left + 1, source.width)
        val bottom = scaledCropRect.bottom.toInt().coerceIn(top + 1, source.height)
        val srcRect = Rect(left, top, right, bottom)
        val dstRect = RectF(placement.left, placement.top, placement.right, placement.bottom)
        canvas.drawBitmap(source, srcRect, dstRect, Paint(Paint.FILTER_BITMAP_FLAG))
    }

    /**
     * Computes decode dimensions bounded by the canvas requirement, an absolute
     * side limit, and an explicit pixel budget. Exposed for tests.
     */
    internal fun decodeMaxDimensions(canvasW: Int, canvasH: Int): Pair<Int, Int> {
        var maxW = (canvasW * 2f).toInt().coerceIn(1, MAX_DECODE_SIDE)
        var maxH = (canvasH * 2f).toInt().coerceIn(1, MAX_DECODE_SIDE)
        val area = maxW.toLong() * maxH
        if (area > MAX_DECODE_PIXELS) {
            val scale = sqrt(MAX_DECODE_PIXELS.toDouble() / area)
            maxW = (maxW * scale).toInt().coerceAtLeast(1)
            maxH = (maxH * scale).toInt().coerceAtLeast(1)
        }
        return maxW to maxH
    }

    companion object {
        const val MAX_DECODE_PIXELS = 14_000_000L
        const val MAX_DECODE_SIDE = 4096
    }
}