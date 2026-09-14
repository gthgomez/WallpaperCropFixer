package com.wallpapercropfixer.presentation.navigation

import android.net.Uri
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wallpapercropfixer.R
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
            val decodedPath = backStackEntry.arguments?.getString("uri")
                ?.let { Uri.decode(it) }
            if (decodedPath.isNullOrBlank()) {
                // Process-death or a malformed route must not show a blank screen.
                MissingPhotoPlaceholder(onBack = { navController.popBackStack() })
            } else {
                WallpaperEditorScreen(
                    imageUri = decodedPath,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(Destinations.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}

@Composable
private fun MissingPhotoPlaceholder(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(R.string.error_photo_missing),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        Spacer(Modifier.height(8.dp))
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
