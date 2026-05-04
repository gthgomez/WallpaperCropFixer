package com.wallpapercropfixer.data.behavior

import com.wallpapercropfixer.domain.model.WallpaperBehaviorProfile

/**
 * Seed profiles. These are best-effort estimates of OEM launcher behavior —
 * not guaranteed to match every device or launcher version.
 */
object KnownWallpaperProfiles {

    val all = listOf(
        WallpaperBehaviorProfile(
            profileId = "generic_default",
            brand = "generic",
            launcherHint = null,
            homeWidthMultiplier = 1.10f,
            lockWidthMultiplier = 1.0f,
            cropBiasX = 0.5f,
            cropBiasY = 0.5f,
            separateTargetsSupported = true
        ),
        WallpaperBehaviorProfile(
            profileId = "samsung_oneui_default",
            brand = "samsung",
            launcherHint = "oneui",
            homeWidthMultiplier = 1.12f,
            lockWidthMultiplier = 1.0f,
            cropBiasX = 0.5f,
            cropBiasY = 0.5f,
            separateTargetsSupported = true
        ),
        WallpaperBehaviorProfile(
            profileId = "google_pixel_default",
            brand = "google",
            launcherHint = "pixel",
            homeWidthMultiplier = 1.08f,
            lockWidthMultiplier = 1.0f,
            cropBiasX = 0.5f,
            cropBiasY = 0.5f,
            separateTargetsSupported = true
        ),
        WallpaperBehaviorProfile(
            profileId = "xiaomi_miui_default",
            brand = "xiaomi",
            launcherHint = "miui",
            homeWidthMultiplier = 1.10f,
            lockWidthMultiplier = 1.0f,
            cropBiasX = 0.5f,
            cropBiasY = 0.5f,
            separateTargetsSupported = true
        ),
        WallpaperBehaviorProfile(
            profileId = "oneplus_default",
            brand = "oneplus",
            launcherHint = null,
            homeWidthMultiplier = 1.10f,
            lockWidthMultiplier = 1.0f,
            cropBiasX = 0.5f,
            cropBiasY = 0.5f,
            separateTargetsSupported = true
        )
    )

    val generic: WallpaperBehaviorProfile = all.first { it.profileId == "generic_default" }

    fun matchBrand(brand: String): WallpaperBehaviorProfile =
        all.firstOrNull { it.brand.equals(brand, ignoreCase = true) } ?: generic
}
