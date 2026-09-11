package com.wallpapercropfixer

import android.os.Bundle
import androidx.activity.ComponentActivity
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
