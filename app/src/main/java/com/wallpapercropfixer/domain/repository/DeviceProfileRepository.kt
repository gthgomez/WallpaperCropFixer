package com.wallpapercropfixer.domain.repository

import com.wallpapercropfixer.domain.model.DeviceProfile

interface DeviceProfileRepository {
    suspend fun getCurrentDeviceProfile(): DeviceProfile
}
