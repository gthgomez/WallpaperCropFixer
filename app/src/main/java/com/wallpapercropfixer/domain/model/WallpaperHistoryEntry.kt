package com.wallpapercropfixer.domain.model

/**
 * A prior wallpaper render the user can reuse from the entry screen's "Recent" list.
 *
 * [id] and [renderedAtEpochMs] are supplied by the caller so this model and the
 * history encode/decode logic stay pure and free of clock or I/O side effects.
 */
data class WallpaperHistoryEntry(
    val id: Long,
    val renderedAtEpochMs: Long,
    val target: WallpaperTarget,
    val cropMode: CropMode,
    val fillMode: BackgroundFillMode,
    val filePath: String,
    val widthPx: Int,
    val heightPx: Int
)
