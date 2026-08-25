package com.wallpapercropfixer.presentation.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wallpapercropfixer.core.util.FileNameFactory
import com.wallpapercropfixer.core.util.Logger
import com.wallpapercropfixer.domain.model.BackgroundFillMode
import com.wallpapercropfixer.domain.model.CropMode
import com.wallpapercropfixer.domain.model.FocusPoint
import com.wallpapercropfixer.domain.model.WallpaperRenderRequest
import com.wallpapercropfixer.domain.model.WallpaperTarget
import com.wallpapercropfixer.domain.model.UserSettings
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

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
    private val applyWallpaper: ApplyWallpaperUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private var previewJob: Job? = null

    init {
        viewModelScope.launch {
            val settings = runCatching { settingsRepository.observeSettings().first() }.getOrDefault(UserSettings())
            _uiState.update {
                it.copy(
                    cropMode = settings.defaultCropMode,
                    wallpaperTarget = settings.defaultWallpaperTarget,
                    backgroundFillMode = settings.defaultBackgroundFillMode,
                    faceAwareEnabled = settings.defaultFaceAwareEnabled
                )
            }
        }
    }

    fun loadImage(uri: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, imageUri = uri, errorMessage = null) }
            runCatching {
                val meta = imageRepository.readImageMeta(uri)
                val deviceProfile = getDeviceProfile()
                val behaviorProfile = resolveBehavior(deviceProfile)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        sourceImageMeta = meta,
                        deviceProfile = deviceProfile,
                        behaviorProfile = behaviorProfile
                    )
                }

                if (_uiState.value.faceAwareEnabled) {
                    val analysis = runCatching { analyzeSubject(uri) }
                        .onFailure { Logger.e("Face detection failed — continuing without it", it) }
                        .getOrNull()
                    _uiState.update { it.copy(subjectAnalysis = analysis) }
                }
                generatePreview()
            }.onFailure { t ->
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Failed to load image: ${t.message}")
                }
                Logger.e("loadImage failed", t)
            }
        }
    }

    fun setCropMode(mode: CropMode) {
        _uiState.update { it.copy(cropMode = mode) }
        generatePreview()
    }

    fun setWallpaperTarget(target: WallpaperTarget) {
        _uiState.update { it.copy(wallpaperTarget = target, previewingLock = false) }
        generatePreview()
    }

    fun setBackgroundFillMode(mode: BackgroundFillMode) {
        _uiState.update { it.copy(backgroundFillMode = mode) }
        generatePreview()
    }

    fun toggleFaceAware(enabled: Boolean) {
        _uiState.update { it.copy(faceAwareEnabled = enabled, manualFocusPoint = null) }
        generatePreview()
    }

    fun updateManualFocusPoint(point: FocusPoint?) {
        _uiState.update { it.copy(manualFocusPoint = point, faceAwareEnabled = false) }
        generatePreview()
    }

    /** Flip the preview frame between HOME and LOCK when target == BOTH. */
    fun togglePreviewTarget() {
        _uiState.update { it.copy(previewingLock = !it.previewingLock) }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            val settings = runCatching { settingsRepository.observeSettings().first() }.getOrDefault(UserSettings())
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

    fun generatePreview() {
        val state = _uiState.value
        val source = state.sourceImageMeta ?: return
        val device = state.deviceProfile ?: return
        val behavior = state.behaviorProfile ?: return

        previewJob?.cancel()
        previewJob = viewModelScope.launch(Dispatchers.Default) {
            _uiState.update { it.copy(isRendering = true, errorMessage = null) }

            runCatching {
                val isBoth = state.wallpaperTarget == WallpaperTarget.BOTH

                // Build the HOME (or single-target) request
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

                val homePlan = buildRenderPlan(homeRequest, state.subjectAnalysis)

                if (isBoth) {
                    // Render HOME and LOCK sequentially or concurrently
                    val lockRequest = homeRequest.copy(target = WallpaperTarget.LOCK)
                    val lockPlan = buildRenderPlan(lockRequest, state.subjectAnalysis)

                    val homeDeferred = async { renderBitmap(homeRequest, homePlan) }
                    val lockDeferred = async { renderBitmap(lockRequest, lockPlan) }

                    val homeBitmap = homeDeferred.await()
                    val lockBitmap = lockDeferred.await()

                    _uiState.update { current ->
                        current.previewBitmap?.takeIf { !it.isRecycled }?.recycle()
                        current.lockPreviewBitmap?.takeIf { !it.isRecycled }?.recycle()
                        current.copy(
                            isRendering = false,
                            renderPlan = homePlan,
                            previewBitmap = homeBitmap,
                            lockPreviewBitmap = lockBitmap,
                            previewRevision = current.previewRevision + 1
                        )
                    }
                } else {
                    val bitmap = renderBitmap(homeRequest, homePlan)
                    _uiState.update { current ->
                        current.previewBitmap?.takeIf { !it.isRecycled }?.recycle()
                        current.lockPreviewBitmap?.takeIf { !it.isRecycled }?.recycle()
                        current.copy(
                            isRendering = false,
                            renderPlan = homePlan,
                            previewBitmap = bitmap,
                            lockPreviewBitmap = null,
                            previewingLock = false,
                            previewRevision = current.previewRevision + 1
                        )
                    }
                }
            }.onFailure { t ->
                _uiState.update {
                    it.copy(isRendering = false, errorMessage = "Preview failed: ${t.message}")
                }
                Logger.e("generatePreview failed", t)
            }
        }
    }

    fun exportWallpaper(quality: Int? = null) {
        val state = _uiState.value
        val homeBitmap = state.previewBitmap ?: return

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isRendering = true, errorMessage = null) }
            runCatching {
                val effectiveQuality = quality
                    ?: runCatching { settingsRepository.observeSettings().first().exportJpegQuality }.getOrDefault(92)

                exportWallpaper(homeBitmap, FileNameFactory.wallpaperFileName("wcf_home"), effectiveQuality)

                // Also export lock bitmap if available
                state.lockPreviewBitmap?.let { lockBitmap ->
                    exportWallpaper(lockBitmap, FileNameFactory.wallpaperFileName("wcf_lock"), effectiveQuality)
                }

                _uiState.update {
                    it.copy(
                        isRendering = false,
                        successMessage = "Saved to Pictures/WallpaperCropFixer"
                    )
                }
            }.onFailure { t ->
                _uiState.update {
                    it.copy(isRendering = false, errorMessage = "Export failed: ${t.message}")
                }
            }
        }
    }

    fun applyWallpaper() {
        val state = _uiState.value
        val homeBitmap = state.previewBitmap ?: return
        val target = state.wallpaperTarget

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isRendering = true, errorMessage = null, successMessage = null) }

            val result = if (target == WallpaperTarget.BOTH && state.lockPreviewBitmap != null) {
                val homeResult = applyWallpaper(homeBitmap, WallpaperTarget.HOME)
                val lockResult = applyWallpaper(state.lockPreviewBitmap, WallpaperTarget.LOCK)
                if (homeResult.isSuccess && lockResult.isSuccess) {
                    Result.success(Unit)
                } else if (homeResult.isFailure && lockResult.isFailure) {
                    Result.failure(Exception("Failed to apply wallpaper to home and lock screens: ${homeResult.exceptionOrNull()?.message}"))
                } else if (homeResult.isFailure) {
                    Result.failure(Exception("Failed to apply home screen wallpaper: ${homeResult.exceptionOrNull()?.message} (Lock screen succeeded)"))
                } else {
                    Result.failure(Exception("Failed to apply lock screen wallpaper: ${lockResult.exceptionOrNull()?.message} (Home screen succeeded)"))
                }
            } else {
                applyWallpaper(homeBitmap, target)
            }

            result
                .onSuccess {
                    val label = when (target) {
                        WallpaperTarget.HOME -> "home screen"
                        WallpaperTarget.LOCK -> "lock screen"
                        WallpaperTarget.BOTH -> "home & lock screen"
                    }
                    _uiState.update {
                        it.copy(isRendering = false, successMessage = "Wallpaper applied to $label")
                    }
                }
                .onFailure { t ->
                    _uiState.update {
                        it.copy(isRendering = false, errorMessage = "Apply failed: ${t.message}")
                    }
                }
        }
    }

    fun clearError() { _uiState.update { it.copy(errorMessage = null) } }
    fun clearSuccess() { _uiState.update { it.copy(successMessage = null) } }
}
