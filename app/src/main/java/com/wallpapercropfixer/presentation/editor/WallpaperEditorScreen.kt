package com.wallpapercropfixer.presentation.editor

import android.content.res.Configuration
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
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wallpapercropfixer.R
import com.wallpapercropfixer.core.math.CropMath
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
    val configuration = LocalConfiguration.current

    // Resolve message strings in composable scope so they track configuration changes.
    val errorMessageText = state.errorMessage?.let { msg ->
        stringResource(msg.resId, *msg.formatArgs.toTypedArray())
    }
    val successMessageText = state.successMessage?.let { msg ->
        stringResource(msg.resId, *msg.formatArgs.toTypedArray())
    }

    LaunchedEffect(imageUri) { viewModel.loadImage(imageUri) }

    // Re-resolve device metrics after orientation/window changes so the canvas and
    // preview always match the current display.
    LaunchedEffect(configuration.orientation) {
        viewModel.refreshForConfigurationChange()
    }

    LaunchedEffect(errorMessageText) {
        if (errorMessageText != null) {
            snackbarHostState.showSnackbar(errorMessageText)
            viewModel.clearError()
        }
    }

    LaunchedEffect(successMessageText) {
        if (successMessageText != null) {
            snackbarHostState.showSnackbar(successMessageText)
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
                        stringResource(R.string.editor_loading_photo),
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
                        contentDescription = stringResource(R.string.back),
                        tint = Color(0xFF333333)
                    )
                }
                Text(
                    stringResource(R.string.editor_title),
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
                        contentDescription = stringResource(R.string.editor_reset_defaults),
                        tint = if (state.sourceImageMeta != null) Color(0xFF666666) else Color(0xFFCCCCCC)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Preview — centrepiece, generous vertical space like Google Photos crop view
            Box(contentAlignment = Alignment.Center) {
                Crossfade(
                    targetState = state.activeBitmap,
                    animationSpec = tween(durationMillis = 220),
                    label = "preview_crossfade"
                ) { currentBitmap ->
                    val canvasFocus = state.renderPlan?.let { plan ->
                        val fp = state.manualFocusPoint ?: state.subjectAnalysis?.suggestedFocusPoint
                        val meta = state.sourceImageMeta
                        if (fp != null && meta != null) {
                            CropMath.sourceFocusToCanvasFocus(
                                sourceFocus = fp,
                                sourceWidth = meta.width,
                                sourceHeight = meta.height,
                                sourceCropRect = plan.sourceCropRect,
                                outputImagePlacement = plan.outputImagePlacement,
                                canvasWidth = plan.targetCanvasSpec.widthPx,
                                canvasHeight = plan.targetCanvasSpec.heightPx
                            )
                        } else null
                    } ?: state.manualFocusPoint ?: state.subjectAnalysis?.suggestedFocusPoint

                    DevicePreviewFrame(
                        bitmap = currentBitmap,
                        deviceAspectRatio = state.deviceAspectRatio,
                        focusPoint = canvasFocus,
                        onFocusTap = if (state.activeBitmap != null) {
                            { tappedCanvasFocus ->
                                val plan = state.renderPlan
                                val meta = state.sourceImageMeta
                                if (plan != null && meta != null) {
                                    val sourceFocus = CropMath.canvasFocusToSourceFocus(
                                        canvasFocus = tappedCanvasFocus,
                                        sourceWidth = meta.width,
                                        sourceHeight = meta.height,
                                        sourceCropRect = plan.sourceCropRect,
                                        outputImagePlacement = plan.outputImagePlacement,
                                        canvasWidth = plan.targetCanvasSpec.widthPx,
                                        canvasHeight = plan.targetCanvasSpec.heightPx
                                    )
                                    viewModel.updateManualFocusPoint(sourceFocus)
                                } else {
                                    viewModel.updateManualFocusPoint(tappedCanvasFocus)
                                }
                            }
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
                        stringResource(R.string.editor_low_resolution),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFE65100)
                    )
                }
            }

            if (state.activeBitmap != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (state.previewingLock) stringResource(R.string.editor_lock_adjusts_both)
                           else stringResource(R.string.editor_tap_to_reposition),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFAAAAAA)
                )
            }

            state.deviceProfile?.let { profile ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        R.string.editor_device_info,
                        profile.manufacturer,
                        profile.model,
                        profile.screenWidthPx,
                        profile.screenHeightPx
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFBBBBBB)
                )
            }

            if (state.activeBitmap != null) {
                Spacer(Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFFBBBBBB),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        stringResource(R.string.editor_launcher_disclaimer),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFBBBBBB)
                    )
                }
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
                    Text(if (state.previewingLock) stringResource(R.string.editor_showing_lock)
                         else stringResource(R.string.editor_showing_home))
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
                        stringResource(R.string.editor_apply_to),
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
                            stringResource(R.string.editor_crop_mode),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFF111111),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            state.cropMode.descriptionRes().let { stringResource(it) },
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
                                stringResource(R.string.editor_face_aware),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = Color(0xFF111111)
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                stringResource(R.string.editor_face_aware_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF999999)
                            )
                        }
                        val faceAwareOn = stringResource(R.string.editor_face_on)
                        val faceAwareOff = stringResource(R.string.editor_face_off)
                        val faceAwareToggleA11y = stringResource(
                            R.string.editor_face_aware_toggle,
                            if (state.faceAwareEnabled) faceAwareOn else faceAwareOff
                        )
                        Switch(
                            checked = state.faceAwareEnabled,
                            onCheckedChange = { viewModel.toggleFaceAware(it) },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.semantics {
                                contentDescription = faceAwareToggleA11y
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
                                    pluralStringResource(R.plurals.editor_faces_detected, count, count),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    if (state.faceAwareEnabled &&
                        (state.faceDetectionStatus == FaceDetectionStatus.NO_FACES ||
                         state.faceDetectionStatus == FaceDetectionStatus.FAILED)
                    ) {
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF8A6D00),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                stringResource(R.string.editor_face_aware_unavailable),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF8A6D00)
                            )
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
                    Text(stringResource(R.string.editor_save))
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
                        stringResource(R.string.editor_apply_wallpaper),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

private fun CropMode.descriptionRes(): Int = when (this) {
    CropMode.SAFE_FIT -> R.string.crop_mode_safe_fit_desc
    CropMode.BALANCED -> R.string.crop_mode_balanced_desc
    CropMode.FILL -> R.string.crop_mode_fill_desc
}