package com.wallpapercropfixer.domain.engine

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

/**
 * Pins the crop-mode geometry contract that the UI copy depends on.
 *
 * These tests document (and guard against drift of) the real behavior:
 *  - SAFE_FIT only pads when a standard crop would remove > 20% of the source
 *    area or clip a detected face. Light crops still crop.
 *  - BALANCED pads only above 40% removal, but pads with the *standard* crop
 *    rect. When that rect already matches the canvas aspect, the padded
 *    placement fills the whole canvas and no background is visible, even
 *    though [WallpaperRenderPlan.usePadding] is true.
 *  - FILL never pads.
 *
 * UI copy must not promise "no cropping" for SAFE_FIT, and the background
 * finish control is only meaningful when the plan actually exposes padding.
 */
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
    fun `SAFE_FIT pads only past the removal threshold`() {
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
}
