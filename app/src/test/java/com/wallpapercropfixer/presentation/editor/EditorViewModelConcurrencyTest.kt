package com.wallpapercropfixer.presentation.editor

import com.wallpapercropfixer.R
import com.wallpapercropfixer.domain.model.CropMode
import com.wallpapercropfixer.domain.model.FaceBounds
import com.wallpapercropfixer.domain.model.FocusPoint
import com.wallpapercropfixer.domain.model.SubjectAnalysis
import com.wallpapercropfixer.domain.model.WallpaperTarget
import com.wallpapercropfixer.domain.repository.ExportDestination
import com.wallpapercropfixer.domain.repository.ExportResult
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EditorViewModelConcurrencyTest {

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
            Thread.sleep(10)
        }
    }

    @Test
    fun `rapid crop mode changes publish only the last request`() {
        val renderer = FakeWallpaperBitmapRenderer(
            sleepMillisByMode = mapOf(
                CropMode.SAFE_FIT to 600L,
                CropMode.BALANCED to 300L,
                CropMode.FILL to 60L
            )
        )
        val vm = buildEditorViewModel(renderer = renderer)

        vm.loadImage("file:///photo")
        waitForCondition { vm.uiState.value.previewBitmap != null && !vm.uiState.value.isRendering }

        vm.setCropMode(CropMode.SAFE_FIT)
        vm.setCropMode(CropMode.BALANCED)
        vm.setCropMode(CropMode.FILL)

        waitForCondition { vm.uiState.value.previewBitmap?.width == CropMode.FILL.ordinal + 1 && !vm.uiState.value.isRendering }

        // Allow any stale render (SAFE_FIT sleeps longest) to have published if the
        // generation guard failed. The final preview must still be FILL.
        Thread.sleep(800)

        assertEquals(CropMode.FILL.ordinal + 1, vm.uiState.value.previewBitmap?.width)
        assertNull("stale render must not surface an error", vm.uiState.value.errorMessage)
        assertFalse("published bitmap must never be recycled", vm.uiState.value.previewBitmap?.isRecycled == true)
    }

    @Test
    fun `loading A then B keeps B authoritative even when A completes last`() {
        val faceRepo = FakeFaceDetectionRepository()
        val analysisA = SubjectAnalysis(listOf(FaceBounds(0f, 0f, 100f, 100f)), FocusPoint(0.1f, 0.1f))
        val analysisB = SubjectAnalysis(listOf(FaceBounds(0f, 0f, 100f, 100f)), FocusPoint(0.9f, 0.9f))
        faceRepo.analyses["A"] = analysisA
        faceRepo.analyses["B"] = analysisB
        faceRepo.gates["A"] = CompletableDeferred()

        val vm = buildEditorViewModel(faceDetectionRepository = faceRepo)

        vm.loadImage("A")
        Thread.sleep(150) // let A reach the (non-cancellable) face-detection gate

        vm.loadImage("B")
        waitForCondition { faceRepo.completedCount == 1 && !vm.uiState.value.isLoading }
        waitForCondition { vm.uiState.value.subjectAnalysis?.suggestedFocusPoint == analysisB.suggestedFocusPoint }

        // Release A's analysis late.
        faceRepo.gates.getValue("A").complete(Unit)
        waitForCondition { faceRepo.completedCount == 2 }

        Thread.sleep(300)
        assertEquals("B", vm.uiState.value.imageUri)
        assertEquals("A's stale analysis must never replace B's", analysisB.suggestedFocusPoint,
            vm.uiState.value.subjectAnalysis?.suggestedFocusPoint)
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun `cancellation of a suspended render does not surface as an error`() {
        val renderer = SuspendingRenderer()
        val vm = buildEditorViewModel(renderer = renderer)

        vm.loadImage("file:///photo")
        waitForCondition { renderer.started >= 1 }
        vm.setCropMode(CropMode.SAFE_FIT)
        waitForCondition { renderer.started >= 2 && !vm.uiState.value.isRendering }

        assertNull("cancellation must not become an error", vm.uiState.value.errorMessage)
        assertEquals(CropMode.SAFE_FIT.ordinal + 1, vm.uiState.value.previewBitmap?.width)
    }

    @Test
    fun `published preview bitmaps are never manually recycled across regenerations`() {
        val vm = buildEditorViewModel()
        vm.loadImage("file:///photo")
        waitForCondition { vm.uiState.value.previewBitmap != null && !vm.uiState.value.isRendering }

        val first = vm.uiState.value.previewBitmap
        assertNotNull(first)

        vm.setCropMode(CropMode.FILL)
        waitForCondition { vm.uiState.value.previewBitmap !== first && !vm.uiState.value.isRendering }

        assertFalse("the replaced preview bitmap must not be recycled while referenced", first!!.isRecycled)
        assertFalse(vm.uiState.value.previewBitmap!!.isRecycled)
    }

    @Test
    fun `apply uses the current published bitmap and reports success`() {
        val applyRepo = FakeApplyRepository()
        val vm = buildEditorViewModel(applyRepository = applyRepo)
        vm.loadImage("file:///photo")
        waitForCondition { vm.uiState.value.previewBitmap != null && !vm.uiState.value.isRendering }

        val published = vm.uiState.value.previewBitmap
        vm.applyWallpaper()
        waitForCondition { !vm.uiState.value.isRendering }

        assertEquals(1, applyRepo.applied.size)
        assertEquals(WallpaperTarget.HOME, applyRepo.applied[0].second)
        assertEquals(published, applyRepo.applied[0].first)
        assertFalse(applyRepo.applied[0].first.isRecycled)
        assertNull(vm.uiState.value.errorMessage)
        assertEquals(R.string.editor_applied_home, vm.uiState.value.successMessage?.resId)
    }

    @Test
    fun `apply during an in-flight render is ignored`() {
        val renderer = FakeWallpaperBitmapRenderer(sleepMillisByMode = mapOf(CropMode.BALANCED to 400L))
        val applyRepo = FakeApplyRepository()
        val vm = buildEditorViewModel(renderer = renderer, applyRepository = applyRepo)
        vm.loadImage("file:///photo")
        waitForCondition { vm.uiState.value.previewBitmap != null && !vm.uiState.value.isRendering }

        vm.setCropMode(CropMode.FILL) // sets isRendering synchronously
        vm.applyWallpaper()
        waitForCondition { !vm.uiState.value.isRendering }

        assertTrue("apply during render must be ignored", applyRepo.applied.isEmpty())
    }

    @Test
    fun `export message matches the actual destination`() {
        val exportRepo = FakeExportRepository(
            result = ExportResult(ExportDestination.APP_EXTERNAL_FILES, "/external", "wcf.jpg")
        )
        val vm = buildEditorViewModel(exportRepository = exportRepo)
        vm.loadImage("file:///photo")
        waitForCondition { vm.uiState.value.previewBitmap != null && !vm.uiState.value.isRendering }

        vm.exportWallpaper()
        waitForCondition { !vm.uiState.value.isRendering }

        assertEquals(1, exportRepo.exported.size)
        assertEquals(R.string.export_saved_app_external, vm.uiState.value.successMessage?.resId)
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun `export failure reports a friendly error`() {
        val exportRepo = FakeExportRepository()
        exportRepo.failWith = IllegalStateException("MediaStore insert returned null")
        val vm = buildEditorViewModel(exportRepository = exportRepo)
        vm.loadImage("file:///photo")
        waitForCondition { vm.uiState.value.previewBitmap != null && !vm.uiState.value.isRendering }

        vm.exportWallpaper()
        waitForCondition { !vm.uiState.value.isRendering }

        assertEquals(R.string.error_export, vm.uiState.value.errorMessage?.resId)
    }

    private class SuspendingRenderer : WallpaperBitmapRenderer {
        var started = 0
        override suspend fun render(
            request: com.wallpapercropfixer.domain.model.WallpaperRenderRequest,
            plan: com.wallpapercropfixer.domain.model.WallpaperRenderPlan
        ): android.graphics.Bitmap {
            started++
            kotlinx.coroutines.delay(1000)
            return android.graphics.Bitmap.createBitmap(request.cropMode.ordinal + 1, 10, android.graphics.Bitmap.Config.ARGB_8888)
        }
    }
}
