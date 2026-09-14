package com.wallpapercropfixer.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Quiet Gallery — the app's visual identity.
 *
 * Always-light scheme: photo apps (Google Photos, Snapseed) stay white regardless
 * of system dark mode so images are always seen against a clean canvas. Warm
 * paper background, white surfaces, charcoal ink, one solid iris accent and a
 * small teal detail. The user's photo is the most colorful thing on screen.
 *
 * Contrast notes (measured, sRGB relative luminance):
 *  - white on iris #5B4EC9 = 6.2:1 (primary action text)
 *  - #62646C on paper #FAFAF7 = ~5.6:1 (secondary text)
 *  - teal #0EA5A6 is decoration only; white-on-teal is 3.0:1 and must never
 *    carry small text.
 */
private val Paper = Color(0xFFFAFAF7)
private val Surface = Color(0xFFFFFFFF)
private val Ink = Color(0xFF202127)
private val InkSecondary = Color(0xFF62646C)
private val Iris = Color(0xFF5B4EC9)
private val Teal = Color(0xFF0EA5A6)

private val LightColors = lightColorScheme(
    primary = Iris,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEFECFC),
    onPrimaryContainer = Color(0xFF241A63),
    secondary = Color(0xFF56567E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE7E5F6),
    onSecondaryContainer = Color(0xFF1F1F3D),
    tertiary = Teal,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC8F1F0),
    onTertiaryContainer = Color(0xFF003233),
    error = Color(0xFF8A3326),
    onError = Color.White,
    errorContainer = Color(0xFFFFF0EA),
    onErrorContainer = Color(0xFF5F1E14),
    background = Paper,
    onBackground = Ink,
    surface = Surface,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEFEFED),
    onSurfaceVariant = InkSecondary,
    outline = Color(0xFFC9C9D1),
    outlineVariant = Color(0xFFE6E5EB)
)

/** Shared corner-radius family: chips (12) / cards (20) / preview frame (28). */
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/** Tokens with no Material color-slot: success state and the photo stage surface. */
data class QgColors(
    val success: Color,
    val stage: Color
)

val LocalQgColors = staticCompositionLocalOf {
    QgColors(success = Color(0xFF28644F), stage = Color(0xFFEFEFED))
}

@Composable
fun WallpaperCropFixerTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalQgColors provides QgColors(success = Color(0xFF28644F), stage = Color(0xFFEFEFED))) {
        MaterialTheme(
            colorScheme = LightColors,
            shapes = AppShapes,
            content = content
        )
    }
}
