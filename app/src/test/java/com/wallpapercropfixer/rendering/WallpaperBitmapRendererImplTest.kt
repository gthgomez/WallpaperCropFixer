package com.wallpapercropfixer.rendering

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.wallpapercropfixer.data.image.AndroidImageRepository
import com.wallpapercropfixer.domain.engine.CropStrategySelector
import com.wallpapercropfixer.domain.engine.FocusPointResolver
import com.wallpapercropfixer.domain.engine.TargetCanvasSpecFactory
import com.wallpapercropfixer.domain.engine.WallpaperCropEngineImpl
import com.wallpapercropfixer.domain.model.BackgroundFillMode
import com.wallpapercropfixer.domain.model.CropMode
import com.wallpapercropfixer.domain.model.DeviceProfile
import com.wallpapercropfixer.domain.model.SourceImageMeta
import com.wallpapercropfixer.domain.model.WallpaperBehaviorProfile
import com.wallpapercropfixer.domain.model.WallpaperRenderRequest
import com.wallpapercropfixer.domain.model.WallpaperTarget
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WallpaperBitmapRendererImplTest {

    private fun writeJpeg(context: Context, width: Int, height: Int, name: String): File {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(Color.rgb(120, 90, 200))
        val paint = android.graphics.Paint().apply { color = Color.WHITE }
        canvas.drawRect(0f, 0f, width * 0.3f, height * 0.3f, paint)
        val file = File(context.cacheDir, name)
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out) }
        bitmap.recycle()
        return file
    }

    private fun buildRenderer(context: Context): WallpaperBitmapRendererImpl =
        WallpaperBitmapRendererImpl(
            AndroidImageRepository(context),
            BlurBackgroundRenderer(),
            GradientBackgroundRenderer()
        )

    @Test
    fun `render produces opaque canvas-sized output for blur padding`() {
        val context = RuntimeEnvironment.getApplication()
        val file = writeJpeg(context, 800, 600, "src_800x600.jpg")
        val imageRepo = AndroidImageRepository(context)
        val renderer = buildRenderer(context)

        val meta = runBlocking { imageRepo.readImageMeta(file.absolutePath) }
        val device = DeviceProfile("test", "phone", 35, 1080, 2400, 2.75f, 1080f / 2400f)
        val behavior = WallpaperBehaviorProfile("generic", "generic", 1.1f, 1.0f)
        val request = WallpaperRenderRequest(
            source = meta,
            deviceProfile = device,
            behaviorProfile = behavior,
            target = WallpaperTarget.HOME,
            cropMode = CropMode.SAFE_FIT,
            backgroundFillMode = BackgroundFillMode.BLUR,
            manualFocusPoint = null,
            enableFaceAwareFocus = true
        )
        val engine = WallpaperCropEngineImpl(TargetCanvasSpecFactory(), FocusPointResolver(), CropStrategySelector())
        val plan = engine.buildPlan(request, null)

        val output = runBlocking { renderer.render(request, plan) }

        assertEquals((1080 * 1.1f).toInt(), output.width)
        assertEquals(2400, output.height)
        // Sample center and corners — all must be opaque.
        val samples = listOf(
            output.getPixel(0, 0),
            output.getPixel(output.width - 1, 0),
            output.getPixel(output.width / 2, output.height / 2),
            output.getPixel(0, output.height - 1),
            output.getPixel(output.width - 1, output.height - 1)
        )
        samples.forEach { px ->
            assertEquals("pixel must be fully opaque: $px", 255, android.graphics.Color.alpha(px))
        }
    }

    @Test
    fun `render handles extreme aspect ratio sources without crashing`() {
        val context = RuntimeEnvironment.getApplication()
        // Extreme panorama
        val file = writeJpeg(context, 3000, 150, "panorama.jpg")
        val imageRepo = AndroidImageRepository(context)
        val renderer = buildRenderer(context)
        val meta = runBlocking { imageRepo.readImageMeta(file.absolutePath) }
        val device = DeviceProfile("test", "phone", 35, 1080, 2400, 2.75f, 1080f / 2400f)
        val behavior = WallpaperBehaviorProfile("generic", "generic", 1.1f, 1.0f)
        val request = WallpaperRenderRequest(meta, device, behavior, WallpaperTarget.HOME, CropMode.SAFE_FIT, BackgroundFillMode.GRADIENT, null, true)
        val engine = WallpaperCropEngineImpl(TargetCanvasSpecFactory(), FocusPointResolver(), CropStrategySelector())
        val plan = engine.buildPlan(request, null)

        val output = runBlocking { renderer.render(request, plan) }
        assertEquals((1080 * 1.1f).toInt(), output.width)
        // Panorama must still yield an opaque, canvas-filling result.
        assertEquals(android.graphics.Color.alpha(output.getPixel(output.width / 2, output.height / 2)), 255)
    }

    @Test
    fun `decode budget bounds canvases for high resolution devices`() {
        val context = RuntimeEnvironment.getApplication()
        val renderer = buildRenderer(context)

        // QHD+ phone with home multiplier → canvas ~1612x3120.
        val (w, h) = renderer.decodeMaxDimensions(1612, 3120)
        assertTrue(w > 0 && h > 0)
        assertTrue(w <= WallpaperBitmapRendererImpl.MAX_DECODE_SIDE)
        assertTrue(h <= WallpaperBitmapRendererImpl.MAX_DECODE_SIDE)
        assertTrue("decoded pixels must respect the budget: $w x $h",
            w.toLong() * h <= WallpaperBitmapRendererImpl.MAX_DECODE_PIXELS)

        // 1080p canvas stays within budget too.
        val (w2, h2) = renderer.decodeMaxDimensions(1188, 2400)
        assertTrue(w2.toLong() * h2 <= WallpaperBitmapRendererImpl.MAX_DECODE_PIXELS)
    }
}