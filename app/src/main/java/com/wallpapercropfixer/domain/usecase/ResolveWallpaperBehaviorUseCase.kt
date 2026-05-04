package com.wallpapercropfixer.domain.usecase

import com.wallpapercropfixer.domain.model.DeviceProfile
import com.wallpapercropfixer.domain.model.WallpaperBehaviorProfile
import com.wallpapercropfixer.domain.repository.WallpaperBehaviorRepository
import javax.inject.Inject

class ResolveWallpaperBehaviorUseCase @Inject constructor(
    private val wallpaperBehaviorRepository: WallpaperBehaviorRepository
) {
    suspend operator fun invoke(deviceProfile: DeviceProfile): WallpaperBehaviorProfile =
        wallpaperBehaviorRepository.resolveBehaviorProfile(deviceProfile)
}
