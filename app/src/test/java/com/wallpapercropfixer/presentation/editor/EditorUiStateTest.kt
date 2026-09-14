package com.wallpapercropfixer.presentation.editor

import android.graphics.Bitmap
import com.wallpapercropfixer.domain.model.BackgroundFillMode
import com.wallpapercropfixer.domain.model.CropMode
import com.wallpapercropfixer.domain.model.CropRect
import com.wallpapercropfixer.domain.model.DeviceProfile
import com.wallpapercropfixer.domain.model.SourceImageMeta
import com.wallpapercropfixer.domain.model.TargetCanvasSpec
import com.wallpapercropfixer.domain.model.WallpaperBehaviorProfile
import com.wallpapercropfixer.domain.model.WallpaperRenderPlan
import com.wallpapercropfixer.domain.model.WallpaperRenderRequest
import com.wallpapercropfixer.domain.model.WallpaperTarget
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Plain-JUnit coverage for the preview/plan selection helpers on [EditorUiState]. */
class EditorUiStateTest {

    private val homePlan = testPlan(WallpaperTarget.HOME)
    private val lockPlan = testPlan(WallpaperTarget.LOCK)

    @Test
    fun `activeRenderPlan returns the home plan when previewing home`() {
        val state = stateWithPreview(withLock = true, previewingLock = false)
        assertEquals(homePlan, state.activeRenderPlan)
    }

    @Test
    fun `activeRenderPlan returns the lock plan while previewing lock`() {
        val state = stateWithPreview(withLock = true, previewingLock = true)
        assertEquals(lockPlan, state.activeRenderPlan)
    }

    @Test
    fun `activeRenderPlan falls back to the home plan when no lock preview exists`() {
        val state = stateWithPreview(withLock = false, previewingLock = true)
        assertEquals(homePlan, state.activeRenderPlan)
    }

    @Test
    fun `activeRenderPlan is null without a published preview`() {
        assertNull(EditorUiState(previewingLock = true).activeRenderPlan)
    }

    private fun stateWithPreview(withLock: Boolean, previewingLock: Boolean): EditorUiState {
        val preview = PublishedPreview(
            revision = 1L,
            target = WallpaperTarget.HOME,
            home = RenderedPreview(testRequest(WallpaperTarget.HOME), homePlan, mockk<Bitmap>(relaxed = true)),
            lock = if (withLock) {
                RenderedPreview(testRequest(WallpaperTarget.LOCK), lockPlan, mockk<Bitmap>(relaxed = true))
            } else {
                null
            }
        )
        return EditorUiState(publishedPreview = preview, previewingLock = previewingLock)
    }

    private fun testRequest(target: WallpaperTarget) = WallpaperRenderRequest(
        source = SourceImageMeta("file:///test.jpg", 4000, 3000, "image/jpeg"),
        deviceProfile = DeviceProfile("test", "phone", 35, 1080, 2400, 2.75f, 1080f / 2400f),
        behaviorProfile = WallpaperBehaviorProfile("generic", "generic", 1f, 1f),
        target = target,
        cropMode = CropMode.BALANCED,
        backgroundFillMode = BackgroundFillMode.BLUR,
        manualFocusPoint = null,
        enableFaceAwareFocus = false
    )

    private fun testPlan(target: WallpaperTarget) = WallpaperRenderPlan(
        sourceCropRect = CropRect(0f, 0f, 100f, 100f),
        targetCanvasSpec = TargetCanvasSpec(1080, 2400, target),
        outputImagePlacement = CropRect(0f, 0f, 1080f, 2400f),
        usePadding = false,
        backgroundFillMode = BackgroundFillMode.BLUR,
        finalFocusPoint = null
    )
}
