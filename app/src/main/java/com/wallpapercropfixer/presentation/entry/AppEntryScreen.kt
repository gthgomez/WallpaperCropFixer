package com.wallpapercropfixer.presentation.entry

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

@Composable
fun AppEntryScreen(
    onImageSelected: (String) -> Unit,
    onSettingsClick: () -> Unit
) {
    val context = LocalContext.current
    var permissionDenied by remember { mutableStateOf(false) }
    var copyError by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult

        // The Android photo picker (content://media/picker/…) issues a temporary grant
        // tied to the activity result callback stack frame. The grant is revoked the
        // moment this lambda returns — it is NOT persistable via takePersistableUriPermission,
        // and any coroutine/IO dispatch loses the grant before openInputStream runs.
        //
        // Fix: copy bytes synchronously on the main thread, right here, before returning.
        // Photos are typically 3–8 MB; copyTo() on a BufferedInputStream takes <150 ms —
        // well within the ANR threshold and not noticeable to the user. The resulting
        // file:// path is permanently accessible without any permissions.
        val fileUri = runCatching {
            val ext = when (context.contentResolver.getType(uri)) {
                "image/png"  -> "png"
                "image/webp" -> "webp"
                "image/gif"  -> "gif"
                else         -> "jpg"
            }
            val dest = File(context.cacheDir, "wcf_pick_${System.currentTimeMillis()}.$ext")
            context.contentResolver.openInputStream(uri)!!.use { input ->
                dest.outputStream().buffered().use { out -> input.copyTo(out) }
            }
            dest.absolutePath   // pass a plain file path — no URI scheme needed
        }.getOrNull()

        if (fileUri != null) {
            onImageSelected(fileUri)
        } else {
            copyError = true
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } else {
            permissionDenied = true
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
                    contentDescription = "Settings",
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
            text = "Wallpaper\nCrop Fixer",
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Bold,
                lineHeight = 44.sp
            ),
            textAlign = TextAlign.Center,
            color = Color(0xFF111111),
            modifier = Modifier.padding(horizontal = 28.dp)
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Preview and fix how Android crops your photos before setting them as wallpaper.",
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
            Icon(
                Icons.Default.PhotoLibrary,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.size(8.dp))
            Text(
                "Choose a Photo",
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp)
            )
        }

        Spacer(Modifier.height(16.dp))

        if (permissionDenied) {
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Photo access required",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFFB71C1C)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Grant photo access in Settings so the app can read your images.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB71C1C)
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                )
                            },
                            shape = RoundedCornerShape(50)
                        ) {
                            Text("Open Settings", color = Color(0xFFD32F2F))
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

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
                        "Couldn't read that photo. Try choosing it again.",
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
                title = "Face-aware crop",
                description = "Detects faces and keeps them in frame automatically"
            )
            FeatureCard(
                icon = Icons.Default.AutoFixHigh,
                title = "Three crop modes",
                description = "Safe Fit, Balanced, or Fill — choose what matters most"
            )
            FeatureCard(
                icon = Icons.Default.Wallpaper,
                title = "Framing preview",
                description = "Preview and optimize framing for your screen"
            )
        }

        Spacer(Modifier.height(40.dp))
    }
}

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
                    title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF111111)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF888888)
                )
            }
        }
    }
}
