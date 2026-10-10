package com.wallpapercropfixer.presentation.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wallpapercropfixer.domain.model.WallpaperHistoryEntry
import com.wallpapercropfixer.domain.repository.ImageRepository
import com.wallpapercropfixer.domain.repository.WallpaperHistoryRepository
import com.wallpapercropfixer.domain.usecase.ApplyWallpaperUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One-shot outcome of re-applying a recent wallpaper. */
enum class EntryMessage { APPLIED, FAILED }

/**
 * Backs the entry screen's "Recent" list: a bounded, on-device history of
 * previously rendered wallpapers that can be set again with one tap. No network,
 * no account, no photo-library permission.
 */
@HiltViewModel
class EntryViewModel @Inject constructor(
    private val historyRepository: WallpaperHistoryRepository,
    private val imageRepository: ImageRepository,
    private val applyWallpaper: ApplyWallpaperUseCase
) : ViewModel() {

    val recent: StateFlow<List<WallpaperHistoryEntry>> = historyRepository.observeHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _message = MutableStateFlow<EntryMessage?>(null)
    val message: StateFlow<EntryMessage?> = _message.asStateFlow()

    private val _isApplying = MutableStateFlow(false)
    val isApplying: StateFlow<Boolean> = _isApplying.asStateFlow()

    fun reapply(entry: WallpaperHistoryEntry) {
        if (_isApplying.value) return
        _isApplying.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val ok = runCatching {
                val bitmap = imageRepository.decodeBitmapSampled(
                    uri = entry.filePath,
                    maxWidth = entry.widthPx.coerceAtLeast(1),
                    maxHeight = entry.heightPx.coerceAtLeast(1)
                )
                applyWallpaper(bitmap, entry.target).getOrThrow()
            }.isSuccess
            _message.value = if (ok) EntryMessage.APPLIED else EntryMessage.FAILED
            _isApplying.value = false
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
