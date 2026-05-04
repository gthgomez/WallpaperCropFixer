package com.wallpapercropfixer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.wallpapercropfixer.presentation.navigation.AppNavGraph
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WallpaperCropFixerTheme {
                Surface {
                    AppNavGraph()
                }
            }
        }
    }
}

@Composable
private fun WallpaperCropFixerTheme(content: @Composable () -> Unit) {
    // Always use light color scheme — photo apps (Google Photos, Snapseed) stay white
    // regardless of system dark mode so images are always seen against a clean white canvas.
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF6750A4),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFEADDFF),
            onPrimaryContainer = Color(0xFF21005D),
            secondary = Color(0xFF625B71),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFE8DEF8),
            onSecondaryContainer = Color(0xFF1D192B),
            tertiary = Color(0xFF0EA5A6),
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFFBDEFEF),
            onTertiaryContainer = Color(0xFF002020),
            background = Color(0xFFFAFAFA),
            onBackground = Color(0xFF111111),
            surface = Color(0xFFFAFAFA),
            onSurface = Color(0xFF111111),
            surfaceVariant = Color(0xFFF1EEF7),
            onSurfaceVariant = Color(0xFF49454F),
            outline = Color(0xFF79747E)
        ),
        content = content
    )
}
