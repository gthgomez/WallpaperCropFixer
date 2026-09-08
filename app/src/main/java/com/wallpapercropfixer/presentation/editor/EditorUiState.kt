package com.wallpapercropfixer.presentation.editor

import android.graphics.Bitmap
import androidx.annotation.StringRes
import com.wallpapercropfixer.domain.model.BackgroundFillMode
import com.wallpapercropfixer.domain.model.CropMode
import com.wallpapercropfixer.domain.model.DeviceProfile
import com.wallpapercropfixer.domain.model.FocusPoint
import com.wallpapercropfixer.domain.model.SourceImageMeta
import com.wallpapercropfixer.domain.model.SubjectAnalysis
import com.wallpapercropfixer.domain.model.WallpaperBehaviorProfile
import com.wallpapercropfixer.domain.model.WallpaperRenderPlan
import com.wallpapercropfixer.domain.model.WallpaperRenderRequest
import com.wallpapercropfixer.domain.model.WallpaperTarget

/** The immutable render inputs and outputs committed as one authoritative revision. */
data class RenderedPreview(
    val request: WallpaperRenderRequest,
    val plan: WallpaperRenderPlan,
    val bitmap: Bitmap
)

data class PublishedPreview(
    val revision: Long,
    val target: WallpaperTarget,
    val home: RenderedPreview,
    val lock: RenderedPreview? = null
)

enum class FaceDetectionStatus { NOT_RUN, DETECTED, NO_FACES, FAILED }

/**
 * A user-facing message expressed as a string resource so the ViewModel never
 * hardcodes user-visible text and the UI resolves the correct locale.
 */
data class UiMessage(@StringRes val resId: Int, val formatArgs: List<Any> = emptyList())

data class EditorUiState(
    val isLoading: Boolean = false,
    val imageUri: String? = null,
    val sourceImageMeta: SourceImageMeta? = null,
    val deviceProfile: DeviceProfile? = null,
    val behaviorProfile: WallpaperBehaviorProfile? = null,
    val cropMode: CropMode = CropMode.BALANCED,
    val wallpaperTarget: WallpaperTarget = WallpaperTarget.HOME,
    val backgroundFillMode: BackgroundFillMode = BackgroundFillMode.BLUR,
    val faceAwareEnabled: Boolean = true,
    val manualFocusPoint: FocusPoint? = null,
    val subjectAnalysis: SubjectAnalysis? = null,
    val faceDetectionStatus: FaceDetectionStatus = FaceDetectionStatus.NOT_RUN,
    val publishedPreview: PublishedPreview? = null,
    // Which of the two bitmaps the user is currently viewing in the frame
    val previewingLock: Boolean = false,
    val isRendering: Boolean = false,
    val isApplying: Boolean = false,
    val isExporting: Boolean = false,
    val errorMessage: UiMessage? = null,
    val successMessage: UiMessage? = null
) {
    /** Primary preview (HOME, or LOCK when target == LOCK). */
    val previewBitmap: Bitmap?
        get() = publishedPreview?.home?.bitmap

    /** Secondary preview only populated when target == BOTH. */
    val lockPreviewBitmap: Bitmap?
        get() = publishedPreview?.lock?.bitmap

    /** The plan that belongs to the currently published bitmap. */
    val renderPlan: WallpaperRenderPlan?
        get() = publishedPreview?.home?.plan

    /** True while any operation can invalidate or consume the current preview. */
    val isBusy: Boolean
        get() = isLoading || isRendering || isApplying || isExporting

    /** The bitmap currently shown in the device frame. */
    val activeBitmap: Bitmap?
        get() = if (previewingLock && lockPreviewBitmap != null) lockPreviewBitmap else previewBitmap

    /** Width/height ratio of the actual device screen for the preview frame. */
    val deviceAspectRatio: Float
        get() = deviceProfile?.let {
            it.screenWidthPx.toFloat() / it.screenHeightPx.toFloat()
        } ?: (9f / 19f)

    /**
     * True when source image resolution is too low for the target canvas (< 80% of target
     * in either dimension), which would force upscaling beyond the 110% fidelity limit.
     */
    val isLowResolution: Boolean
        get() {
            val meta = sourceImageMeta ?: return false
            val profile = deviceProfile ?: return false
            val targetW = profile.screenWidthPx
            val targetH = profile.screenHeightPx
            return meta.width < targetW * 0.8f || meta.height < targetH * 0.8f
        }
}
