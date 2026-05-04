package com.wallpapercropfixer.presentation.preview

import android.graphics.Bitmap
import com.wallpapercropfixer.domain.model.WallpaperTarget

data class PreviewUiState(
    val isRendering: Boolean = false,
    val homePreviewBitmap: Bitmap? = null,
    val lockPreviewBitmap: Bitmap? = null,
    val selectedTarget: WallpaperTarget = WallpaperTarget.HOME,
    val compareModeEnabled: Boolean = false,
    val errorMessage: String? = null
)
