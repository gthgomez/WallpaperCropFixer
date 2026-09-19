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
    fun `committing apply rejects framing and image mutations until completion`() {
        val repo = FakeApplyRepository().apply {
            started = CompletableDeferred()
            gate = CompletableDeferred()
        }
        val vm = buildEditorViewModel(applyRepository = repo)
        vm.loadImage("photo")
        waitForCondition { vm.uiState.value.isPreviewCurrent && !vm.uiState.value.isBusy }
        val before = vm.uiState.value
        vm.applyWallpaper()
        runBlocking { repo.started!!.await() }
        vm.setCropMode(CropMode.FILL)
        vm.setWallpaperTarget(WallpaperTarget.BOTH)
        vm.toggleFaceAware(false)
        vm.updateManualFocusPoint(FocusPoint(0.2f, 0.3f))
        vm.resetToDefaults()
        vm.loadImage("replacement")
        vm.refreshForConfigurationChange()
        assertEquals(before.publishedPreview, vm.uiState.value.publishedPreview)
        assertEquals(before.cropMode, vm.uiState.value.cropMode)
        assertEquals(before.wallpaperTarget, vm.uiState.value.wallpaperTarget)
        assertEquals(before.faceAwareEnabled, vm.uiState.value.faceAwareEnabled)
        assertEquals("photo", vm.uiState.value.imageUri)
        repo.gate!!.complete(Unit)
        waitForCondition { !vm.uiState.value.isBusy }
        assertEquals(R.string.editor_applied_home, vm.uiState.value.successMessage?.resId)
    }

    @Test
    fun `committing save rejects edits but edits resume after completion`() {
        val repo = FakeExportRepository().apply {
            started = CompletableDeferred()
            gate = CompletableDeferred()
        }
        val vm = buildEditorViewModel(exportRepository = repo)
        vm.loadImage("photo")
        waitForCondition { vm.uiState.value.isPreviewCurrent && !vm.uiState.value.isBusy }
        val publication = vm.uiState.value.publishedPreview
        vm.exportWallpaper()
        runBlocking { repo.started!!.await() }
        vm.toggleFaceAware(false)
        assertTrue(vm.uiState.value.faceAwareEnabled)
        assertEquals(publication, vm.uiState.value.publishedPreview)
        repo.gate!!.complete(Unit)
        waitForCondition { !vm.uiState.value.isBusy }
        vm.toggleFaceAware(false)
        waitForCondition { !vm.uiState.value.isBusy }
        assertFalse(vm.uiState.value.faceAwareEnabled)
        assertTrue(vm.uiState.value.publishedPreview!!.revision > publication!!.revision)
    }

    @Test
    fun `queued configuration refresh drains when gated apply finishes before snackbar dismissal`() {
        val deviceRepository = FakeDeviceProfileRepository()
        val applyRepository = FakeApplyRepository().apply {
            started = CompletableDeferred()
            gate = CompletableDeferred()
        }
        val vm = buildEditorViewModel(
            deviceProfileRepository = deviceRepository,
            applyRepository = applyRepository
        )
        vm.loadImage("photo")
        waitForCondition { vm.uiState.value.isPreviewCurrent && !vm.uiState.value.isBusy }

        deviceRepository.profile = deviceRepository.profile.copy(
            screenWidthPx = 1440,
            screenHeightPx = 2560,
            aspectRatio = 1440f / 2560f
        )
        deviceRepository.gate = CompletableDeferred()
        vm.applyWallpaper()
        runBlocking { applyRepository.started!!.await() }
        vm.refreshForConfigurationChange()

        applyRepository.gate!!.complete(Unit)
        waitForCondition {
            !vm.uiState.value.isCommitting &&
                vm.uiState.value.successMessage?.resId == R.string.editor_applied_home &&
                deviceRepository.calls >= 2
        }
        assertEquals(R.string.editor_applied_home, vm.uiState.value.successMessage?.resId)

        // Editing immediately cancels the snackbar effect, so the refresh must already
        // be owned by the operation completion rather than clearSuccess().
        vm.setCropMode(CropMode.FILL)
        assertFalse("queued refresh must withdraw old geometry while metrics are gated",
            vm.uiState.value.isPreviewCurrent)
        deviceRepository.gate!!.complete(Unit)
        waitForCondition {
            deviceRepository.calls >= 2 &&
                vm.uiState.value.isPreviewCurrent &&
                !vm.uiState.value.isBusy
        }

        assertEquals(1440, vm.uiState.value.deviceProfile?.screenWidthPx)
    }

    @Test
    fun `queued configuration refresh drains when gated save finishes before snackbar dismissal`() {
        val deviceRepository = FakeDeviceProfileRepository()
        val exportRepository = FakeExportRepository().apply {
            started = CompletableDeferred()
            gate = CompletableDeferred()
        }
        val vm = buildEditorViewModel(
            deviceProfileRepository = deviceRepository,
            exportRepository = exportRepository
        )
        vm.loadImage("photo")
        waitForCondition { vm.uiState.value.isPreviewCurrent && !vm.uiState.value.isBusy }

        deviceRepository.profile = deviceRepository.profile.copy(
            screenWidthPx = 1440,
            screenHeightPx = 2560,
            aspectRatio = 1440f / 2560f
        )
        deviceRepository.gate = CompletableDeferred()
        vm.exportWallpaper()
        runBlocking { exportRepository.started!!.await() }
        vm.refreshForConfigurationChange()

        exportRepository.gate!!.complete(Unit)
        waitForCondition {
            !vm.uiState.value.isCommitting &&
                vm.uiState.value.successMessage?.resId == R.string.export_saved_media_store &&
                deviceRepository.calls >= 2
        }
        assertEquals(R.string.export_saved_media_store, vm.uiState.value.successMessage?.resId)

        vm.setCropMode(CropMode.FILL)
        assertFalse("queued refresh must withdraw old geometry while metrics are gated",
            vm.uiState.value.isPreviewCurrent)
        deviceRepository.gate!!.complete(Unit)
        waitForCondition {
            deviceRepository.calls >= 2 &&
                vm.uiState.value.isPreviewCurrent &&
                !vm.uiState.value.isBusy
        }

        assertEquals(1440, vm.uiState.value.deviceProfile?.screenWidthPx)
    }

    @Test
    fun `enabling face analysis waits then renders exactly once`() {
        val faces = FakeFaceDetectionRepository()
        val renderer = FakeWallpaperBitmapRenderer()
        val settings = FakeSettingsRepository().apply {
            settings.value = settings.value.copy(defaultFaceAwareEnabled = false)
        }
        val vm = buildEditorViewModel(settingsRepository = settings, renderer = renderer, faceDetectionRepository = faces)
        vm.loadImage("photo")
        waitForCondition { vm.uiState.value.isPreviewCurrent && !vm.uiState.value.isBusy }
        val baseline = renderer.renderCalls
        faces.gates["photo"] = CompletableDeferred()
        vm.toggleFaceAware(true)
        waitForCondition { faces.started["photo"]?.isCompleted == true }
        assertEquals(baseline, renderer.renderCalls)
        faces.gates.getValue("photo").complete(Unit)
        waitForCondition { vm.uiState.value.isPreviewCurrent && !vm.uiState.value.isBusy }
        assertEquals(baseline + 1, renderer.renderCalls)
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

    @Test
    fun `re-render keeps the last preview visible and replaces it on completion`() {
        val renderer = FakeWallpaperBitmapRenderer()
        val vm = buildEditorViewModel(renderer = renderer)
        vm.loadImage("file:///photo")
        waitForCondition { vm.uiState.value.previewBitmap != null && !vm.uiState.value.isBusy }
        val first = vm.uiState.value.previewBitmap
        val firstRevision = vm.uiState.value.publishedPreview!!.revision

        renderer.gates[CropMode.FILL] = CompletableDeferred()
        vm.setCropMode(CropMode.FILL)
        awaitRenderStarted(renderer, CropMode.FILL)

        assertTrue("re-render must be in flight", vm.uiState.value.isRendering)
        assertEquals("last good preview must stay visible while re-rendering",
            first, vm.uiState.value.activeBitmap)
        assertNull("eligibility must be withdrawn during the re-render",
            vm.uiState.value.publishedPreview)
        assertEquals("retained preview must still be the old revision",
            firstRevision, vm.uiState.value.retainedPreview!!.revision)

        renderer.gates.getValue(CropMode.FILL).complete(Unit)
        waitForCondition {
            !vm.uiState.value.isBusy && vm.uiState.value.previewBitmap?.width == CropMode.FILL.ordinal + 1
        }
        assertTrue(vm.uiState.value.publishedPreview!!.revision > firstRevision)
        assertTrue("the new revision must replace the retained bitmap",
            vm.uiState.value.activeBitmap !== first)
    }

    @Test
    fun `retryRender recovers after a failed render`() {
        val renderer = FlakyRenderer(failuresRemaining = 1)
        val vm = buildEditorViewModel(renderer = renderer)
        vm.loadImage("file:///photo")
        // Wait for the first render to settle definitively: isBusy starts false
        // before the IO load begins, so waiting on it alone can race the load.
        waitForCondition { vm.uiState.value.renderFailed && !vm.uiState.value.isBusy }

        assertNull(vm.uiState.value.previewBitmap)

        vm.retryRender()
        waitForCondition { !vm.uiState.value.isBusy }

        assertFalse("retry must clear the failure state", vm.uiState.value.renderFailed)
        assertNotNull("retry must publish a preview", vm.uiState.value.previewBitmap)
    }

    @Test
    fun `retryRender does not stack a second render while busy`() {
        val renderer = FakeWallpaperBitmapRenderer()
        val vm = buildEditorViewModel(renderer = renderer)
        vm.loadImage("file:///photo")
        waitForCondition { vm.uiState.value.previewBitmap != null && !vm.uiState.value.isBusy }

        renderer.gates[CropMode.BALANCED] = CompletableDeferred()
        vm.setCropMode(CropMode.BALANCED)
        awaitRenderStarted(renderer, CropMode.BALANCED, 2)
        val callsBeforeRetry = renderer.renderCalls

        vm.retryRender()

        assertEquals("retry must be ignored while a render is in flight",
            callsBeforeRetry, renderer.renderCalls)

        renderer.gates.getValue(CropMode.BALANCED).complete(Unit)
        waitForCondition { !vm.uiState.value.isBusy }
    }

    @Test
    fun `retryRender does nothing before an image is loaded`() {
        val renderer = FakeWallpaperBitmapRenderer()
        val vm = buildEditorViewModel(renderer = renderer)

        vm.retryRender()

        assertEquals(0, renderer.renderCalls)
        assertFalse(vm.uiState.value.isBusy)
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun `lock-target export names the file with the wcf_lock prefix`() {
        val exportRepo = FakeExportRepository()
        val vm = buildEditorViewModel(exportRepository = exportRepo)
        vm.loadImage("file:///photo")
        waitForCondition { vm.uiState.value.previewBitmap != null && !vm.uiState.value.isBusy }

        vm.setWallpaperTarget(WallpaperTarget.LOCK)
        waitForCondition { !vm.uiState.value.isBusy }
        vm.exportWallpaper()
        waitForCondition { !vm.uiState.value.isBusy }

        assertEquals(1, exportRepo.exported.size)
        assertTrue("lock export must use the wcf_lock_ prefix: ${exportRepo.exported[0].second}",
            exportRepo.exported[0].second.startsWith("wcf_lock_"))
    }

    @Test
    fun `both-target export writes one wcf_home_ and one wcf_lock_ file`() {
        val exportRepo = FakeExportRepository()
        val vm = buildEditorViewModel(exportRepository = exportRepo)
        vm.loadImage("file:///photo")
        waitForCondition { vm.uiState.value.previewBitmap != null && !vm.uiState.value.isBusy }

        vm.setWallpaperTarget(WallpaperTarget.BOTH)
        waitForCondition { !vm.uiState.value.isBusy && vm.uiState.value.lockPreviewBitmap != null }
        vm.exportWallpaper()
        waitForCondition { !vm.uiState.value.isBusy }

        assertEquals(2, exportRepo.exported.size)
        assertTrue(exportRepo.exported[0].second.startsWith("wcf_home_"))
        assertTrue(exportRepo.exported[1].second.startsWith("wcf_lock_"))
    }

    @Test
    fun `both export attempts each target and reports either partial success`() {
        for (failedPrefix in listOf("wcf_home", "wcf_lock")) {
            val repo = FakeExportRepository().apply { failPrefix = failedPrefix }
            val vm = buildEditorViewModel(exportRepository = repo)
            vm.loadImage("photo")
            waitForCondition { vm.uiState.value.isPreviewCurrent && !vm.uiState.value.isBusy }
            vm.setWallpaperTarget(WallpaperTarget.BOTH)
            waitForCondition { vm.uiState.value.lockPreviewBitmap != null && !vm.uiState.value.isBusy }
            vm.exportWallpaper()
            waitForCondition { !vm.uiState.value.isBusy }
            assertEquals(1, repo.exported.size)
            assertNull(vm.uiState.value.successMessage)
            assertNotNull(vm.uiState.value.errorMessage)
            assertTrue(vm.uiState.value.errorMessage?.resId != R.string.error_export)
        }
    }

    @Test
    fun `late face analysis after disabling cannot replace the center preview`() {
        val faces = FakeFaceDetectionRepository()
        val renderer = FakeWallpaperBitmapRenderer()
        val settings = FakeSettingsRepository().apply {
            settings.value = settings.value.copy(defaultFaceAwareEnabled = false)
        }
        val vm = buildEditorViewModel(settingsRepository = settings, renderer = renderer, faceDetectionRepository = faces)
        vm.loadImage("photo")
        waitForCondition { vm.uiState.value.isPreviewCurrent && !vm.uiState.value.isBusy }
        faces.gates["photo"] = CompletableDeferred()
        vm.toggleFaceAware(true)
        waitForCondition { faces.started["photo"]?.isCompleted == true }
        vm.toggleFaceAware(false)
        waitForCondition { vm.uiState.value.isPreviewCurrent && !vm.uiState.value.isBusy }
        val center = vm.uiState.value.publishedPreview
        faces.gates.getValue("photo").complete(Unit)
        waitForCondition { faces.completedCount == 1 }
        assertFalse(vm.uiState.value.faceAwareEnabled)
        assertEquals(center, vm.uiState.value.publishedPreview)
        assertNull(vm.uiState.value.subjectAnalysis)
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
