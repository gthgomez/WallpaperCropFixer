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
import kotlinx.coroutines.runBlocking
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
            Thread.yield()
        }
    }

    /** Blocks until the render for [mode] has entered the fake renderer, then awaits its start marker. */
    private fun awaitRenderStarted(
        renderer: FakeWallpaperBitmapRenderer,
        mode: CropMode,
        minimumInvocations: Int = 1
    ) {
        waitForCondition { (renderer.invocationCount[mode] ?: 0) >= minimumInvocations }
        runBlocking { renderer.started.getValue(mode).await() }
    }

    @Test
    fun `rapid crop mode changes publish only the last request`() {
        val renderer = FakeWallpaperBitmapRenderer()
        val vm = buildEditorViewModel(renderer = renderer)

        vm.loadImage("file:///photo")
        waitForCondition { vm.uiState.value.previewBitmap != null && !vm.uiState.value.isBusy }

        renderer.gates[CropMode.SAFE_FIT] = CompletableDeferred()
        renderer.gates[CropMode.BALANCED] = CompletableDeferred()
        renderer.gates[CropMode.FILL] = CompletableDeferred()
        renderer.nonCancellableModes += setOf(CropMode.SAFE_FIT, CropMode.BALANCED)

        vm.setCropMode(CropMode.SAFE_FIT)
        awaitRenderStarted(renderer, CropMode.SAFE_FIT)
        val balancedBaseline = renderer.invocationCount[CropMode.BALANCED] ?: 0
        vm.setCropMode(CropMode.BALANCED)
        awaitRenderStarted(renderer, CropMode.BALANCED, balancedBaseline + 1)
        vm.setCropMode(CropMode.FILL)
        awaitRenderStarted(renderer, CropMode.FILL)
        renderer.gates.getValue(CropMode.FILL).complete(Unit)

        waitForCondition { vm.uiState.value.previewBitmap?.width == CropMode.FILL.ordinal + 1 && !vm.uiState.value.isBusy }

        // Release stale non-cooperative renders after the newest revision published.
        renderer.gates.getValue(CropMode.SAFE_FIT).complete(Unit)
        renderer.gates.getValue(CropMode.BALANCED).complete(Unit)

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
        faceRepo.started["A"] = CompletableDeferred()

        val vm = buildEditorViewModel(faceDetectionRepository = faceRepo)

        vm.loadImage("A")
        runBlocking { faceRepo.started.getValue("A").await() }

        vm.loadImage("B")
        waitForCondition { faceRepo.completedCount == 1 && !vm.uiState.value.isLoading }
        waitForCondition { vm.uiState.value.subjectAnalysis?.suggestedFocusPoint == analysisB.suggestedFocusPoint }

        // Release A's analysis late.
        faceRepo.gates.getValue("A").complete(Unit)
        waitForCondition { faceRepo.completedCount == 2 }

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
        waitForCondition { renderer.started >= 2 }
        renderer.release.complete(Unit)
        waitForCondition { vm.uiState.value.previewBitmap != null && !vm.uiState.value.isBusy }

        assertNull("cancellation must not become an error", vm.uiState.value.errorMessage)
        assertEquals(CropMode.SAFE_FIT.ordinal + 1, vm.uiState.value.previewBitmap?.width)
    }

    @Test
    fun `published preview bitmaps are never manually recycled across regenerations`() {
        val vm = buildEditorViewModel()
        vm.loadImage("file:///photo")
        waitForCondition { vm.uiState.value.previewBitmap != null && !vm.uiState.value.isBusy }

        val first = vm.uiState.value.previewBitmap
        assertNotNull(first)

        vm.setCropMode(CropMode.FILL)
        waitForCondition { vm.uiState.value.previewBitmap !== first && !vm.uiState.value.isBusy }

        assertFalse("the replaced preview bitmap must not be recycled while referenced", first!!.isRecycled)
        assertFalse(vm.uiState.value.previewBitmap!!.isRecycled)
    }

    @Test
    fun `apply uses the current published bitmap and reports success`() {
        val applyRepo = FakeApplyRepository()
        val vm = buildEditorViewModel(applyRepository = applyRepo)
        vm.loadImage("file:///photo")
        waitForCondition { vm.uiState.value.previewBitmap != null && !vm.uiState.value.isBusy }

        val published = vm.uiState.value.previewBitmap
        vm.applyWallpaper()
        waitForCondition { !vm.uiState.value.isBusy }

        assertEquals(1, applyRepo.applied.size)
        assertEquals(WallpaperTarget.HOME, applyRepo.applied[0].second)
        assertEquals(published, applyRepo.applied[0].first)
        assertFalse(applyRepo.applied[0].first.isRecycled)
        assertNull(vm.uiState.value.errorMessage)
        assertEquals(R.string.editor_applied_home, vm.uiState.value.successMessage?.resId)
    }

    @Test
    fun `apply during an in-flight render is ignored`() {
        val renderer = FakeWallpaperBitmapRenderer()
        renderer.gates[CropMode.FILL] = CompletableDeferred()
        val applyRepo = FakeApplyRepository()
        val vm = buildEditorViewModel(renderer = renderer, applyRepository = applyRepo)
        vm.loadImage("file:///photo")
        waitForCondition { vm.uiState.value.previewBitmap != null && !vm.uiState.value.isBusy }

        vm.setCropMode(CropMode.FILL) // invalidates the published preview synchronously
        vm.applyWallpaper()
        awaitRenderStarted(renderer, CropMode.FILL)
        renderer.gates.getValue(CropMode.FILL).complete(Unit)
        waitForCondition { !vm.uiState.value.isBusy }

        assertTrue("apply during render must be ignored", applyRepo.applied.isEmpty())
    }

    @Test
    fun `export message matches the actual destination`() {
        val exportRepo = FakeExportRepository(
            result = ExportResult(ExportDestination.APP_EXTERNAL_FILES, "/external", "wcf.jpg")
        )
        val vm = buildEditorViewModel(exportRepository = exportRepo)
        vm.loadImage("file:///photo")
        waitForCondition { vm.uiState.value.previewBitmap != null && !vm.uiState.value.isBusy }

        vm.exportWallpaper()
        waitForCondition { !vm.uiState.value.isBusy }

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
        waitForCondition { vm.uiState.value.previewBitmap != null && !vm.uiState.value.isBusy }

        vm.exportWallpaper()
        waitForCondition { !vm.uiState.value.isBusy }

        assertEquals(R.string.error_export, vm.uiState.value.errorMessage?.resId)
    }

    @Test
    fun `apply completion cannot clear busy state or certify stale crop revision`() {
        val renderer = FakeWallpaperBitmapRenderer()
        val applyRepo = FakeApplyRepository().apply {
            started = CompletableDeferred()
            gate = CompletableDeferred()
        }
        val vm = buildEditorViewModel(renderer = renderer, applyRepository = applyRepo)
        vm.loadImage("file:///photo")
        waitForCondition { vm.uiState.value.previewBitmap != null && !vm.uiState.value.isBusy }
        val appliedRevision = vm.uiState.value.publishedPreview!!.revision
        val appliedBitmap = vm.uiState.value.previewBitmap

        vm.applyWallpaper()
        runBlocking { applyRepo.started!!.await() }

        val renderB = CompletableDeferred<Unit>()
        renderer.gates[CropMode.FILL] = renderB
        vm.setCropMode(CropMode.FILL)
        waitForCondition { (renderer.invocationCount[CropMode.FILL] ?: 0) == 1 }

        applyRepo.gate!!.complete(Unit)
        waitForCondition { !vm.uiState.value.isApplying }

        assertTrue("render B must remain busy after apply A completes", vm.uiState.value.isRendering)
        assertNull("stale apply must not certify revision A", vm.uiState.value.successMessage)
        assertNull("stale apply must not surface an error", vm.uiState.value.errorMessage)
        assertEquals(1, applyRepo.applied.size)
        assertEquals(appliedBitmap, applyRepo.applied.single().first)
        assertNull("revision A is no longer publishable", vm.uiState.value.previewBitmap)

        renderB.complete(Unit)
        waitForCondition { !vm.uiState.value.isBusy && vm.uiState.value.previewBitmap?.width == CropMode.FILL.ordinal + 1 }
        assertTrue(vm.uiState.value.publishedPreview!!.revision > appliedRevision)
    }

    @Test
    fun `export completion cannot clear busy state or certify stale face-aware revision`() {
        val renderer = FakeWallpaperBitmapRenderer()
        val exportRepo = FakeExportRepository().apply {
            started = CompletableDeferred()
            gate = CompletableDeferred()
        }
        val vm = buildEditorViewModel(renderer = renderer, exportRepository = exportRepo)
        vm.loadImage("file:///photo")
        waitForCondition { vm.uiState.value.previewBitmap != null && !vm.uiState.value.isBusy }
        val exportedBitmap = vm.uiState.value.previewBitmap
        val exportedRevision = vm.uiState.value.publishedPreview!!.revision

        vm.exportWallpaper()
        runBlocking { exportRepo.started!!.await() }

        val renderB = CompletableDeferred<Unit>()
        renderer.gates[CropMode.BALANCED] = renderB
        vm.toggleFaceAware(false)
        waitForCondition { (renderer.invocationCount[CropMode.BALANCED] ?: 0) >= 2 }

        exportRepo.gate!!.complete(Unit)
        waitForCondition { !vm.uiState.value.isExporting }

        assertTrue("render B must remain busy after export A completes", vm.uiState.value.isRendering)
        assertNull("stale export must not certify revision A", vm.uiState.value.successMessage)
        assertEquals(exportedBitmap, exportRepo.exported.single().first)
        assertNull("revision A is no longer publishable", vm.uiState.value.previewBitmap)

        renderB.complete(Unit)
        waitForCondition { !vm.uiState.value.isBusy && vm.uiState.value.previewBitmap != null }
        assertTrue(vm.uiState.value.publishedPreview!!.revision > exportedRevision)
    }

    @Test
    fun `apply BOTH owns one published revision while a new preview begins`() {
        val renderer = FakeWallpaperBitmapRenderer()
        val applyRepo = FakeApplyRepository().apply {
            started = CompletableDeferred()
            gate = CompletableDeferred()
        }
        val vm = buildEditorViewModel(renderer = renderer, applyRepository = applyRepo)
        vm.loadImage("file:///photo")
        waitForCondition { vm.uiState.value.previewBitmap != null && !vm.uiState.value.isBusy }

        vm.setWallpaperTarget(WallpaperTarget.BOTH)
        waitForCondition { vm.uiState.value.lockPreviewBitmap != null && !vm.uiState.value.isBusy }
        val revisionA = vm.uiState.value.publishedPreview!!

        vm.applyWallpaper()
        runBlocking { applyRepo.started!!.await() }

        val renderB = CompletableDeferred<Unit>()
        renderer.gates[CropMode.FILL] = renderB
        vm.setCropMode(CropMode.FILL)
        waitForCondition { (renderer.invocationCount[CropMode.FILL] ?: 0) == 1 }
        applyRepo.gate!!.complete(Unit)
        waitForCondition { !vm.uiState.value.isApplying }

        assertTrue(vm.uiState.value.isRendering)
        assertNull(vm.uiState.value.successMessage)
        assertEquals(2, applyRepo.applied.size)
        assertEquals(listOf(WallpaperTarget.HOME, WallpaperTarget.LOCK), applyRepo.applied.map { it.second })
        assertEquals(revisionA.home.bitmap, applyRepo.applied[0].first)
        assertEquals(revisionA.lock!!.bitmap, applyRepo.applied[1].first)

        renderB.complete(Unit)
        waitForCondition { !vm.uiState.value.isBusy && vm.uiState.value.lockPreviewBitmap != null }
        assertTrue(vm.uiState.value.publishedPreview!!.revision > revisionA.revision)
    }

    @Test
    fun `repeated apply and save taps cannot start competing operations`() {
        val applyRepo = FakeApplyRepository().apply {
            started = CompletableDeferred()
            gate = CompletableDeferred()
        }
        val exportRepo = FakeExportRepository().apply {
            started = CompletableDeferred()
            gate = CompletableDeferred()
        }
        val vm = buildEditorViewModel(applyRepository = applyRepo, exportRepository = exportRepo)
        vm.loadImage("file:///photo")
        waitForCondition { vm.uiState.value.previewBitmap != null && !vm.uiState.value.isBusy }

        vm.applyWallpaper()
        vm.applyWallpaper()
        vm.exportWallpaper()
        vm.exportWallpaper()
        runBlocking { applyRepo.started!!.await() }
        assertTrue("Save must not compete with Apply", exportRepo.started?.isCompleted != true)
        assertEquals(0, exportRepo.exported.size)

        applyRepo.gate!!.complete(Unit)
        waitForCondition { !vm.uiState.value.isBusy }
        assertEquals(1, applyRepo.applied.size)
    }

    private class SuspendingRenderer : WallpaperBitmapRenderer {
        @Volatile var started = 0
        val release = CompletableDeferred<Unit>()
        override suspend fun render(
            request: com.wallpapercropfixer.domain.model.WallpaperRenderRequest,
            plan: com.wallpapercropfixer.domain.model.WallpaperRenderPlan
        ): android.graphics.Bitmap {
            started++
            release.await()
            return android.graphics.Bitmap.createBitmap(request.cropMode.ordinal + 1, 10, android.graphics.Bitmap.Config.ARGB_8888)
        }
    }
}
