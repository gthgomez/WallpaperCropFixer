package com.wallpapercropfixer.presentation.editor

import android.content.res.Configuration
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.activity.compose.BackHandler
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wallpapercropfixer.R
import com.wallpapercropfixer.core.math.CropMath
import com.wallpapercropfixer.domain.model.BackgroundFillMode
import com.wallpapercropfixer.domain.model.CropMode
import com.wallpapercropfixer.domain.model.FocusPoint
import com.wallpapercropfixer.domain.model.WallpaperTarget
import com.wallpapercropfixer.presentation.components.DevicePreviewFrame
import com.wallpapercropfixer.presentation.components.FillModeRow
import com.wallpapercropfixer.presentation.components.ModeChipRow
import com.wallpapercropfixer.presentation.components.PreviewScreenTabs
import com.wallpapercropfixer.presentation.components.WallpaperTargetTabs
import com.wallpapercropfixer.presentation.theme.LocalQgColors
import com.wallpapercropfixer.presentation.theme.WallpaperCropFixerTheme

/** Everything the stateless editor content needs to talk back to the app. */
internal data class EditorCallbacks(
    val onBack: () -> Unit,
    val onReset: () -> Unit,
    val onCropMode: (CropMode) -> Unit,
    val onTarget: (WallpaperTarget) -> Unit,
    val onFillMode: (BackgroundFillMode) -> Unit,
    val onFaceAware: (Boolean) -> Unit,
    val onFocusTap: (FocusPoint) -> Unit,
    val onViewingLock: (Boolean) -> Unit,
    val onSave: () -> Unit,
    val onApply: () -> Unit,
    val onRetry: () -> Unit
)

@Composable
fun WallpaperEditorScreen(
    imageUri: String,
    onBack: () -> Unit,
    viewModel: EditorViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val configuration = LocalConfiguration.current

    // Resolve message strings in composable scope so they track configuration changes.
    val errorMessageText = state.errorMessage?.let { msg ->
        stringResource(msg.resId, *msg.formatArgs.toTypedArray())
    }
    val successMessageText = state.successMessage?.let { msg ->
        stringResource(msg.resId, *msg.formatArgs.toTypedArray())
    }

    LaunchedEffect(imageUri) { viewModel.loadImage(imageUri) }

    // System back during an in-flight apply/export would cancel the operation and
    // can leave the wallpaper half-applied, so back is swallowed while busy.
    BackHandler(enabled = state.isBusy) { }

    // Re-resolve device metrics after orientation/window changes so the canvas and
    // preview always match the current display. smallestScreenWidthDp catches
    // fold/unfold resizes that keep the orientation unchanged.
    LaunchedEffect(configuration.orientation, configuration.smallestScreenWidthDp) {
        viewModel.refreshForConfigurationChange()
    }

    LaunchedEffect(errorMessageText) {
        if (errorMessageText != null) {
            // Long duration: partial-failure text (e.g. one target set, the other not)
            // is easy to miss at Short.
            snackbarHostState.showSnackbar(errorMessageText, duration = SnackbarDuration.Long)
            viewModel.clearError()
        }
    }

    LaunchedEffect(successMessageText) {
        if (successMessageText != null) {
            snackbarHostState.showSnackbar(successMessageText)
            viewModel.clearSuccess()
        }
    }

    EditorContent(
        state = state,
        callbacks = EditorCallbacks(
            onBack = onBack,
            onReset = viewModel::resetToDefaults,
            onCropMode = viewModel::setCropMode,
            onTarget = viewModel::setWallpaperTarget,
            onFillMode = viewModel::setBackgroundFillMode,
            onFaceAware = viewModel::toggleFaceAware,
            onFocusTap = viewModel::updateManualFocusPoint,
            onViewingLock = viewModel::setViewingLock,
            onSave = viewModel::exportWallpaper,
            onApply = viewModel::applyWallpaper,
            onRetry = viewModel::retryRender
        ),
        snackbarHostState = snackbarHostState
    )
}

/**
 * Stateless editor UI (previewable). Viewing (Home/Lock tabs) and applying
 * (Apply-to + destination-specific button) are separate decisions. A stale
 * render stays visible with an "Updating" status, and a failed render offers
 * Retry while Apply/Save stay disabled.
 */
@Composable
internal fun EditorContent(
    state: EditorUiState,
    callbacks: EditorCallbacks,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    var isCleanPreview by remember { androidx.compose.runtime.mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (state.latestPublication != null && !state.isLoading && !isCleanPreview) {
                EditorBottomBar(state = state, callbacks = callbacks, haptic = haptic, view = view)
            }
        }
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            val availableWidth = maxWidth
            val previewMaxHeight = if (isCleanPreview) {
                (maxHeight * 0.75f).coerceIn(300.dp, 750.dp)
            } else {
                (maxHeight * 0.42f).coerceIn(240.dp, 500.dp)
            }

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.editor_loading_photo),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                return@BoxWithConstraints
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                EditorTopBar(
                    state = state,
                    callbacks = callbacks,
                    isCleanPreview = isCleanPreview,
                    onToggleCleanPreview = { isCleanPreview = !isCleanPreview }
                )

                if (state.latestPublication?.lock != null && !isCleanPreview) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(top = 2.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.editor_viewing),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        PreviewScreenTabs(
                            viewingLock = state.previewingLock,
                            onSelect = callbacks.onViewingLock
                        )
                    }
                }

                PreviewStage(
                    state = state,
                    callbacks = callbacks,
                    availableWidth = availableWidth,
                    previewMaxHeight = previewMaxHeight,
                    isCleanPreview = isCleanPreview
                )

                if (!isCleanPreview) {
                    StatusRow(state, callbacks)

                if (state.activeBitmap != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.editor_focus_linked),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }

                state.deviceProfile?.let { profile ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(
                            R.string.editor_device_info,
                            profile.manufacturer,
                            profile.model
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (state.activeBitmap != null) {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            stringResource(R.string.editor_launcher_disclaimer),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                    Spacer(Modifier.height(20.dp))

                    ControlsCard(state, callbacks)

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun EditorTopBar(
    state: EditorUiState,
    callbacks: EditorCallbacks,
    isCleanPreview: Boolean,
    onToggleCleanPreview: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = callbacks.onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            stringResource(R.string.editor_title),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (state.activeBitmap != null) {
            IconButton(onClick = onToggleCleanPreview) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = stringResource(
                        if (isCleanPreview) R.string.editor_exit_clean_preview else R.string.editor_clean_preview
                    ),
                    tint = if (isCleanPreview) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
        IconButton(onClick = callbacks.onReset, enabled = state.sourceImageMeta != null) {
            Icon(
                Icons.Default.RestartAlt,
                contentDescription = stringResource(R.string.editor_reset_defaults),
                tint = if (state.sourceImageMeta != null) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.outline
                }
            )
        }
    }
}

@Composable
private fun PreviewStage(
    state: EditorUiState,
    callbacks: EditorCallbacks,
    availableWidth: androidx.compose.ui.unit.Dp,
    previewMaxHeight: androidx.compose.ui.unit.Dp,
    isCleanPreview: Boolean = false
) {
    val stageWidth = minOf(
        availableWidth * 0.72f,
        previewMaxHeight * state.deviceAspectRatio
    ).coerceAtLeast(140.dp)

    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Crossfade(
            targetState = state.displayedPreview,
            animationSpec = tween(durationMillis = 220),
            label = "preview_crossfade"
        ) { displayed ->
            DevicePreviewFrame(
                bitmap = displayed?.bitmap,
                deviceAspectRatio = state.deviceAspectRatio,
                // Overlay focus comes from the displayed snapshot's resolved plan
                // focus — never from live editor state, so a stale image is never
                // paired with newer geometry.
                focusPoint = canvasFocusFor(displayed),
                // Hide the focus reticle in clean preview mode
                showFocusMarker = !isCleanPreview,
                // Focus editing requires a current (not retained) render.
                onFocusTap = if (state.isPreviewCurrent && !state.isBusy && !isCleanPreview) {
                    { tapped -> callbacks.onFocusTap(sourceFocusForTap(state, tapped)) }
                } else null,
                modifier = Modifier.width(stageWidth)
            )
        }
        // First render: there is no previous render to retain, so give the empty
        // frame explicit progress feedback instead of the idle placeholder.
        if (state.isRendering && state.activeBitmap == null) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.dp
            )
        }
    }

    if (state.isLowResolution) {
        Spacer(Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                stringResource(R.string.editor_low_resolution),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

/** Projects the displayed snapshot's resolved focus into canvas space for the overlay. */
private fun canvasFocusFor(preview: RenderedPreview?): FocusPoint? {
    preview ?: return null
    val plan = preview.plan
    val focus = plan.finalFocusPoint ?: return null
    return CropMath.sourceFocusToCanvasFocus(
        sourceFocus = focus,
        sourceWidth = preview.request.source.width,
        sourceHeight = preview.request.source.height,
        sourceCropRect = plan.sourceCropRect,
        outputImagePlacement = plan.outputImagePlacement,
        canvasWidth = plan.targetCanvasSpec.widthPx,
        canvasHeight = plan.targetCanvasSpec.heightPx
    )
}

/** Maps a tap on the displayed snapshot's bitmap back into source-image space. */
private fun sourceFocusForTap(
    state: EditorUiState,
    tapped: FocusPoint
): FocusPoint {
    val preview = state.displayedPreview ?: return tapped
    val plan = preview.plan
    return CropMath.canvasFocusToSourceFocus(
        canvasFocus = tapped,
        sourceWidth = preview.request.source.width,
        sourceHeight = preview.request.source.height,
        sourceCropRect = plan.sourceCropRect,
        outputImagePlacement = plan.outputImagePlacement,
        canvasWidth = plan.targetCanvasSpec.widthPx,
        canvasHeight = plan.targetCanvasSpec.heightPx
    )
}

@Composable
private fun StatusRow(state: EditorUiState, callbacks: EditorCallbacks) {
    if (!state.renderFailed && !state.isRendering && !state.isPreviewCurrent) return

    Spacer(Modifier.height(10.dp))
    when {
        state.renderFailed -> Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp)
        ) {
            StatusDot(MaterialTheme.colorScheme.error)
            Spacer(Modifier.width(6.dp))
            Text(
                stringResource(R.string.status_update_failed),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error
            )
            TextButton(onClick = callbacks.onRetry, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.action_retry), fontWeight = FontWeight.SemiBold)
            }
        }
        state.isRendering -> {
            val updatingText = if (state.latestPublication == null) {
                stringResource(R.string.status_preparing)
            } else {
                stringResource(R.string.status_updating)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                StatusDot(MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(6.dp))
                Text(
                    updatingText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        state.isPreviewCurrent -> {
            val success = LocalQgColors.current.success
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                StatusDot(success)
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.status_preview_ready),
                    style = MaterialTheme.typography.labelMedium,
                    color = success
                )
            }
        }
    }
}

@Composable
private fun StatusDot(color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun ControlsCard(state: EditorUiState, callbacks: EditorCallbacks) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            // Framing
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.editor_crop_mode),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    stringResource(state.cropMode.descriptionRes()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            ModeChipRow(
                selected = state.cropMode,
                onSelect = callbacks.onCropMode,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(16.dp))

            // Face-aware
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.editor_face_aware),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        stringResource(R.string.editor_face_aware_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    onCheckedChange = callbacks.onFaceAware,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.semantics { contentDescription = faceAwareToggleA11y }
                )
            }

            state.subjectAnalysis?.faces?.size?.let { count ->
                if (count > 0) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusDot(MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            pluralStringResource(R.plurals.editor_faces_detected, count, count),
                            style = MaterialTheme.typography.labelMedium,
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
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.editor_face_aware_unavailable),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Background finish — only meaningful when the crop can expose background.
            if (state.cropMode != CropMode.FILL) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.editor_background),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        stringResource(R.string.editor_background_when_space),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(8.dp))
                FillModeRow(
                    selected = state.backgroundFillMode,
                    onSelect = callbacks.onFillMode,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun EditorBottomBar(
    state: EditorUiState,
    callbacks: EditorCallbacks,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    view: android.view.View
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.editor_apply_to),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                WallpaperTargetTabs(
                    selected = state.wallpaperTarget,
                    onSelect = callbacks.onTarget
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = callbacks.onSave,
                    enabled = state.isPreviewCurrent && !state.isBusy,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = MaterialTheme.shapes.small
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
                        callbacks.onApply()
                    },
                    enabled = state.isPreviewCurrent && !state.isBusy,
                    modifier = Modifier
                        .weight(2f)
                        .height(52.dp),
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    if (state.isApplying || state.isExporting) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text(
                            stringResource(applyLabelRes(state.wallpaperTarget)),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

private fun applyLabelRes(target: WallpaperTarget): Int = when (target) {
    WallpaperTarget.HOME -> R.string.editor_apply_dest_home
    WallpaperTarget.LOCK -> R.string.editor_apply_dest_lock
    WallpaperTarget.BOTH -> R.string.editor_apply_dest_both
}

private fun CropMode.descriptionRes(): Int = when (this) {
    CropMode.SAFE_FIT -> R.string.crop_mode_safe_fit_desc
    CropMode.BALANCED -> R.string.crop_mode_balanced_desc
    CropMode.FILL -> R.string.crop_mode_fill_desc
}

@Preview(showBackground = true, widthDp = 400, heightDp = 880)
@Composable
private fun EditorContentPreview() {
    WallpaperCropFixerTheme {
        EditorContent(
            state = EditorUiState(),
            callbacks = EditorCallbacks(
                onBack = {}, onReset = {}, onCropMode = {}, onTarget = {},
                onFillMode = {}, onFaceAware = {}, onFocusTap = {}, onViewingLock = {},
                onSave = {}, onApply = {}, onRetry = {}
            )
        )
    }
}
