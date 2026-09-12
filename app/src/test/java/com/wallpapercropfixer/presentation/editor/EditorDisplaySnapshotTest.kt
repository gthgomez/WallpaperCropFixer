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
import com.wallpapercropfixer.rendering.WallpaperBitmapRenderer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Contract tests for the display-snapshot model:
 *
 *  1. What is displayed is a complete RenderedPreview tuple (request + plan +
 *     bitmap); the Home/Lock viewing tab selects within a publication only.
 *  2. A LOCK-only render lives in the primary slot and is displayed correctly.
 *  3. Option changes retain the last display but withdraw Apply/Save eligibility;
 *     a failed render keeps the display, offers retry, and never restores
 *     eligibility. Only a current publication is applicable.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EditorDisplaySnapshotTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun waitForCondition(timeoutMs: Long = 10_000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!condition()) {
            if (System.currentTimeMillis() > deadline) {
                throw AssertionError("Timed out waiting for condition")
            }
            Thread.yield()
        }
    }

    // ---- state-level selection ----

    private val meta = SourceImageMeta("file:///p", 100, 100, "image/jpeg")
    private val device = DeviceProfile("m", "m", 35, 1080, 2400, 2f, 0.45f)
    private val behavior = WallpaperBehaviorProfile("g", "G", 1.0f, 1.0f)

    private fun request(target: WallpaperTarget) = WallpaperRenderRequest(
        source = meta,
        deviceProfile = device,
        behaviorProfile = behavior,
        target = target,
        cropMode = CropMode.BALANCED,
        backgroundFillMode = BackgroundFillMode.BLUR,
        manualFocusPoint = null,
        enableFaceAwareFocus = false
    )

    private fun plan(target: WallpaperTarget) = WallpaperRenderPlan(
        sourceCropRect = CropRect(0f, 0f, 100f, 100f),
        targetCanvasSpec = TargetCanvasSpec(1080, 2400, target),
        outputImagePlacement = CropRect(0f, 0f, 1080f, 2400f),
        usePadding = false,
        backgroundFillMode = BackgroundFillMode.BLUR,
        finalFocusPoint = null
    )

    private fun render(target: WallpaperTarget, marker: Int) = RenderedPreview(
        request = request(target),
        plan = plan(target),
        bitmap = Bitmap.createBitmap(marker, 10, Bitmap.Config.ARGB_8888)
    )

    @Test
    fun `HOME-only publication displays the primary slot`() {
        val home = render(WallpaperTarget.HOME, marker = 1)
        val state = EditorUiState(
            wallpaperTarget = WallpaperTarget.HOME,
            publishedPreview = PublishedPreview(1, WallpaperTarget.HOME, home = home)
        )
        assertEquals(home, state.displayedPreview)
        assertTrue(state.isPreviewCurrent)
        assertEquals(home.bitmap, state.previewBitmap)
    }

    @Test
    fun `LOCK-only render lives in the primary slot and is what is displayed`() {
        val lockOnly = render(WallpaperTarget.LOCK, marker = 2)
        val state = EditorUiState(
            wallpaperTarget = WallpaperTarget.LOCK,
            publishedPreview = PublishedPreview(1, WallpaperTarget.LOCK, home = lockOnly)
        )
        assertEquals(
            "the primary slot doubles as the LOCK-only render; it must be displayed",
            lockOnly,
            state.displayedPreview
        )
        assertEquals(lockOnly.bitmap, state.previewBitmap)
    }

    @Test
    fun `BOTH publication viewing tab selects between the two renders`() {
        val home = render(WallpaperTarget.HOME, marker = 1)
        val lock = render(WallpaperTarget.LOCK, marker = 2)
        val publication = PublishedPreview(1, WallpaperTarget.BOTH, home = home, lock = lock)

        val homeView = EditorUiState(
            wallpaperTarget = WallpaperTarget.BOTH,
            previewingLock = false,
            publishedPreview = publication
        )
        val lockView = homeView.copy(previewingLock = true)

        assertEquals(home, homeView.displayedPreview)
        assertEquals(lock, lockView.displayedPreview)
    }

    @Test
    fun `BOTH with a missing lock render falls back to the primary slot`() {
        val home = render(WallpaperTarget.HOME, marker = 1)
        val state = EditorUiState(
            wallpaperTarget = WallpaperTarget.BOTH,
            previewingLock = true,
            publishedPreview = PublishedPreview(1, WallpaperTarget.BOTH, home = home, lock = null)
        )
        assertEquals(home, state.displayedPreview)
    }

    @Test
    fun `retained snapshot stays visible but is not eligible`() {
        val home = render(WallpaperTarget.HOME, marker = 1)
        val state = EditorUiState(
            wallpaperTarget = WallpaperTarget.HOME,
            retainedPreview = PublishedPreview(1, WallpaperTarget.HOME, home = home)
        )
        assertEquals(home, state.displayedPreview)
        assertEquals(home, state.latestPublication?.home)
        assertFalse("a retained snapshot must not re-enable Apply/Save", state.isPreviewCurrent)
        assertNull(state.previewBitmap)
    }

    // ---- ViewModel-level contract ----

    @Test
    fun `option change retains the display but withdraws eligibility synchronously`() {
        // Gate the FILL render so a fast publication cannot land between
        // setCropMode() and the synchronous eligibility assertions below.
        val renderer = FakeWallpaperBitmapRenderer()
        val vm = buildEditorViewModel(renderer = renderer)
        vm.loadImage("file:///photo")
        waitForCondition { vm.uiState.value.isPreviewCurrent && !vm.uiState.value.isBusy }
        val shown = vm.uiState.value.activeBitmap

        renderer.gates[CropMode.FILL] = CompletableDeferred()
        vm.setCropMode(CropMode.FILL)

        val state = vm.uiState.value
        assertEquals("previous render must stay on screen", shown, state.activeBitmap)
        assertNotNull(state.retainedPreview)
        assertFalse("eligibility must be withdrawn immediately", state.isPreviewCurrent)
        assertNull(state.previewBitmap)
        assertTrue(state.isRendering)

        // Release the gated render: a normal publication restores eligibility.
        renderer.gates[CropMode.FILL]?.complete(Unit)
        waitForCondition { vm.uiState.value.isPreviewCurrent && !vm.uiState.value.isBusy }
    }

    @Test
    fun `failed render keeps the display, blocks apply and save, and retry restores eligibility`() {
        val renderer = FlakyRenderer()
        val applyRepo = FakeApplyRepository()
        val exportRepo = FakeExportRepository()
        val vm = buildEditorViewModel(
            renderer = renderer,
            applyRepository = applyRepo,
            exportRepository = exportRepo
        )
        vm.loadImage("file:///photo")
        waitForCondition { vm.uiState.value.isPreviewCurrent && !vm.uiState.value.isBusy }
        val goodBitmap = vm.uiState.value.activeBitmap

        // Next render fails.
        renderer.failuresRemaining = 1
        vm.setCropMode(CropMode.FILL)
        waitForCondition { vm.uiState.value.renderFailed && !vm.uiState.value.isRendering }

        val failedState = vm.uiState.value
        assertEquals("failed update keeps the previous picture visible", goodBitmap, failedState.activeBitmap)
        assertFalse(failedState.isPreviewCurrent)
        assertNull(failedState.previewBitmap)

        // Apply and Save must be no-ops while the display is stale.
        vm.applyWallpaper()
        vm.exportWallpaper()
        waitForCondition { !vm.uiState.value.isApplying && !vm.uiState.value.isExporting }
        assertTrue("stale display must not be applicable", applyRepo.applied.isEmpty())
        assertTrue("stale display must not be exportable", exportRepo.exported.isEmpty())

        // Retry succeeds -> a current publication restores eligibility.
        vm.retryRender()
        waitForCondition { vm.uiState.value.isPreviewCurrent && !vm.uiState.value.isBusy }
        assertFalse("a successful retry clears the failure state", vm.uiState.value.renderFailed)
        assertNotNull(vm.uiState.value.previewBitmap)
        assertNotEquals(goodBitmap, vm.uiState.value.previewBitmap)
    }

    @Test
    fun `viewing tab survives option edits and publish, resets on image and target changes`() {
        val vm = buildEditorViewModel()
        vm.loadImage("file:///photo")
        waitForCondition { vm.uiState.value.isPreviewCurrent && !vm.uiState.value.isBusy }

        vm.setWallpaperTarget(WallpaperTarget.BOTH)
        waitForCondition { vm.uiState.value.lockPreviewBitmap != null && !vm.uiState.value.isBusy }

        vm.setViewingLock(true)
        vm.setCropMode(CropMode.SAFE_FIT)
        waitForCondition { vm.uiState.value.isPreviewCurrent && !vm.uiState.value.isBusy }
        assertTrue(
            "same-image option edits must not kick the user back to HOME",
            vm.uiState.value.previewingLock
        )
        assertEquals(
            "lock view must display the lock render of the new publication",
            vm.uiState.value.latestPublication?.lock?.bitmap,
            vm.uiState.value.activeBitmap
        )

        vm.setWallpaperTarget(WallpaperTarget.HOME)
        waitForCondition { vm.uiState.value.isPreviewCurrent && !vm.uiState.value.isBusy }
        assertFalse(
            "leaving BOTH makes the lock view unavailable",
            vm.uiState.value.previewingLock
        )

        vm.setWallpaperTarget(WallpaperTarget.BOTH)
        waitForCondition { vm.uiState.value.lockPreviewBitmap != null && !vm.uiState.value.isBusy }
        vm.setViewingLock(true)
        vm.loadImage("file:///photo2")
        waitForCondition { vm.uiState.value.isPreviewCurrent && !vm.uiState.value.isBusy }
        assertFalse("a new photo starts on the HOME view", vm.uiState.value.previewingLock)
    }

    @Test
    fun `viewing tab switch never triggers a render`() {
        val renderer = FakeWallpaperBitmapRenderer()
        val vm = buildEditorViewModel(renderer = renderer)
        vm.loadImage("file:///photo")
        waitForCondition { vm.uiState.value.isPreviewCurrent && !vm.uiState.value.isBusy }
        val baseline = renderer.renderCalls

        vm.setViewingLock(true)
        vm.setViewingLock(false)
        Thread.sleep(100)

        assertEquals(
            "preview-only switching is a pure view change",
            baseline,
            renderer.renderCalls
        )
    }

    @Test
    fun `LOCK-only apply uses the primary-slot bitmap with the LOCK target`() {
        val applyRepo = FakeApplyRepository()
        val vm = buildEditorViewModel(applyRepository = applyRepo)
        vm.loadImage("file:///photo")
        waitForCondition { vm.uiState.value.isPreviewCurrent && !vm.uiState.value.isBusy }

        vm.setWallpaperTarget(WallpaperTarget.LOCK)
        waitForCondition { vm.uiState.value.isPreviewCurrent && !vm.uiState.value.isBusy }
        val lockRender = vm.uiState.value.previewBitmap

        vm.applyWallpaper()
        waitForCondition { !vm.uiState.value.isBusy }

        assertEquals(1, applyRepo.applied.size)
        assertEquals(WallpaperTarget.LOCK, applyRepo.applied[0].second)
        assertEquals(lockRender, applyRepo.applied[0].first)
    }

    @Test
    fun `replacing the photo cannot inherit the previous display`() {
        val vm = buildEditorViewModel()
        vm.loadImage("file:///photoA")
        waitForCondition { vm.uiState.value.isPreviewCurrent && !vm.uiState.value.isBusy }
        val bitmapA = vm.uiState.value.activeBitmap
        assertNotNull(bitmapA)

        vm.loadImage("file:///photoB")
        waitForCondition { vm.uiState.value.isPreviewCurrent && !vm.uiState.value.isBusy }

        assertEquals(
            "a new image starts clean: retained == published, no stale display",
            vm.uiState.value.publishedPreview,
            vm.uiState.value.retainedPreview
        )
        assertNotEquals("the new photo's render must replace the old bitmap", bitmapA, vm.uiState.value.activeBitmap)
    }

    /** Succeeds by default; throws for the next [failuresRemaining] renders. */
    private class FlakyRenderer : WallpaperBitmapRenderer {
        var failuresRemaining = 0
        override suspend fun render(
            request: WallpaperRenderRequest,
            plan: WallpaperRenderPlan
        ): Bitmap {
            if (failuresRemaining > 0) {
                failuresRemaining--
                throw IllegalStateException("planned render failure")
            }
            return Bitmap.createBitmap(request.cropMode.ordinal + 1, 10, Bitmap.Config.ARGB_8888)
        }
    }
}
