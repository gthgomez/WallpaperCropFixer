package com.wallpapercropfixer.presentation.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wallpapercropfixer.R
import com.wallpapercropfixer.core.util.FileNameFactory
import com.wallpapercropfixer.core.util.Logger
import com.wallpapercropfixer.data.wallpaper.BothScreensApplyFailedException
import com.wallpapercropfixer.data.wallpaper.HomeScreenApplyFailedException
import com.wallpapercropfixer.data.wallpaper.LockScreenApplyFailedException
import com.wallpapercropfixer.data.wallpaper.WallpaperPolicyDisallowedException
import com.wallpapercropfixer.data.wallpaper.WallpaperSetFailedException
import com.wallpapercropfixer.data.wallpaper.WallpaperUnsupportedException
import com.wallpapercropfixer.domain.model.BackgroundFillMode
import com.wallpapercropfixer.domain.model.CropMode
import com.wallpapercropfixer.domain.model.WallpaperRenderRequest
import com.wallpapercropfixer.domain.model.WallpaperTarget
import com.wallpapercropfixer.domain.model.UserSettings
import com.wallpapercropfixer.domain.repository.ExportDestination
import com.wallpapercropfixer.domain.repository.ExportResult
import com.wallpapercropfixer.domain.repository.ImageRepository
import com.wallpapercropfixer.domain.repository.SettingsRepository
import com.wallpapercropfixer.domain.usecase.AnalyzeSubjectUseCase
import com.wallpapercropfixer.domain.usecase.ApplyWallpaperUseCase
import com.wallpapercropfixer.domain.usecase.BuildWallpaperRenderPlanUseCase
import com.wallpapercropfixer.domain.usecase.ExportWallpaperUseCase
import com.wallpapercropfixer.domain.usecase.GetCurrentDeviceProfileUseCase
import com.wallpapercropfixer.domain.usecase.RenderWallpaperBitmapUseCase
import com.wallpapercropfixer.domain.usecase.ResolveWallpaperBehaviorUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject

/**
 * Owns the editor state and the render/load pipeline.
 *
 * Concurrency invariant: a result generated for request N may only publish state
 * while N is still the latest request. All rendering inputs are captured into an
 * immutable snapshot before any asynchronous work begins, and every publish is
 * guarded by a monotonically increasing generation token. This makes the preview,
 * the selected options, and any applied/exported bitmap always correspond to the
 * same render generation. A committed operation owns the immutable PublishedPreview
 * it captured; later option changes invalidate publication without changing the
 * operation's inputs. Bitmaps are intentionally never manually recycled because
 * Compose and committed operations may still reference them.
 *
 * Display vs eligibility: [EditorUiState.retainedPreview] keeps the last committed
 * render visible while a newer one renders or after a failure, but only
 * [EditorUiState.publishedPreview] restores Apply/Save eligibility. The displayed
 * [RenderedPreview] tuple (request, plan, bitmap) is selected as one unit so the
 * overlay and tap mapping always use the geometry of the bitmap actually shown.
 */
@HiltViewModel
class EditorViewModel @Inject constructor(
    private val imageRepository: ImageRepository,
    private val settingsRepository: SettingsRepository,
    private val getDeviceProfile: GetCurrentDeviceProfileUseCase,
    private val resolveBehavior: ResolveWallpaperBehaviorUseCase,
    private val analyzeSubject: AnalyzeSubjectUseCase,
    private val buildRenderPlan: BuildWallpaperRenderPlanUseCase,
    private val renderBitmap: RenderWallpaperBitmapUseCase,
    private val exportWallpaper: ExportWallpaperUseCase,
    private val applyWallpaper: ApplyWallpaperUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    // Generation tokens are bumped from both the main thread (option changes) and
    // IO-dispatched load paths, so they must be atomics.
    private val previewGeneration = AtomicInteger(0)

    @Volatile
    private var loadGeneration = 0

    private var previewJob: Job? = null
    private var loadJob: Job? = null
    private var faceAnalysisJob: Job? = null
    private val configurationRefreshLock = Any()
    private var pendingConfigurationRefresh = false
    @Volatile
    private var configurationRefreshInFlight = false
    @Volatile
    private var configurationRefreshFailed = false
    private val faceAnalysisGeneration = AtomicInteger(0)

    private val publishedRevision = AtomicLong(0L)

    @Volatile
    private var applyOperationToken = 0L

    @Volatile
    private var exportOperationToken = 0L

    init {
        // Restore in-session options from process-death state synchronously (primitives only).
        restoreOptionsFromSavedState()

        viewModelScope.launch {
            val settings = runCatching { settingsRepository.observeSettings().first() }.getOrDefault(UserSettings())
            if (_uiState.value.isCommitting) return@launch
            val current = _uiState.value
            val initialMode = current.cropMode
            val initialTarget = current.wallpaperTarget
            val initialFill = current.backgroundFillMode
            val initialFace = current.faceAwareEnabled

            val newMode = savedStateHandle.get<String>(KEY_CROP_MODE)
                ?.let { runCatching { CropMode.valueOf(it) }.getOrNull() }
                ?: settings.defaultCropMode
            val newTarget = savedStateHandle.get<String>(KEY_TARGET)
                ?.let { runCatching { WallpaperTarget.valueOf(it) }.getOrNull() }
                ?: settings.defaultWallpaperTarget
            val newFill = savedStateHandle.get<String>(KEY_FILL_MODE)
                ?.let { runCatching { BackgroundFillMode.valueOf(it) }.getOrNull() }
                ?: settings.defaultBackgroundFillMode
            val newFace = savedStateHandle.get<Boolean>(KEY_FACE_AWARE)
                ?: settings.defaultFaceAwareEnabled

            val optionsChanged = newMode != initialMode || newTarget != initialTarget ||
                    newFill != initialFill || newFace != initialFace

            _uiState.update {
                it.copy(
                    cropMode = newMode,
                    wallpaperTarget = newTarget,
                    backgroundFillMode = newFill,
                    faceAwareEnabled = newFace
                )
            }

            // If defaults changed after an image was already loaded/rendered, trigger re-render
            if (optionsChanged && _uiState.value.sourceImageMeta != null) {
                invalidatePublishedPreview()
                generatePreview()
            }
        }
    }

    private fun restoreOptionsFromSavedState() {
        savedStateHandle.get<String>(KEY_CROP_MODE)?.let { runCatching { CropMode.valueOf(it) }.getOrNull() }
            ?.let { mode -> _uiState.update { it.copy(cropMode = mode) } }
        savedStateHandle.get<String>(KEY_TARGET)?.let { runCatching { WallpaperTarget.valueOf(it) }.getOrNull() }
            ?.let { target -> _uiState.update { it.copy(wallpaperTarget = target) } }
        savedStateHandle.get<String>(KEY_FILL_MODE)?.let { runCatching { BackgroundFillMode.valueOf(it) }.getOrNull() }
            ?.let { mode -> _uiState.update { it.copy(backgroundFillMode = mode) } }
        savedStateHandle.get<Boolean>(KEY_FACE_AWARE)?.let { enabled ->
            _uiState.update { it.copy(faceAwareEnabled = enabled) }
        }
        val focusX = savedStateHandle.get<Float>(KEY_FOCUS_X)
        val focusY = savedStateHandle.get<Float>(KEY_FOCUS_Y)
        if (focusX != null && focusY != null) {
            val restoredFocus = com.wallpapercropfixer.domain.model.FocusPoint(focusX, focusY)
            _uiState.update { it.copy(manualFocusPoint = restoredFocus) }
        }
    }

    fun loadImage(uri: String) {
        if (_uiState.value.isCommitting) return
        val current = _uiState.value
        if (current.imageUri == uri && current.sourceImageMeta != null && !current.isLoading) {
            return
        }

        cancelFaceAnalysis()
        val gen = ++loadGeneration
        previewGeneration.incrementAndGet()
        ++applyOperationToken
        ++exportOperationToken
        configurationRefreshFailed = false
        previewJob?.cancel()
        loadJob?.cancel()

        loadJob = viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    imageUri = uri,
                    errorMessage = null,
                    successMessage = null,
                    sourceImageMeta = null,
                    deviceProfile = null,
                    behaviorProfile = null,
                    subjectAnalysis = null,
                    faceDetectionStatus = FaceDetectionStatus.NOT_RUN,
                    publishedPreview = null,
                    retainedPreview = null,
                    renderFailed = false,
                    manualFocusPoint = null,
                    previewingLock = false,
                    isRendering = false,
                    isApplying = false,
                    isExporting = false
                )
            }

            try {
                val meta = imageRepository.readImageMeta(uri)
                val deviceProfile = getDeviceProfile()
                val behaviorProfile = resolveBehavior(deviceProfile)

                if (gen != loadGeneration) return@launch

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        // Stay busy (face analysis + first render) until the first
                        // preview publishes, so the UI never shows a dead empty frame.
                        isRendering = true,
                        sourceImageMeta = meta,
                        deviceProfile = deviceProfile,
                        behaviorProfile = behaviorProfile
                    )
                }

                withContext(Dispatchers.Main.immediate) {
                    if (gen == loadGeneration) generatePreview()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                if (gen == loadGeneration) {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = UiMessage(R.string.error_load_image))
                    }
                }
                Logger.e("loadImage failed", t)
            }
        }
    }

    fun setCropMode(mode: CropMode) {
        if (_uiState.value.isCommitting) return
        invalidatePublishedPreview()
        savedStateHandle[KEY_CROP_MODE] = mode.name
        _uiState.update { it.copy(cropMode = mode) }
        generatePreview()
    }

    fun setWallpaperTarget(target: WallpaperTarget) {
        if (_uiState.value.isCommitting) return
        invalidatePublishedPreview()
        savedStateHandle[KEY_TARGET] = target.name
        _uiState.update {
            it.copy(
                wallpaperTarget = target,
                // The lock view only exists when both screens are rendered.
                previewingLock = it.previewingLock && target == WallpaperTarget.BOTH
            )
        }
        generatePreview()
    }

    fun setBackgroundFillMode(mode: BackgroundFillMode) {
        if (_uiState.value.isCommitting) return
        invalidatePublishedPreview()
        savedStateHandle[KEY_FILL_MODE] = mode.name
        _uiState.update { it.copy(backgroundFillMode = mode) }
        generatePreview()
    }

    fun toggleFaceAware(enabled: Boolean) {
        if (_uiState.value.isCommitting) return
        invalidatePublishedPreview()
        savedStateHandle[KEY_FACE_AWARE] = enabled
        _uiState.update { it.copy(faceAwareEnabled = enabled, manualFocusPoint = null) }

        cancelFaceAnalysis()
        generatePreview()
    }

    fun updateManualFocusPoint(point: com.wallpapercropfixer.domain.model.FocusPoint?) {
        if (_uiState.value.isCommitting) return
        invalidatePublishedPreview()
        cancelFaceAnalysis()
        _uiState.update { it.copy(manualFocusPoint = point, faceAwareEnabled = false) }
        savedStateHandle[KEY_FACE_AWARE] = false
        if (point != null) {
            savedStateHandle[KEY_FOCUS_X] = point.xNormalized
            savedStateHandle[KEY_FOCUS_Y] = point.yNormalized
        } else {
            savedStateHandle.remove<Float>(KEY_FOCUS_X)
            savedStateHandle.remove<Float>(KEY_FOCUS_Y)
        }
        generatePreview()
    }

    /** Select which of the two renders (BOTH target) is displayed. Never triggers a render. */
    fun setViewingLock(viewingLock: Boolean) {
        _uiState.update { it.copy(previewingLock = viewingLock) }
    }

    fun resetToDefaults() {
        if (_uiState.value.isCommitting) return
        viewModelScope.launch {
            val settings = runCatching { settingsRepository.observeSettings().first() }.getOrDefault(UserSettings())
            if (_uiState.value.isCommitting) return@launch
            cancelFaceAnalysis()
            invalidatePublishedPreview()
            savedStateHandle[KEY_CROP_MODE] = settings.defaultCropMode.name
            savedStateHandle[KEY_TARGET] = settings.defaultWallpaperTarget.name
            savedStateHandle[KEY_FILL_MODE] = settings.defaultBackgroundFillMode.name
            savedStateHandle[KEY_FACE_AWARE] = settings.defaultFaceAwareEnabled
            savedStateHandle.remove<Float>(KEY_FOCUS_X)
            savedStateHandle.remove<Float>(KEY_FOCUS_Y)
            _uiState.update {
                it.copy(
                    cropMode = settings.defaultCropMode,
                    wallpaperTarget = settings.defaultWallpaperTarget,
                    backgroundFillMode = settings.defaultBackgroundFillMode,
                    faceAwareEnabled = settings.defaultFaceAwareEnabled,
                    manualFocusPoint = null
                )
            }
            generatePreview()
        }
    }

    /** Re-runs the render for the current selection after a failure. */
    fun retryRender() {
        if (_uiState.value.sourceImageMeta == null || _uiState.value.isBusy) return
        if (configurationRefreshFailed) {
            refreshForConfigurationChange()
            return
        }
        generatePreview()
    }

    /**
     * Re-resolves device metrics (orientation / window changes) and regenerates the
     * preview against the current selection.
     */
    fun refreshForConfigurationChange() {
        synchronized(configurationRefreshLock) {
            if (_uiState.value.isCommitting) {
                pendingConfigurationRefresh = true
                return
            }
            pendingConfigurationRefresh = false
            if (_uiState.value.sourceImageMeta == null || _uiState.value.isLoading) return
            if (configurationRefreshInFlight) {
                pendingConfigurationRefresh = true
                return
            }
            configurationRefreshInFlight = true
            configurationRefreshFailed = false
            invalidatePublishedPreview()
        }
        startConfigurationRefresh()
    }

    /** Starts a configuration refresh after its publication gate has been closed. */
    private fun startConfigurationRefresh(preserveOutcome: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val deviceProfile = getDeviceProfile()
                val behaviorProfile = resolveBehavior(deviceProfile)
                var requery = false
                withContext(Dispatchers.Main.immediate) {
                    synchronized(configurationRefreshLock) {
                        if (_uiState.value.isCommitting) {
                            pendingConfigurationRefresh = true
                            configurationRefreshInFlight = false
                        } else if (pendingConfigurationRefresh) {
                            // A newer configuration event arrived while this lookup was
                            // suspended. Re-query before publishing any geometry.
                            pendingConfigurationRefresh = false
                            requery = true
                        } else {
                            _uiState.update { it.copy(deviceProfile = deviceProfile, behaviorProfile = behaviorProfile) }
                            configurationRefreshInFlight = false
                            configurationRefreshFailed = false
                            generatePreview(preserveOutcome = preserveOutcome)
                        }
                    }
                }
                if (requery) startConfigurationRefresh(preserveOutcome)
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                synchronized(configurationRefreshLock) {
                    pendingConfigurationRefresh = false
                    configurationRefreshInFlight = false
                    configurationRefreshFailed = true
                    _uiState.update { it.copy(isRendering = false, renderFailed = true) }
                }
                Logger.e("refreshForConfigurationChange failed", t)
            }
        }
    }

    fun generatePreview(preserveOutcome: Boolean = false) {
        if (_uiState.value.isCommitting || configurationRefreshInFlight) return
        if (configurationRefreshFailed) {
            refreshForConfigurationChange()
            return
        }
        val state = _uiState.value
        val source = state.sourceImageMeta ?: return
        val device = state.deviceProfile ?: return
        val behavior = state.behaviorProfile ?: return

        if (state.faceAwareEnabled && state.faceDetectionStatus == FaceDetectionStatus.NOT_RUN) {
            analyzeThenRender(state.imageUri ?: return)
            return
        }

        val generation = previewGeneration.incrementAndGet()
        previewJob?.cancel()

        // Disable apply/save immediately and atomically with the option change.
        val revision = publishedRevision.incrementAndGet()
        _uiState.update {
            it.copy(
                isRendering = true,
                publishedPreview = null,
                // Keep the last committed render visible; it stays display-only
                // because publishedPreview is null until this render publishes.
                retainedPreview = it.publishedPreview ?: it.retainedPreview,
                renderFailed = false,
                errorMessage = if (preserveOutcome) it.errorMessage else null,
                successMessage = if (preserveOutcome) it.successMessage else null
            )
        }

        // Capture every render input as an immutable snapshot before launching work.
        val isBoth = state.wallpaperTarget == WallpaperTarget.BOTH
        val homeRequest = WallpaperRenderRequest(
            source = source,
            deviceProfile = device,
            behaviorProfile = behavior,
            target = if (isBoth) WallpaperTarget.HOME else state.wallpaperTarget,
            cropMode = state.cropMode,
            backgroundFillMode = state.backgroundFillMode,
            manualFocusPoint = state.manualFocusPoint,
            enableFaceAwareFocus = state.faceAwareEnabled
        )
        val lockRequest = if (isBoth) homeRequest.copy(target = WallpaperTarget.LOCK) else null
        val analysisSnapshot = state.subjectAnalysis

        previewJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                val homePlan = buildRenderPlan(homeRequest, analysisSnapshot)
                val homeBitmap = renderBitmap(homeRequest, homePlan)

                val lockPlan = if (lockRequest != null) buildRenderPlan(lockRequest, analysisSnapshot) else null
                val lockBitmap = if (lockRequest != null && lockPlan != null) {
                    renderBitmap(lockRequest, lockPlan)
                } else {
                    null
                }

                val published = PublishedPreview(
                    revision = revision,
                    target = state.wallpaperTarget,
                    home = RenderedPreview(homeRequest, homePlan, homeBitmap),
                    lock = if (lockRequest != null && lockPlan != null && lockBitmap != null) {
                        RenderedPreview(lockRequest, lockPlan, lockBitmap)
                    } else {
                        null
                    }
                )
                _uiState.update {
                    if (generation != previewGeneration.get()) {
                        it
                    } else {
                        it.copy(
                            isRendering = false,
                            publishedPreview = published,
                            retainedPreview = published,
                            renderFailed = false
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                if (isCurrentPreviewGeneration(generation)) {
                    // The retained display stays on screen; eligibility is NOT restored.
                    _uiState.update { it.copy(isRendering = false, renderFailed = true) }
                }
                Logger.e("generatePreview failed", t)
            }
        }
    }

    private fun cancelFaceAnalysis() {
        faceAnalysisGeneration.incrementAndGet()
        faceAnalysisJob?.cancel()
        faceAnalysisJob = null
    }

    private fun analyzeThenRender(uri: String) {
        if (faceAnalysisJob?.isActive == true) return
        val generation = faceAnalysisGeneration.incrementAndGet()
        val imageGeneration = loadGeneration
        _uiState.update { it.copy(isRendering = true) }
        faceAnalysisJob = viewModelScope.launch(Dispatchers.IO) {
            val analysis = try {
                analyzeSubject(uri)
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                Logger.e("Face detection failed", t)
                null
            }
            withContext(Dispatchers.Main.immediate) {
                if (generation != faceAnalysisGeneration.get() || imageGeneration != loadGeneration ||
                    !_uiState.value.faceAwareEnabled || _uiState.value.imageUri != uri) return@withContext
                _uiState.update {
                    it.copy(subjectAnalysis = analysis, faceDetectionStatus = when {
                        analysis == null -> FaceDetectionStatus.FAILED
                        analysis.faces.isEmpty() -> FaceDetectionStatus.NO_FACES
                        else -> FaceDetectionStatus.DETECTED
                    })
                }
                generatePreview()
            }
        }
    }

    fun exportWallpaper(quality: Int? = null) {
        val state = _uiState.value
        val published = state.publishedPreview ?: return
        if (state.isBusy) return

        val operationToken = ++exportOperationToken
        val operationGeneration = previewGeneration.get()
        val operationRevision = published.revision

        _uiState.update {
            it.copy(isExporting = true, errorMessage = null, successMessage = null)
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val effectiveQuality = quality
                    ?: runCatching { settingsRepository.observeSettings().first().exportJpegQuality }.getOrDefault(92)

                // When the target is LOCK the single published bitmap lives in
                // `published.home`, but the saved file must still be named wcf_lock_.
                // BOTH keeps exporting two files: wcf_home_ + wcf_lock_.
                val homePrefix = if (published.target == WallpaperTarget.LOCK) "wcf_lock" else "wcf_home"
                suspend fun save(preview: RenderedPreview, prefix: String): Result<ExportResult> = try {
                    Result.success(exportWallpaper(preview.bitmap, FileNameFactory.wallpaperFileName(prefix),
                        effectiveQuality.coerceIn(0, 100)))
                } catch (e: CancellationException) {
                    throw e
                } catch (t: Throwable) {
                    Logger.e("Export failed for $prefix", t)
                    Result.failure(t)
                }
                val homeExport = save(published.home, homePrefix)
                val lockExport = published.lock?.let { save(it, "wcf_lock") }
                val home = homeExport.getOrNull()
                val lock = lockExport?.getOrNull()
                val error = when {
                    lockExport == null && home == null -> R.string.error_export
                    lockExport != null && home == null && lock == null -> R.string.error_export
                    lockExport != null && home == null -> R.string.export_only_lock_saved
                    lockExport != null && lock == null -> R.string.export_only_home_saved
                    else -> null
                }
                finishExport(
                    operationToken, operationRevision, operationGeneration,
                    successMessage = if (error == null && home != null) exportSuccessMessage(home, lock) else null,
                    errorMessage = error?.let { UiMessage(it) }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                finishExport(
                    operationToken,
                    operationRevision,
                    operationGeneration,
                    errorMessage = UiMessage(R.string.error_export)
                )
                Logger.e("exportWallpaper failed", t)
            }
        }
    }

    fun applyWallpaper() {
        val state = _uiState.value
        val published = state.publishedPreview ?: return
        if (state.isBusy) return

        val operationToken = ++applyOperationToken
        val operationGeneration = previewGeneration.get()
        val operationRevision = published.revision
        val target = published.target

        _uiState.update {
            it.copy(isApplying = true, errorMessage = null, successMessage = null)
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = if (target == WallpaperTarget.BOTH && published.lock != null) {
                    val homeResult = applyWallpaper(published.home.bitmap, WallpaperTarget.HOME)
                    val lockResult = applyWallpaper(published.lock.bitmap, WallpaperTarget.LOCK)
                    when {
                        homeResult.isSuccess && lockResult.isSuccess -> Result.success(Unit)
                        homeResult.isFailure && lockResult.isFailure ->
                            Result.failure(BothScreensApplyFailedException())
                        homeResult.isFailure ->
                            Result.failure(HomeScreenApplyFailedException())
                        else ->
                            Result.failure(LockScreenApplyFailedException())
                    }
                } else {
                    applyWallpaper(published.home.bitmap, target)
                }

                result
                    .onSuccess {
                        finishApply(
                            operationToken,
                            operationRevision,
                            operationGeneration,
                            successMessage = UiMessage(appliedRes(target))
                        )
                    }
                    .onFailure { t ->
                        finishApply(
                            operationToken,
                            operationRevision,
                            operationGeneration,
                            errorMessage = UiMessage(applyErrorRes(t))
                        )
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                finishApply(
                    operationToken,
                    operationRevision,
                    operationGeneration,
                    errorMessage = UiMessage(R.string.error_apply_generic)
                )
                Logger.e("applyWallpaper failed", t)
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
        refreshPendingConfiguration()
    }
    fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null) }
        refreshPendingConfiguration()
    }

    private fun refreshPendingConfiguration() {
        val shouldRefresh = synchronized(configurationRefreshLock) {
            pendingConfigurationRefresh && !_uiState.value.isCommitting
        }
        if (shouldRefresh) refreshForConfigurationChange()
    }

    private fun isCurrentPreviewGeneration(generation: Int): Boolean = generation == previewGeneration.get()

    /**
     * Closes the publication window before a mutable editor option changes. The last
     * committed render stays visible (retained) but loses Apply/Save eligibility, and
     * the Home/Lock viewing tab is preserved for same-image option changes.
     */
    private fun invalidatePublishedPreview() {
        previewGeneration.incrementAndGet()
        previewJob?.cancel()
        _uiState.update { state ->
            state.copy(
                publishedPreview = null,
                isRendering = state.sourceImageMeta != null &&
                    state.deviceProfile != null &&
                    state.behaviorProfile != null,
                errorMessage = null,
                successMessage = null,
                renderFailed = false
            )
        }
    }

    private fun finishExport(
        operationToken: Long,
        operationRevision: Long,
        operationGeneration: Int,
        successMessage: UiMessage? = null,
        errorMessage: UiMessage? = null
    ) {
        var refreshQueued = false
        synchronized(configurationRefreshLock) {
            if (operationToken != exportOperationToken) return
            refreshQueued = pendingConfigurationRefresh
            if (refreshQueued) configurationRefreshInFlight = true
            _uiState.update { state ->
                val stillAuthoritative = operationGeneration == previewGeneration.get() &&
                    state.publishedPreview?.revision == operationRevision
                state.copy(
                    isExporting = false,
                    // A queued configuration change invalidates the old geometry at
                    // the same transition that publication work becomes editable.
                    publishedPreview = if (refreshQueued) null else state.publishedPreview,
                    isRendering = if (refreshQueued) {
                        state.sourceImageMeta != null && state.deviceProfile != null &&
                            state.behaviorProfile != null
                    } else {
                        state.isRendering
                    },
                    successMessage = if (stillAuthoritative) successMessage else state.successMessage,
                    errorMessage = if (stillAuthoritative) errorMessage else state.errorMessage
                )
            }
            pendingConfigurationRefresh = false
        }
        if (refreshQueued) startConfigurationRefresh(preserveOutcome = true)
    }

    private fun finishApply(
        operationToken: Long,
        operationRevision: Long,
        operationGeneration: Int,
        successMessage: UiMessage? = null,
        errorMessage: UiMessage? = null
    ) {
        var refreshQueued = false
        synchronized(configurationRefreshLock) {
            if (operationToken != applyOperationToken) return
            refreshQueued = pendingConfigurationRefresh
            if (refreshQueued) configurationRefreshInFlight = true
            _uiState.update { state ->
                val stillAuthoritative = operationGeneration == previewGeneration.get() &&
                    state.publishedPreview?.revision == operationRevision
                state.copy(
                    isApplying = false,
                    // A queued configuration change invalidates the old geometry at
                    // the same transition that publication work becomes editable.
                    publishedPreview = if (refreshQueued) null else state.publishedPreview,
                    isRendering = if (refreshQueued) {
                        state.sourceImageMeta != null && state.deviceProfile != null &&
                            state.behaviorProfile != null
                    } else {
                        state.isRendering
                    },
                    successMessage = if (stillAuthoritative) successMessage else state.successMessage,
                    errorMessage = if (stillAuthoritative) errorMessage else state.errorMessage
                )
            }
            pendingConfigurationRefresh = false
        }
        if (refreshQueued) startConfigurationRefresh(preserveOutcome = true)
    }

    private fun appliedRes(target: WallpaperTarget): Int = when (target) {
        WallpaperTarget.HOME -> R.string.editor_applied_home
        WallpaperTarget.LOCK -> R.string.editor_applied_lock
        WallpaperTarget.BOTH -> R.string.editor_applied_both
    }

    private fun applyErrorRes(t: Throwable): Int = when (t) {
        is WallpaperUnsupportedException -> R.string.error_apply_unsupported
        is WallpaperPolicyDisallowedException -> R.string.error_apply_policy
        is WallpaperSetFailedException -> R.string.error_apply_device
        is BothScreensApplyFailedException -> R.string.error_apply_both
        is HomeScreenApplyFailedException -> R.string.error_apply_home_failed
        is LockScreenApplyFailedException -> R.string.error_apply_lock_failed
        else -> R.string.error_apply_generic
    }

    private fun exportSuccessMessage(home: ExportResult, lock: ExportResult?): UiMessage {
        if (lock != null && lock.destination != home.destination) return UiMessage(R.string.export_saved_mixed)
        val isBoth = lock != null
        val res = when (home.destination) {
            ExportDestination.MEDIA_STORE ->
                if (isBoth) R.string.export_saved_media_store_both else R.string.export_saved_media_store
            ExportDestination.APP_EXTERNAL_FILES ->
                if (isBoth) R.string.export_saved_app_external_both else R.string.export_saved_app_external
            ExportDestination.APP_INTERNAL_FILES ->
                if (isBoth) R.string.export_saved_app_internal_both else R.string.export_saved_app_internal
        }
        return UiMessage(res)
    }

    companion object {
        private const val KEY_CROP_MODE = "editor_crop_mode"
        private const val KEY_TARGET = "editor_target"
        private const val KEY_FILL_MODE = "editor_fill_mode"
        private const val KEY_FACE_AWARE = "editor_face_aware"
        private const val KEY_FOCUS_X = "editor_focus_x"
        private const val KEY_FOCUS_Y = "editor_focus_y"
    }
}
