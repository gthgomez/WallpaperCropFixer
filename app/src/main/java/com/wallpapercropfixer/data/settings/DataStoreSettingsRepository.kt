package com.wallpapercropfixer.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.wallpapercropfixer.domain.model.BackgroundFillMode
import com.wallpapercropfixer.domain.model.CropMode
import com.wallpapercropfixer.domain.model.UserSettings
import com.wallpapercropfixer.domain.model.WallpaperTarget
import com.wallpapercropfixer.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "wcf_settings")

class DataStoreSettingsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) : SettingsRepository {

    private object Keys {
        val CROP_MODE = stringPreferencesKey("crop_mode")
        val TARGET = stringPreferencesKey("wallpaper_target")
        val FILL_MODE = stringPreferencesKey("fill_mode")
        val FACE_AWARE = booleanPreferencesKey("face_aware")
        val JPEG_QUALITY = intPreferencesKey("jpeg_quality")
    }

    override fun observeSettings(): Flow<UserSettings> =
        context.dataStore.data.map { prefs ->
            UserSettings(
                defaultCropMode = prefs[Keys.CROP_MODE]?.let { CropMode.valueOf(it) }
                    ?: CropMode.BALANCED,
                defaultWallpaperTarget = prefs[Keys.TARGET]?.let { WallpaperTarget.valueOf(it) }
                    ?: WallpaperTarget.HOME,
                defaultBackgroundFillMode = prefs[Keys.FILL_MODE]?.let { BackgroundFillMode.valueOf(it) }
                    ?: BackgroundFillMode.BLUR,
                defaultFaceAwareEnabled = prefs[Keys.FACE_AWARE] ?: true,
                exportJpegQuality = prefs[Keys.JPEG_QUALITY] ?: 92
            )
        }

    override suspend fun updateSettings(settings: UserSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CROP_MODE] = settings.defaultCropMode.name
            prefs[Keys.TARGET] = settings.defaultWallpaperTarget.name
            prefs[Keys.FILL_MODE] = settings.defaultBackgroundFillMode.name
            prefs[Keys.FACE_AWARE] = settings.defaultFaceAwareEnabled
            prefs[Keys.JPEG_QUALITY] = settings.exportJpegQuality
        }
    }
}
