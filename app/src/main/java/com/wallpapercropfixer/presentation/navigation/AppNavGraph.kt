package com.wallpapercropfixer.presentation.navigation

import android.net.Uri
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wallpapercropfixer.presentation.editor.WallpaperEditorScreen
import com.wallpapercropfixer.presentation.entry.AppEntryScreen
import com.wallpapercropfixer.presentation.settings.SettingsScreen

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Destinations.ENTRY,
        enterTransition = { slideInHorizontally { it } + fadeIn() },
        exitTransition = { slideOutHorizontally { -it / 3 } + fadeOut(targetAlpha = 0.6f) },
        popEnterTransition = { slideInHorizontally { -it / 3 } + fadeIn() },
        popExitTransition = { slideOutHorizontally { it } + fadeOut() }
    ) {

        composable(Destinations.ENTRY) {
            AppEntryScreen(
                onImageSelected = { path ->
                    // Uri.encode handles slashes and special chars in the file path.
                    // The route uses a query param so encoded slashes are not parsed
                    // as path separators by the Navigation component.
                    val encoded = Uri.encode(path)
                    navController.navigate(Destinations.editorRoute(encoded))
                },
                onSettingsClick = { navController.navigate(Destinations.SETTINGS) }
            )
        }

        composable(
            route = Destinations.EDITOR,
            arguments = listOf(navArgument("uri") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedPath = backStackEntry.arguments?.getString("uri") ?: return@composable
            val path = Uri.decode(encodedPath)
            WallpaperEditorScreen(
                imageUri = path,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Destinations.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
