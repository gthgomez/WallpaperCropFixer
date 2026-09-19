package com.wallpapercropfixer.domain.engine

import com.wallpapercropfixer.core.math.CropMath
import com.wallpapercropfixer.domain.model.FaceBounds
import com.wallpapercropfixer.domain.model.SubjectAnalysis
import com.wallpapercropfixer.domain.model.BackgroundFillMode
import com.wallpapercropfixer.domain.model.CropMode
import com.wallpapercropfixer.domain.model.DeviceProfile
import com.wallpapercropfixer.domain.model.FocusPoint
import com.wallpapercropfixer.domain.model.SourceImageMeta
import com.wallpapercropfixer.domain.model.WallpaperBehaviorProfile
import com.wallpapercropfixer.domain.model.WallpaperRenderPlan
import com.wallpapercropfixer.domain.model.WallpaperRenderRequest
import com.wallpapercropfixer.domain.model.WallpaperTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Whole-photo Safe Fit, bounded Balanced cropping, unrestricted Fill. */
class CropModeGeometryContractTest {

    private val engine = WallpaperCropEngineImpl(
        canvasSpecFactory = TargetCanvasSpecFactory(),
        focusPointResolver = FocusPointResolver(),
        strategySelector = CropStrategySelector()
    )

    // Multiplier 1.0 so the canvas is exactly the device screen size.
    private val behavior = WallpaperBehaviorProfile(
        profileId = "test",
        brand = "Test",
        homeWidthMultiplier = 1.0f,
        lockWidthMultiplier = 1.0f
    )

    // 1000x2000 screen -> 1:2 canvas.
    private val device = DeviceProfile(
        manufacturer = "test",
        model = "device",
        androidVersion = 35,
        screenWidthPx = 1000,
        screenHeightPx = 2000,
        density = 2f,
        aspectRatio = 0.5f
    )

    private fun request(sourceW: Int, sourceH: Int, mode: CropMode) = WallpaperRenderRequest(
        source = SourceImageMeta("file:///test", sourceW, sourceH, "image/jpeg"),
        deviceProfile = device,
        behaviorProfile = behavior,
        target = WallpaperTarget.HOME,
        cropMode = mode,
        backgroundFillMode = BackgroundFillMode.BLUR,
        manualFocusPoint = null,
        enableFaceAwareFocus = false
    )

    private fun placementCoverage(plan: WallpaperRenderPlan): Float {
        val canvasW = plan.targetCanvasSpec.widthPx.toFloat()
        val canvasH = plan.targetCanvasSpec.heightPx.toFloat()
        val p = plan.outputImagePlacement
        return (p.width * p.height) / (canvasW * canvasH)
    }

    @Test
    fun `SAFE_FIT preserves the full photo and pads when aspect ratio differs`() {
        // 950x2000 into a 1:2 (1000x2000) canvas: aspect differs slightly.
        // Under the strict Fit contract, SAFE_FIT preserves the full 950x2000 photo and pads.
        val plan = engine.buildPlan(request(950, 2000, CropMode.SAFE_FIT), null)

        assertTrue("SAFE_FIT must pad when aspect ratio differs to guarantee no cuts", plan.usePadding)
        assertEquals(950f, plan.sourceCropRect.width, 0.5f)
        assertEquals(2000f, plan.sourceCropRect.height, 0.5f)
        assertTrue("padded placement must leave background exposed", placementCoverage(plan) < 1f)
    }

    @Test
    fun `SAFE_FIT preserves all source area for large aspect differences`() {
        // 2000x2000 into a 1:2 canvas: crop to 1000x2000 removes 50% -> full photo + padding.
        val plan = engine.buildPlan(request(2000, 2000, CropMode.SAFE_FIT), null)

        assertTrue(plan.usePadding)
        assertEquals(2000f, plan.sourceCropRect.width, 0.5f)
        assertEquals(2000f, plan.sourceCropRect.height, 0.5f)
        assertTrue("padded placement must leave background exposed", placementCoverage(plan) < 1f)
    }

    @Test
    fun `BALANCED pads and exposes background when removal exceeds crop budget`() {
        // 4000x3000 into a 1:2 canvas: standard crop is 1500x3000 (1:2), removal ~62.5%
        // is above BALANCED's 35% threshold. BALANCED must pad and actually expose background.
        val plan = engine.buildPlan(request(4000, 3000, CropMode.BALANCED), null)

        assertTrue("BALANCED must request padding above crop budget", plan.usePadding)
        assertTrue(
            "BALANCED padding must actually leave background exposed (< 100% canvas coverage)",
            placementCoverage(plan) < 1f
        )
    }

    @Test
    fun `FILL never pads`() {
        val plan = engine.buildPlan(request(2000, 2000, CropMode.FILL), null)
        assertFalse(plan.usePadding)
        assertEquals(1f, placementCoverage(plan), 0.0001f)
    }

    @Test
    fun `plan carries the resolved focus actually used for the crop`() {
        val focus = FocusPoint(0.25f, 0.75f)
        val plan = engine.buildPlan(
            request(2000, 2000, CropMode.SAFE_FIT).copy(manualFocusPoint = focus),
            null
        )
        assertEquals(focus, plan.finalFocusPoint)
        // A left-biased focus must bias the crop rect toward the left half.
        assertTrue("crop must be biased toward the manual focus", plan.sourceCropRect.left < 500f)
    }

    @Test
    fun `Balanced stays continuous across 349 350 and 351 per mille in both orientations`() {
        for (landscape in listOf(false, true)) {
            for (removed in listOf(349, 350, 351)) {
                val sourceW = if (landscape) 2000 else 1000
                val sourceH = if (landscape) 1000 else 2000
                val targetW = if (landscape) sourceW else 1000 - removed
                val targetH = if (landscape) 1000 - removed else sourceH
                val req = request(sourceW, sourceH, CropMode.BALANCED).copy(
                    deviceProfile = device.copy(screenWidthPx = targetW, screenHeightPx = targetH),
                    manualFocusPoint = FocusPoint(0.1f, 0.9f)
                )
                val plan = engine.buildPlan(req, null)
                val removal = CropMath.cropRemovalFraction(sourceW, sourceH, plan.sourceCropRect)
                assertEquals(minOf(removed / 1000f, 0.35f), removal, 0.00001f)
                assertTrue(removal <= 0.35f + 0.00001f)
                assertEquals(removed > 350, plan.usePadding)
                if (landscape) assertEquals(sourceH.toFloat(), plan.sourceCropRect.bottom, 0.001f)
                else assertEquals(0f, plan.sourceCropRect.left, 0.001f)
            }
        }
    }

    @Test
    fun `Balanced expands toward edge faces without exceeding removal budget`() {
        for ((w, h) in listOf(4000 to 3000, 2000 to 4000)) {
            val faces = listOf(FaceBounds(0f, 0f, 100f, 100f),
                FaceBounds(w - 100f, h - 100f, w.toFloat(), h.toFloat()))
            val plan = engine.buildPlan(
                request(w, h, CropMode.BALANCED).copy(enableFaceAwareFocus = true,
                    manualFocusPoint = FocusPoint(0.2f, 0.8f)),
                SubjectAnalysis(faces, null)
            )
            for (face in faces) {
                assertTrue(plan.sourceCropRect.left <= face.left)
                assertTrue(plan.sourceCropRect.top <= face.top)
                assertTrue(plan.sourceCropRect.right >= face.right)
                assertTrue(plan.sourceCropRect.bottom >= face.bottom)
            }
            assertTrue(CropMath.cropRemovalFraction(w, h, plan.sourceCropRect) <= 0.35001f)
        }
    }
    @Test
    fun `Balanced face expansion keeps a partial crop when whole photo is unnecessary`() {
        val face = FaceBounds(3500f, 1000f, 3800f, 1500f)
        val plan = engine.buildPlan(
            request(4000, 3000, CropMode.BALANCED).copy(enableFaceAwareFocus = true,
                manualFocusPoint = FocusPoint(0.5f, 0.5f)),
            SubjectAnalysis(listOf(face), null)
        )
        assertTrue(plan.sourceCropRect.right >= face.right)
        assertTrue(plan.sourceCropRect.width < 4000f)
        assertTrue(CropMath.cropRemovalFraction(4000, 3000, plan.sourceCropRect) <= 0.35001f)
        assertTrue(plan.usePadding)
    }

    @Test
    fun `Safe Fit preserves photo even for sub-per-mille aspect differences`() {
        val plan = engine.buildPlan(request(1001, 2000, CropMode.SAFE_FIT), null)
        assertEquals(1001f, plan.sourceCropRect.width, 0f)
        assertEquals(2000f, plan.sourceCropRect.height, 0f)
        assertTrue(plan.usePadding)
    }
    @Test
    fun `Balanced budget and source bounds hold across varied sources focus and canvases`() {
        val random = kotlin.random.Random(352026)
        repeat(500) {
            val w = random.nextInt(100, 30000)
            val h = random.nextInt(100, 30000)
            val plan = engine.buildPlan(request(w, h, CropMode.BALANCED).copy(
                deviceProfile = device.copy(screenWidthPx = random.nextInt(100, 3000),
                    screenHeightPx = random.nextInt(100, 4000)),
                manualFocusPoint = FocusPoint(random.nextFloat(), random.nextFloat())
            ), null)
            val crop = plan.sourceCropRect
            assertTrue(CropMath.cropRemovalFraction(w, h, crop) <= 0.35001f)
            assertTrue(crop.left >= 0f && crop.top >= 0f)
            assertTrue(crop.right <= w + 0.01f && crop.bottom <= h + 0.01f)
            assertTrue(crop.width > 0f && crop.height > 0f)
        }
    }
}
