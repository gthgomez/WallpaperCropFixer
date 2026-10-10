package com.wallpapercropfixer.data.history

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.wallpapercropfixer.domain.model.BackgroundFillMode
import com.wallpapercropfixer.domain.model.CropMode
import com.wallpapercropfixer.domain.model.WallpaperHistoryEntry
import com.wallpapercropfixer.domain.model.WallpaperTarget
import com.wallpapercropfixer.domain.repository.WallpaperHistoryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Base64
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Field separator for the line-based history encoding. Never appears in an encoded field. */
private const val FIELD_SEPARATOR = "|"

/** Number of '|'-separated fields in a single encoded entry. */
private const val FIELD_COUNT = 8

private val Context.historyDataStore: DataStore<Preferences> by preferencesDataStore(name = "wcf_history")

/**
 * DataStore-backed [WallpaperHistoryRepository] persisting the whole history as a single
 * string preference (`history_v1`) inside its own `wcf_history` preferences file.
 *
 * The list is serialized with the pure, dependency-free [encode]/[decode] helpers below.
 */
class DataStoreWallpaperHistoryRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) : WallpaperHistoryRepository {

    private object Keys {
        val HISTORY = stringPreferencesKey("history_v1")
    }

    override fun observeHistory(): Flow<List<WallpaperHistoryEntry>> =
        context.historyDataStore.data.map { prefs -> decode(prefs[Keys.HISTORY]) }

    override suspend fun record(entry: WallpaperHistoryEntry) {
        context.historyDataStore.edit { prefs ->
            val deduped = decode(prefs[Keys.HISTORY]).filterNot { it.filePath == entry.filePath }
            val capped = (listOf(entry) + deduped).take(WallpaperHistoryRepository.MAX_ENTRIES)
            prefs[Keys.HISTORY] = encode(capped)
        }
    }

    override suspend fun clear() {
        context.historyDataStore.edit { prefs -> prefs.remove(Keys.HISTORY) }
    }
}

/**
 * Encodes [entries] into the on-disk format: one entry per line (newline-separated),
 * fields joined by '|' in a fixed order. The free-form [WallpaperHistoryEntry.filePath]
 * is base64-encoded so paths containing '|' or newlines survive a round-trip; every
 * other field is numeric or a Kotlin enum name and therefore never contains '|'.
 */
internal fun encode(entries: List<WallpaperHistoryEntry>): String =
    entries.joinToString(separator = "\n", transform = ::encodeEntry)

/**
 * Decodes [raw] produced by [encode]. Tolerant by design: a null/blank payload yields an
 * empty list, and any malformed line (wrong field count, unparsable number or base64,
 * unknown enum name) is skipped instead of throwing.
 */
internal fun decode(raw: String?): List<WallpaperHistoryEntry> {
    if (raw.isNullOrEmpty()) return emptyList()
    return raw.lineSequence()
        .filter { it.isNotBlank() }
        .mapNotNull(::decodeEntry)
        .toList()
}

private fun encodeEntry(entry: WallpaperHistoryEntry): String = listOf(
    entry.id.toString(),
    entry.renderedAtEpochMs.toString(),
    entry.target.name,
    entry.cropMode.name,
    entry.fillMode.name,
    Base64.getEncoder().encodeToString(entry.filePath.toByteArray(Charsets.UTF_8)),
    entry.widthPx.toString(),
    entry.heightPx.toString()
).joinToString(separator = FIELD_SEPARATOR)

private fun decodeEntry(line: String): WallpaperHistoryEntry? {
    val fields = line.split(FIELD_SEPARATOR)
    if (fields.size != FIELD_COUNT) return null

    val id = fields[0].toLongOrNull() ?: return null
    val renderedAtEpochMs = fields[1].toLongOrNull() ?: return null
    val target = runCatching { WallpaperTarget.valueOf(fields[2]) }.getOrNull() ?: return null
    val cropMode = runCatching { CropMode.valueOf(fields[3]) }.getOrNull() ?: return null
    val fillMode = runCatching { BackgroundFillMode.valueOf(fields[4]) }.getOrNull() ?: return null
    val filePath = runCatching { String(Base64.getDecoder().decode(fields[5]), Charsets.UTF_8) }.getOrNull() ?: return null
    val widthPx = fields[6].toIntOrNull() ?: return null
    val heightPx = fields[7].toIntOrNull() ?: return null

    return WallpaperHistoryEntry(
        id = id,
        renderedAtEpochMs = renderedAtEpochMs,
        target = target,
        cropMode = cropMode,
        fillMode = fillMode,
        filePath = filePath,
        widthPx = widthPx,
        heightPx = heightPx
    )
}
