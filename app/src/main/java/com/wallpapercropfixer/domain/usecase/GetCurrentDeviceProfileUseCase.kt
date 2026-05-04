package com.wallpapercropfixer.domain.usecase

import com.wallpapercropfixer.domain.model.DeviceProfile
import com.wallpapercropfixer.domain.repository.DeviceProfileRepository
import javax.inject.Inject

class GetCurrentDeviceProfileUseCase @Inject constructor(
    private val deviceProfileRepository: DeviceProfileRepository
) {
    suspend operator fun invoke(): DeviceProfile =
        deviceProfileRepository.getCurrentDeviceProfile()
}
