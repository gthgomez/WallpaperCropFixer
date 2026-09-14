package com.wallpapercropfixer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import com.wallpapercropfixer.presentation.navigation.AppNavGraph
import com.wallpapercropfixer.presentation.theme.WallpaperCropFixerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Pin light system bars to match the always-light content below — with the
        // DayNight XML theme, default edge-to-edge styling draws dark status-bar
        // icons that disappear when the system forces dark mode.
        val transparent = android.graphics.Color.TRANSPARENT
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(transparent, transparent),
            navigationBarStyle = SystemBarStyle.light(transparent, transparent)
        )
        setContent {
            WallpaperCropFixerTheme {
                Surface {
                    AppNavGraph()
                }
            }
        }
    }
}
