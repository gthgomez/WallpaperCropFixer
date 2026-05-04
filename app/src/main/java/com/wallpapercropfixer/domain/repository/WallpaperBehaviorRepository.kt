package com.wallpapercropfixer.domain.repository

import com.wallpapercropfixer.domain.model.DeviceProfile
import com.wallpapercropfixer.domain.model.WallpaperBehaviorProfile

interface WallpaperBehaviorRepository {
    suspend fun resolveBehaviorProfile(deviceProfile: DeviceProfile): WallpaperBehaviorProfile
}
