package com.wallpapercropfixer.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wallpapercropfixer.domain.model.UserSettings
import com.wallpapercropfixer.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<UserSettings> = settingsRepository
        .observeSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())

    /**
     * True once DataStore's first emission arrived. Before that, `settings` holds
     * the default [UserSettings], so editing controls must stay disabled to avoid
     * persisting those defaults over stored values.
     */
    val loaded: StateFlow<Boolean> = settingsRepository
        .observeSettings()
        .map { true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun update(settings: UserSettings) {
        viewModelScope.launch { settingsRepository.updateSettings(settings) }
    }
}
