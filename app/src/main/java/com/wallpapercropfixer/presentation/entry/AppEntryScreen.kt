package com.wallpapercropfixer.presentation.entry

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wallpapercropfixer.R
import com.wallpapercropfixer.presentation.components.CropMarkBadge
import com.wallpapercropfixer.presentation.components.FitDemoCanvas
import com.wallpapercropfixer.presentation.theme.WallpaperCropFixerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun AppEntryScreen(
    onImageSelected: (String) -> Unit,
    onSettingsClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isPreparing by remember { mutableStateOf(false) }
    var copyError by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult

        // Copy the picked image into private cache storage off the main thread.
        // The Photo Picker URI grant is valid for this activity session, so the
        // short IO dispatch is safe. Copying to our own cache makes the file path
        // stable for the whole editing session without any storage permission.
        isPreparing = true
        copyError = false
        scope.launch {
            val path = copyPickedImageToCache(context, uri)
            isPreparing = false
            if (path != null) {
                onImageSelected(path)
            } else {
                copyError = true
            }
        }
    }

    EntryContent(
        isPreparing = isPreparing,
        copyError = copyError,
        onChoosePhoto = {
            copyError = false
            launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        onSettingsClick = onSettingsClick
    )
}

/** Stateless entry UI (previewable): one promise, one demonstration, one action. */
@Composable
internal fun EntryContent(
    isPreparing: Boolean,
    copyError: Boolean,
    onChoosePhoto: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = stringResource(R.string.settings),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        CropMarkBadge()

        Spacer(Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.entry_headline),
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 28.dp)
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.entry_desc),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 28.dp)
        )

        Spacer(Modifier.height(28.dp))

        // Fit demonstration — abstract and labeled as an example, not a promise.
        Box(
            modifier = Modifier
                .width(150.dp)
                .shadow(elevation = 10.dp, shape = RoundedCornerShape(22.dp))
                .clip(RoundedCornerShape(22.dp))
        ) {
            FitDemoCanvas(modifier = Modifier)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.entry_demo_label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = onChoosePhoto,
            enabled = !isPreparing,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(56.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp,
                pressedElevation = 2.dp
            )
        ) {
            if (isPreparing) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.size(8.dp))
            Text(
                if (isPreparing) stringResource(R.string.entry_preparing_photo)
                else stringResource(R.string.entry_choose_photo),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp)
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.entry_privacy),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 28.dp)
        )

        if (copyError) {
            Spacer(Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Text(
                        stringResource(R.string.entry_copy_failed),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Column(modifier = Modifier.padding(horizontal = 28.dp)) {
            Text(
                text = stringResource(R.string.entry_help_title),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.entry_help_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(40.dp))
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 880)
@Composable
private fun EntryContentPreview() {
    WallpaperCropFixerTheme {
        EntryContent(isPreparing = false, copyError = false, onChoosePhoto = {}, onSettingsClick = {})
    }
}

/**
 * Copies the picked image bytes into a private cache file and returns the plain
 * file path. Runs on IO. If the copy fails (or the scope is cancelled mid-copy),
 * the partial file is removed and null is returned.
 */
private suspend fun copyPickedImageToCache(context: Context, uri: Uri): String? =
    withContext(Dispatchers.IO) {
        pruneOldPickFiles(context)
        val ext = when (context.contentResolver.getType(uri)) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            else -> "jpg"
        }
        val dest = File(context.cacheDir, "wcf_pick_${System.currentTimeMillis()}.$ext")
        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: return@withContext null
            input.use { src ->
                dest.outputStream().buffered().use { out -> src.copyTo(out) }
            }
            dest.absolutePath
        } catch (t: Throwable) {
            runCatching { dest.delete() }
            null
        }
    }

/** Removes picker cache files older than one day so the cache stays bounded. */
private fun pruneOldPickFiles(context: Context) {
    val cutoff = System.currentTimeMillis() - PICK_FILE_TTL_MS
    runCatching {
        context.cacheDir.listFiles { f ->
            f.name.startsWith("wcf_pick_") && f.lastModified() < cutoff
        }?.forEach { runCatching { it.delete() } }
    }
}

private const val PICK_FILE_TTL_MS = 24L * 60 * 60 * 1000
