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
import javax.inject.Inject

/**
 * Owns the editor state and the render/load pipeline.
 *
 * Concurrency invariant: a result generated for request N may only publish state
 * while N is still the latest request. All rendering inputs are captured into an
 * immutable snapshot before any asynchronous work begins, and every publish is
 * guarded by a monotonically increasing generation token. This makes the preview,
 * the selected options, and any applied/exported bitmap always correspond to the
 * same render generation. Bitmaps that are published to the UI are intentionally
 * NOT manually recycled — Compose and the apply/export paths may reference them,
 * and GC reclaims replaced previews safely.
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

    @Volatile
    private var previewGeneration = 0

    @Volatile
    private var loadGeneration = 0

    private var previewJob: Job? = null
    private var loadJob: Job? = null

    init {
        // Restore in-session options from process-death state synchronously (primitives only).
        restoreOptionsFromSavedState()

        viewModelScope.launch {
            val settings = runCatching { settingsRepository.observeSettings().first() }.getOrDefault(UserSettings())
            // SavedStateHandle (an in-session change) wins over DataStore defaults.
            _uiState.update { current ->
                current.copy(
                    cropMode = savedStateHandle.get<String>(KEY_CROP_MODE)
                        ?.let { runCatching { CropMode.valueOf(it) }.getOrNull() }
                        ?: settings.defaultCropMode,
                    wallpaperTarget = savedStateHandle.get<String>(KEY_TARGET)
                        ?.let { runCatching { WallpaperTarget.valueOf(it) }.getOrNull() }
                        ?: settings.defaultWallpaperTarget,
                    backgroundFillMode = savedStateHandle.get<String>(KEY_FILL_MODE)
                        ?.let { runCatching { BackgroundFillMode.valueOf(it) }.getOrNull() }
                        ?: settings.defaultBackgroundFillMode,
                    faceAwareEnabled = savedStateHandle.get<Boolean>(KEY_FACE_AWARE)
                        ?: settings.defaultFaceAwareEnabled
                )
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
    }

    fun loadImage(uri: String) {
        val current = _uiState.value
        if (current.imageUri == uri && current.sourceImageMeta != null && !current.isLoading) {
            return
        }

        val gen = ++loadGeneration
        savedStateHandle[KEY_IMAGE_URI] = uri
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
                    renderPlan = null,
                    previewBitmap = null,
                    lockPreviewBitmap = null,
                    manualFocusPoint = null,
                    previewingLock = false
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
                        sourceImageMeta = meta,
                        deviceProfile = deviceProfile,
                        behaviorProfile = behaviorProfile
                    )
                }

                if (_uiState.value.faceAwareEnabled) {
                    val analysis = try {
                        analyzeSubject(uri)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (t: Throwable) {
                        Logger.e("Face detection failed — continuing without it", t)
                        null
                    }
                    if (gen != loadGeneration) return@launch
                    val status = when {
                        analysis == null -> FaceDetectionStatus.FAILED
                        analysis.faces.isEmpty() -> FaceDetectionStatus.NO_FACES
                        else -> FaceDetectionStatus.DETECTED
                    }
                    _uiState.update {
                        it.copy(subjectAnalysis = analysis, faceDetectionStatus = status)
                    }
                }
                generatePreview()
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
        savedStateHandle[KEY_CROP_MODE] = mode.name
        _uiState.update { it.copy(cropMode = mode) }
        generatePreview()
    }

    fun setWallpaperTarget(target: WallpaperTarget) {
        savedStateHandle[KEY_TARGET] = target.name
        _uiState.update { it.copy(wallpaperTarget = target, previewingLock = false) }
        generatePreview()
    }

    fun setBackgroundFillMode(mode: BackgroundFillMode) {
        savedStateHandle[KEY_FILL_MODE] = mode.name
        _uiState.update { it.copy(backgroundFillMode = mode) }
        generatePreview()
    }

    fun toggleFaceAware(enabled: Boolean) {
        savedStateHandle[KEY_FACE_AWARE] = enabled
        _uiState.update { it.copy(faceAwareEnabled = enabled, manualFocusPoint = null) }

        if (enabled && _uiState.value.sourceImageMeta != null && _uiState.value.subjectAnalysis == null) {
            val uri = _uiState.value.imageUri
            val gen = loadGeneration
            if (uri != null) {
                viewModelScope.launch(Dispatchers.IO) {
                    val analysis = try {
                        analyzeSubject(uri)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (t: Throwable) {
                        Logger.e("Face detection failed", t)
                        null
                    }
                    if (gen == loadGeneration && _uiState.value.imageUri == uri) {
                        _uiState.update {
                            it.copy(
                                subjectAnalysis = analysis,
                                faceDetectionStatus = when {
                                    analysis == null -> FaceDetectionStatus.FAILED
                                    analysis.faces.isEmpty() -> FaceDetectionStatus.NO_FACES
                                    else -> FaceDetectionStatus.DETECTED
                                }
                            )
                        }
                        generatePreview()
                    }
                }
            }
        }
        generatePreview()
    }

    fun updateManualFocusPoint(point: com.wallpapercropfixer.domain.model.FocusPoint?) {
        _uiState.update { it.copy(manualFocusPoint = point, faceAwareEnabled = false) }
        savedStateHandle[KEY_FACE_AWARE] = false
        generatePreview()
    }

    /** Flip the preview frame between HOME and LOCK when target == BOTH. */
    fun togglePreviewTarget() {
        _uiState.update { it.copy(previewingLock = !it.previewingLock) }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            val settings = runCatching { settingsRepository.observeSettings().first() }.getOrDefault(UserSettings())
            savedStateHandle[KEY_CROP_MODE] = settings.defaultCropMode.name
            savedStateHandle[KEY_TARGET] = settings.defaultWallpaperTarget.name
            savedStateHandle[KEY_FILL_MODE] = settings.defaultBackgroundFillMode.name
            savedStateHandle[KEY_FACE_AWARE] = settings.defaultFaceAwareEnabled
            _uiState.update {
                it.copy(
                    cropMode = settings.defaultCropMode,
                    wallpaperTarget = settings.defaultWallpaperTarget,
                    backgroundFillMode = settings.defaultBackgroundFillMode,
                    faceAwareEnabled = settings.defaultFaceAwareEnabled,
                    manualFocusPoint = null,
                    previewingLock = false
                )
            }
            generatePreview()
        }
    }

    /**
     * Re-resolves device metrics (orientation / window changes) and regenerates the
     * preview against the current selection.
     */
    fun refreshForConfigurationChange() {
        if (_uiState.value.sourceImageMeta == null || _uiState.value.isLoading) return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val deviceProfile = getDeviceProfile()
                val behaviorProfile = resolveBehavior(deviceProfile)
                _uiState.update { it.copy(deviceProfile = deviceProfile, behaviorProfile = behaviorProfile) }
                generatePreview()
            }.onFailure { Logger.e("refreshForConfigurationChange failed", it) }
        }
    }

    fun generatePreview() {
        val state = _uiState.value
        val source = state.sourceImageMeta ?: return
        val device = state.deviceProfile ?: return
        val behavior = state.behaviorProfile ?: return

        val generation = ++previewGeneration
        previewJob?.cancel()

        // Disable apply/save immediately and atomically with the option change.
        _uiState.update { it.copy(isRendering = true, errorMessage = null) }

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

                if (!isCurrentPreviewGeneration(generation)) {
                    // This result is stale — discard it and its resources without touching state.
                    homeBitmap.recycle()
                    lockBitmap?.recycle()
                    return@launch
                }

                _uiState.update {
                    it.copy(
                        isRendering = false,
                        renderPlan = homePlan,
                        previewBitmap = homeBitmap,
                        lockPreviewBitmap = lockBitmap,
                        previewingLock = false
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                if (isCurrentPreviewGeneration(generation)) {
                    _uiState.update {
                        it.copy(isRendering = false, errorMessage = UiMessage(R.string.error_preview))
                    }
                }
                Logger.e("generatePreview failed", t)
            }
        }
    }

    fun exportWallpaper(quality: Int? = null) {
        val state = _uiState.value
        val homeBitmap = state.previewBitmap ?: return
        if (state.isRendering) return

        _uiState.update { it.copy(isRendering = true, errorMessage = null, successMessage = null) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val effectiveQuality = quality
                    ?: runCatching { settingsRepository.observeSettings().first().exportJpegQuality }.getOrDefault(92)

                val homeExport = exportWallpaper(homeBitmap, FileNameFactory.wallpaperFileName("wcf_home"), effectiveQuality)
                val lockExport = state.lockPreviewBitmap?.let {
                    exportWallpaper(it, FileNameFactory.wallpaperFileName("wcf_lock"), effectiveQuality)
                }

                _uiState.update {
                    it.copy(isRendering = false, successMessage = exportSuccessMessage(homeExport, lockExport))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(isRendering = false, errorMessage = UiMessage(R.string.error_export))
                }
                Logger.e("exportWallpaper failed", t)
            }
        }
    }

    fun applyWallpaper() {
        val state = _uiState.value
        val homeBitmap = state.previewBitmap ?: return
        if (state.isRendering) return

        val target = state.wallpaperTarget
        val lockBitmap = state.lockPreviewBitmap

        _uiState.update { it.copy(isRendering = true, errorMessage = null, successMessage = null) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = if (target == WallpaperTarget.BOTH && lockBitmap != null) {
                    val homeResult = applyWallpaper(homeBitmap, WallpaperTarget.HOME)
                    val lockResult = applyWallpaper(lockBitmap, WallpaperTarget.LOCK)
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
                    applyWallpaper(homeBitmap, target)
                }

                result
                    .onSuccess {
                        _uiState.update {
                            it.copy(isRendering = false, successMessage = UiMessage(appliedRes(target)))
                        }
                    }
                    .onFailure { t ->
                        _uiState.update {
                            it.copy(isRendering = false, errorMessage = UiMessage(applyErrorRes(t)))
                        }
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(isRendering = false, errorMessage = UiMessage(R.string.error_apply_generic))
                }
                Logger.e("applyWallpaper failed", t)
            }
        }
    }

    fun clearError() { _uiState.update { it.copy(errorMessage = null) } }
    fun clearSuccess() { _uiState.update { it.copy(successMessage = null) } }

    private fun isCurrentPreviewGeneration(generation: Int): Boolean = generation == previewGeneration

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
        private const val KEY_IMAGE_URI = "editor_image_uri"
        private const val KEY_CROP_MODE = "editor_crop_mode"
        private const val KEY_TARGET = "editor_target"
        private const val KEY_FILL_MODE = "editor_fill_mode"
        private const val KEY_FACE_AWARE = "editor_face_aware"
    }
}