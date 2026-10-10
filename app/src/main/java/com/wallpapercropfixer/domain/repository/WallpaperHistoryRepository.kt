package com.wallpapercropfixer.domain.repository

import com.wallpapercropfixer.domain.model.WallpaperHistoryEntry
import kotlinx.coroutines.flow.Flow

/**
 * Bounded, most-recent-first store of previously rendered wallpapers.
 *
 * The history is intentionally small and local; it exists only so the entry
 * screen can surface a "Recent" list for reuse. Implementations must never
 * perform network access.
 */
interface WallpaperHistoryRepository {

    /** Emits the retained history, most recent entry first. Emits an empty list when nothing is stored. */
    fun observeHistory(): Flow<List<WallpaperHistoryEntry>>

    /**
     * Inserts [entry] at the head of the history, dropping any existing entry with the
     * same [WallpaperHistoryEntry.filePath], then caps the result at [MAX_ENTRIES].
     */
    suspend fun record(entry: WallpaperHistoryEntry)

    /** Removes all retained history. */
    suspend fun clear()

    companion object {
        /** Maximum number of retained entries; the oldest entries are dropped beyond this. */
        const val MAX_ENTRIES = 50
    }
}
