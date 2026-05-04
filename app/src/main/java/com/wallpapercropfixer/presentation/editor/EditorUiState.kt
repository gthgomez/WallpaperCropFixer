package com.wallpapercropfixer.presentation.editor

import android.graphics.Bitmap
import com.wallpapercropfixer.domain.model.BackgroundFillMode
import com.wallpapercropfixer.domain.model.CropMode
import com.wallpapercropfixer.domain.model.DeviceProfile
import com.wallpapercropfixer.domain.model.FocusPoint
import com.wallpapercropfixer.domain.model.SourceImageMeta
import com.wallpapercropfixer.domain.model.SubjectAnalysis
import com.wallpapercropfixer.domain.model.WallpaperBehaviorProfile
import com.wallpapercropfixer.domain.model.WallpaperRenderPlan
import com.wallpapercropfixer.domain.model.WallpaperTarget

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
    val renderPlan: WallpaperRenderPlan? = null,
    // Primary preview (HOME, or LOCK when target == LOCK)
    val previewBitmap: Bitmap? = null,
    // Secondary preview only populated when target == BOTH
    val lockPreviewBitmap: Bitmap? = null,
    // Which of the two bitmaps the user is currently viewing in the frame
    val previewingLock: Boolean = false,
    // Monotonically increasing counter — used as Crossfade key so the animation key is
    // decoupled from the Bitmap reference, preventing reads on recycled bitmaps during fade
    val previewRevision: Int = 0,
    val isRendering: Boolean = false,
    val exportedPath: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
) {
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
