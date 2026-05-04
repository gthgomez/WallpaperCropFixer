package com.wallpapercropfixer.di

import com.wallpapercropfixer.data.behavior.DefaultWallpaperBehaviorRepository
import com.wallpapercropfixer.data.device.AndroidDeviceProfileRepository
import com.wallpapercropfixer.data.export.AndroidWallpaperExportRepository
import com.wallpapercropfixer.data.face.MlKitFaceDetectionRepository
import com.wallpapercropfixer.data.image.AndroidImageRepository
import com.wallpapercropfixer.data.settings.DataStoreSettingsRepository
import com.wallpapercropfixer.data.wallpaper.AndroidWallpaperApplyRepository
import com.wallpapercropfixer.domain.repository.DeviceProfileRepository
import com.wallpapercropfixer.domain.repository.FaceDetectionRepository
import com.wallpapercropfixer.domain.repository.ImageRepository
import com.wallpapercropfixer.domain.repository.SettingsRepository
import com.wallpapercropfixer.domain.repository.WallpaperApplyRepository
import com.wallpapercropfixer.domain.repository.WallpaperBehaviorRepository
import com.wallpapercropfixer.domain.repository.WallpaperExportRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindDeviceProfileRepository(impl: AndroidDeviceProfileRepository): DeviceProfileRepository

    @Binds @Singleton
    abstract fun bindWallpaperBehaviorRepository(impl: DefaultWallpaperBehaviorRepository): WallpaperBehaviorRepository

    @Binds @Singleton
    abstract fun bindImageRepository(impl: AndroidImageRepository): ImageRepository

    @Binds @Singleton
    abstract fun bindFaceDetectionRepository(impl: MlKitFaceDetectionRepository): FaceDetectionRepository

    @Binds @Singleton
    abstract fun bindWallpaperExportRepository(impl: AndroidWallpaperExportRepository): WallpaperExportRepository

    @Binds @Singleton
    abstract fun bindWallpaperApplyRepository(impl: AndroidWallpaperApplyRepository): WallpaperApplyRepository

    @Binds @Singleton
    abstract fun bindSettingsRepository(impl: DataStoreSettingsRepository): SettingsRepository
}
