package com.wallpapercropfixer.domain.repository

import com.wallpapercropfixer.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeSettings(): Flow<UserSettings>
    suspend fun updateSettings(settings: UserSettings)
}
