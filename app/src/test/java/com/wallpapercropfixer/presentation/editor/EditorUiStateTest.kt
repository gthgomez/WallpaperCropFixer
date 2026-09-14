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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Plain-JUnit coverage for the displayed-vs-eligible snapshot selection on
 * [EditorUiState] — in particular that the LOCK viewing tab in BOTH mode pairs
 * the lock bitmap with the LOCK plan (HOME canvases are wider, so using the
 * HOME plan misplaces the focus overlay by several percent).
 */
class EditorUiStateTest {

    private val homePlan = testPlan(WallpaperTarget.HOME)
    private val lockPlan = testPlan(WallpaperTarget.LOCK)

    @Test
    fun `displayedPreview uses the home render when viewing home`() {
        val state = stateWithPreview(withLock = true, previewingLock = false)
        assertEquals(homePlan, state.displayedPreview?.plan)
    }

    @Test
    fun `displayedPreview selects the lock render and its plan while previewing lock in BOTH`() {
        val state = stateWithPreview(withLock = true, previewingLock = true)
        assertEquals("the lock tab must pair the lock bitmap with the lock plan",
            lockPlan, state.displayedPreview?.plan)
    }

    @Test
    fun `displayedPreview falls back to the home render when BOTH has no lock render`() {
        val state = stateWithPreview(withLock = false, previewingLock = true)
        assertEquals(homePlan, state.displayedPreview?.plan)
    }

    @Test
    fun `displayedPreview is null without any publication`() {
        assertNull(EditorUiState(previewingLock = true).displayedPreview)
    }

    @Test
    fun `retained preview is displayed but never eligible`() {
        val retained = PublishedPreview(
            revision = 1L,
            target = WallpaperTarget.HOME,
            home = RenderedPreview(testRequest(WallpaperTarget.HOME), homePlan, mockk<Bitmap>(relaxed = true)),
            lock = null
        )
        val state = EditorUiState(publishedPreview = null, retainedPreview = retained)

        assertNotNull("the retained render stays on screen", state.displayedPreview)
        assertEquals(retained.home.bitmap, state.activeBitmap)
        assertNull("eligibility is withdrawn once the publication is gone", state.previewBitmap)
        assertFalse("a retained render must not count as current", state.isPreviewCurrent)
    }

    @Test
    fun `latestPublication prefers the current publication over the retained one`() {
        val retained = PublishedPreview(
            revision = 1L,
            target = WallpaperTarget.HOME,
            home = RenderedPreview(testRequest(WallpaperTarget.HOME), homePlan, mockk<Bitmap>(relaxed = true)),
            lock = null
        )
        val published = retained.copy(revision = 2L)
        val state = EditorUiState(publishedPreview = published, retainedPreview = retained)

        assertEquals(2L, state.latestPublication?.revision)
    }

    private fun stateWithPreview(withLock: Boolean, previewingLock: Boolean): EditorUiState {
        val preview = PublishedPreview(
            revision = 1L,
            target = if (withLock) WallpaperTarget.BOTH else WallpaperTarget.HOME,
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
