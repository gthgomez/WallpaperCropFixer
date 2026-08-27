package com.wallpapercropfixer.data.behavior

import com.wallpapercropfixer.domain.model.WallpaperBehaviorProfile

/**
 * Seed profiles. These are best-effort estimates of OEM launcher behavior —
 * not guaranteed to match every device or launcher version. They are used only
 * to size the rendered canvas; the launcher ultimately decides how a wallpaper
 * is displayed (see PRIVACY/QA docs for the "optimize framing" guidance).
 */
object KnownWallpaperProfiles {

    val all = listOf(
        WallpaperBehaviorProfile(
            profileId = "generic_default",
            brand = "generic",
            homeWidthMultiplier = 1.10f,
            lockWidthMultiplier = 1.0f
        ),
        WallpaperBehaviorProfile(
            profileId = "samsung_oneui_default",
            brand = "samsung",
            homeWidthMultiplier = 1.12f,
            lockWidthMultiplier = 1.0f
        ),
        WallpaperBehaviorProfile(
            profileId = "google_pixel_default",
            brand = "google",
            homeWidthMultiplier = 1.08f,
            lockWidthMultiplier = 1.0f
        ),
        WallpaperBehaviorProfile(
            profileId = "xiaomi_miui_default",
            brand = "xiaomi",
            homeWidthMultiplier = 1.10f,
            lockWidthMultiplier = 1.0f
        ),
        WallpaperBehaviorProfile(
            profileId = "oneplus_default",
            brand = "oneplus",
            homeWidthMultiplier = 1.10f,
            lockWidthMultiplier = 1.0f
        )
    )

    val generic: WallpaperBehaviorProfile = all.first { it.profileId == "generic_default" }

    fun matchBrand(brand: String): WallpaperBehaviorProfile =
        all.firstOrNull { it.brand.equals(brand, ignoreCase = true) } ?: generic
}