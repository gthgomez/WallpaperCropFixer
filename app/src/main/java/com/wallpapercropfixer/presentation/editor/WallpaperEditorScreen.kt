package com.wallpapercropfixer.presentation.editor

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wallpapercropfixer.domain.model.CropMode
import com.wallpapercropfixer.domain.model.WallpaperTarget
import com.wallpapercropfixer.presentation.components.DevicePreviewFrame
import com.wallpapercropfixer.presentation.components.ModeChipRow
import com.wallpapercropfixer.presentation.components.WallpaperTargetTabs

@Composable
fun WallpaperEditorScreen(
    imageUri: String,
    onBack: () -> Unit,
    viewModel: EditorViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current

    LaunchedEffect(imageUri) { viewModel.loadImage(imageUri) }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSuccess()
        }
    }

    Scaffold(
        containerColor = Color(0xFFFAFAFA),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        if (state.isLoading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFAFAFA)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Loading photo…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF888888)
                    )
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Custom top bar — no divider, blends into the white page
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF333333)
                    )
                }
                Text(
                    "Edit Wallpaper",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF111111),
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { viewModel.resetToDefaults() },
                    enabled = state.sourceImageMeta != null
                ) {
                    Icon(
                        Icons.Default.RestartAlt,
                        contentDescription = "Reset to defaults",
                        tint = if (state.sourceImageMeta != null) Color(0xFF666666) else Color(0xFFCCCCCC)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Preview — centrepiece, generous vertical space like Google Photos crop view
            Box(contentAlignment = Alignment.Center) {
                Crossfade(
                    targetState = state.previewRevision,
                    animationSpec = tween(durationMillis = 220),
                    label = "preview_crossfade"
                ) {
                    DevicePreviewFrame(
                        bitmap = state.activeBitmap,
                        deviceAspectRatio = state.deviceAspectRatio,
                        focusPoint = state.manualFocusPoint
                            ?: state.subjectAnalysis?.suggestedFocusPoint,
                        onFocusTap = if (state.activeBitmap != null) {
                            { viewModel.updateManualFocusPoint(it) }
                        } else null,
                        modifier = Modifier.padding(horizontal = 40.dp)
                    )
                }

                if (state.isRendering) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            if (state.isLowResolution) {
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .wrapContentHeight()
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFE65100),
                        modifier = Modifier
                            .size(14.dp)
                            .padding(end = 4.dp)
                    )
                    Text(
                        "Low resolution — may appear pixelated",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFE65100)
                    )
                }
            }

            if (state.activeBitmap != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (state.previewingLock) "Lock screen preview  ·  focus adjusts both"
                           else "Tap preview to reposition focus",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFAAAAAA)
                )
            }

            state.deviceProfile?.let { profile ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${profile.manufacturer} ${profile.model}  ·  ${profile.screenWidthPx}×${profile.screenHeightPx}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFBBBBBB)
                )
            }

            if (state.wallpaperTarget == WallpaperTarget.BOTH && state.lockPreviewBitmap != null) {
                Spacer(Modifier.height(12.dp))
                FilledTonalButton(
                    onClick = { viewModel.togglePreviewTarget() },
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(
                        imageVector = if (state.previewingLock) Icons.Default.PhoneAndroid
                                      else Icons.Default.LockOpen,
                        contentDescription = null,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(end = 0.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (state.previewingLock) "Showing lock screen" else "Showing home screen")
                }
            }

            Spacer(Modifier.height(20.dp))

            // Controls card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {

                    // Target tabs
                    Text(
                        "Apply to",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF888888)
                    )
                    Spacer(Modifier.height(8.dp))
                    WallpaperTargetTabs(
                        selected = state.wallpaperTarget,
                        onSelect = { viewModel.setWallpaperTarget(it) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(20.dp))

                    // Crop mode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Crop mode",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFF111111),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            state.cropMode.description(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF999999)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    ModeChipRow(
                        selected = state.cropMode,
                        onSelect = { viewModel.setCropMode(it) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(16.dp))

                    // Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFFF0F0F0))
                    )

                    Spacer(Modifier.height(16.dp))

                    // Face-aware row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Face-aware crop",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = Color(0xFF111111)
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "Centers crop around detected faces",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF999999)
                            )
                        }
                        Switch(
                            checked = state.faceAwareEnabled,
                            onCheckedChange = { viewModel.toggleFaceAware(it) },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.semantics {
                                contentDescription = "Face-aware crop, ${if (state.faceAwareEnabled) "on" else "off"}"
                            }
                        )
                    }

                    state.subjectAnalysis?.faces?.size?.let { count ->
                        if (count > 0) {
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "$count face${if (count > 1) "s" else ""} detected",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.exportWallpaper() },
                    enabled = state.previewBitmap != null && !state.isRendering,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("Save")
                }

                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        } else {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        viewModel.applyWallpaper()
                    },
                    enabled = state.previewBitmap != null && !state.isRendering,
                    modifier = Modifier
                        .weight(2f)
                        .height(52.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text(
                        "Apply Wallpaper",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

private fun CropMode.description() = when (this) {
    CropMode.SAFE_FIT -> "Full photo · adds padding"
    CropMode.BALANCED -> "Balanced coverage"
    CropMode.FILL     -> "Fills screen · may crop"
}
