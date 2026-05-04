package com.wallpapercropfixer.data.behavior

import com.wallpapercropfixer.domain.model.DeviceProfile
import com.wallpapercropfixer.domain.model.WallpaperBehaviorProfile
import com.wallpapercropfixer.domain.repository.WallpaperBehaviorRepository
import javax.inject.Inject

class DefaultWallpaperBehaviorRepository @Inject constructor() : WallpaperBehaviorRepository {

    override suspend fun resolveBehaviorProfile(deviceProfile: DeviceProfile): WallpaperBehaviorProfile =
        KnownWallpaperProfiles.matchBrand(deviceProfile.manufacturer)
}
