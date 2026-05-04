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

        // Decode at up to 2× canvas resolution — large enough for quality, small enough for memory
        val sourceBitmap = imageRepository.decodeBitmapSampled(
            uri = request.source.uri,
            maxWidth = canvasW * 2,
            maxHeight = canvasH * 2
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
}
