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
    /** The current, render-matching publication. The only source of Apply/Save eligibility. */
    val publishedPreview: PublishedPreview? = null,
    /**
     * Display-only snapshot kept on screen while a newer render is pending or has failed.
     * It is never eligible for apply/export; a failed update must not make an old render
     * applicable again.
     */
    val retainedPreview: PublishedPreview? = null,
    /** True when the latest render attempt failed and [retainedPreview] is what is shown. */
    val renderFailed: Boolean = false,
    // Which of the two renders the user is currently viewing (BOTH target only)
    val previewingLock: Boolean = false,
    val isRendering: Boolean = false,
    val isApplying: Boolean = false,
    val isExporting: Boolean = false,
    val errorMessage: UiMessage? = null,
    val successMessage: UiMessage? = null
) {
    /** True only when what is displayed exactly matches the selected options and focus. */
    val isPreviewCurrent: Boolean
        get() = publishedPreview != null

    /** Eligible primary preview (HOME, or the LOCK-only render stored in the primary slot). */
    val previewBitmap: Bitmap?
        get() = publishedPreview?.home?.bitmap

    /** Eligible secondary preview only populated when target == BOTH. */
    val lockPreviewBitmap: Bitmap?
        get() = publishedPreview?.lock?.bitmap

    /** True while any operation can invalidate or consume the current preview. */
    val isBusy: Boolean
        get() = isLoading || isRendering || isApplying || isExporting

    /**
     * The complete snapshot (request + plan + bitmap) currently displayed: the published
     * render when current, otherwise the retained one. The tuple always travels together
     * so overlays and taps can never pair one render's bitmap with another render's
     * geometry. In BOTH mode the viewing tab selects which render is shown.
     */
    val displayedPreview: RenderedPreview?
        get() {
            val publication = publishedPreview ?: retainedPreview ?: return null
            return displayedRender(publication)
        }

    /** The most recent publication regardless of freshness (drives viewing-tab availability). */
    val latestPublication: PublishedPreview?
        get() = publishedPreview ?: retainedPreview

    /** The bitmap currently shown in the device frame. */
    val activeBitmap: Bitmap?
        get() = displayedPreview?.bitmap

    /** Width/height ratio of the actual device screen for the preview frame. */
    val deviceAspectRatio: Float
        get() = deviceProfile?.let {
            it.screenWidthPx.toFloat() / it.screenHeightPx.toFloat()
        } ?: (9f / 19f)

    /**
     * True when source image resolution is too low for the target canvas (< 80% of target
     * in either dimension). Uses the displayed snapshot so source and canvas stay paired.
     */
    val isLowResolution: Boolean
        get() {
            val render = displayedPreview ?: return false
            val meta = render.request.source
            val targetW = render.plan.targetCanvasSpec.widthPx
            val targetH = render.plan.targetCanvasSpec.heightPx
            return meta.width < targetW * 0.8f || meta.height < targetH * 0.8f
        }

    private fun displayedRender(publication: PublishedPreview): RenderedPreview =
        if (publication.target == WallpaperTarget.BOTH && previewingLock) {
            publication.lock ?: publication.home
        } else {
            publication.home
        }
}
