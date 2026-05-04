package com.wallpapercropfixer.di

import com.wallpapercropfixer.domain.engine.WallpaperCropEngine
import com.wallpapercropfixer.domain.engine.WallpaperCropEngineImpl
import com.wallpapercropfixer.rendering.WallpaperBitmapRenderer
import com.wallpapercropfixer.rendering.WallpaperBitmapRendererImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RendererModule {

    @Binds @Singleton
    abstract fun bindCropEngine(impl: WallpaperCropEngineImpl): WallpaperCropEngine

    @Binds @Singleton
    abstract fun bindBitmapRenderer(impl: WallpaperBitmapRendererImpl): WallpaperBitmapRenderer
}
