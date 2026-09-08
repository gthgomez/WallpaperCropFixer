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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wallpaper
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wallpapercropfixer.R
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

    fun launchPicker() {
        copyError = false
        launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top bar row — settings gear lives here so it's never obscured
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
                    tint = Color(0xFF888888)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Gradient icon badge
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Wallpaper,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text = stringResource(R.string.entry_title),
            // No fixed line height: it clipped glyphs at large font scales.
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Bold
            ),
            textAlign = TextAlign.Center,
            color = Color(0xFF111111),
            modifier = Modifier.padding(horizontal = 28.dp)
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.entry_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF777777),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
            modifier = Modifier.padding(horizontal = 28.dp)
        )

        Spacer(Modifier.height(28.dp))

        // Primary CTA
        Button(
            onClick = { launchPicker() },
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
                    color = Color.White,
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

        Spacer(Modifier.height(16.dp))

        if (copyError) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0F0)),
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
                        tint = Color(0xFFD32F2F),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Text(
                        stringResource(R.string.entry_copy_failed),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB71C1C)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        Spacer(Modifier.height(24.dp))

        // Feature cards
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FeatureCard(
                icon = Icons.Default.CenterFocusStrong,
                title = stringResource(R.string.entry_feature_face_title),
                description = stringResource(R.string.entry_feature_face_desc)
            )
            FeatureCard(
                icon = Icons.Default.AutoFixHigh,
                title = stringResource(R.string.entry_feature_modes_title),
                description = stringResource(R.string.entry_feature_modes_desc)
            )
            FeatureCard(
                icon = Icons.Default.Wallpaper,
                title = stringResource(R.string.entry_feature_preview_title),
                description = stringResource(R.string.entry_feature_preview_desc)
            )
        }

        Spacer(Modifier.height(40.dp))
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

@Composable
private fun FeatureCard(icon: ImageVector, title: String, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF111111)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF888888)
                )
            }
        }
    }
}