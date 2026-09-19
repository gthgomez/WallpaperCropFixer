package com.wallpapercropfixer.data.settings

import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.test.core.app.ApplicationProvider
import com.wallpapercropfixer.domain.model.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DataStoreSettingsRepositoryTest {
    @Test fun `unknown persisted modes fall back without discarding other preferences`() {
        val repository = DataStoreSettingsRepository(ApplicationProvider.getApplicationContext())
        val preferences = mutablePreferencesOf(
            stringPreferencesKey("crop_mode") to "REMOVED_MODE",
            stringPreferencesKey("wallpaper_target") to "FUTURE_TARGET",
            stringPreferencesKey("fill_mode") to "",
            intPreferencesKey("jpeg_quality") to 83
        )
        assertEquals(UserSettings(exportJpegQuality = 83), repository.decodeSettings(preferences))
    }

    @Test fun `persisted jpeg quality is bounded for bitmap compression`() {
        val repository = DataStoreSettingsRepository(ApplicationProvider.getApplicationContext())
        for ((stored, expected) in listOf(-7 to 60, 120 to 100, 75 to 75)) {
            assertEquals(expected, repository.decodeSettings(mutablePreferencesOf(
                intPreferencesKey("jpeg_quality") to stored
            )).exportJpegQuality)
        }
    }
}
