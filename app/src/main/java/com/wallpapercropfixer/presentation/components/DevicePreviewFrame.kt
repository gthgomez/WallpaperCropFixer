package com.wallpapercropfixer.presentation.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.wallpapercropfixer.R
import com.wallpapercropfixer.core.math.ViewportTransform
import com.wallpapercropfixer.domain.model.FocusPoint

/** Fraction of the canvas the focus moves per accessibility action. */
private const val focusStep = 0.05f

/**
 * Phone-shaped preview frame with a soft drop shadow (no hard border).
 *
 * The frame displays the rendered wallpaper bitmap with `ContentScale.Crop`,
 * which center-crops the bitmap when its aspect differs from the frame aspect
 * (e.g. a HOME canvas wider than the visible screen). [focusPoint] is expressed
 * in the rendered-bitmap normalized space; the overlay and tap handling convert
 * through [ViewportTransform] so the crosshair aligns with the subject and taps
 * map back to the correct bitmap position. The caller owns the frame width;
 * the height follows [deviceAspectRatio].
 */
@Composable
fun DevicePreviewFrame(
    bitmap: Bitmap?,
    modifier: Modifier = Modifier,
    deviceAspectRatio: Float = 9f / 19f,
    focusPoint: FocusPoint? = null,
    showFocusMarker: Boolean = true,
    onFocusTap: ((FocusPoint) -> Unit)? = null
) {
    val frameShape = RoundedCornerShape(28.dp)
    val haptic = LocalHapticFeedback.current
    val frameA11y = stringResource(R.string.preview_frame_a11y)
    val emptyText = stringResource(R.string.preview_empty)
    val centerLabel = stringResource(R.string.a11y_focus_center)
    val moveLeftLabel = stringResource(R.string.a11y_focus_move_left)
    val moveRightLabel = stringResource(R.string.a11y_focus_move_right)
    val moveUpLabel = stringResource(R.string.a11y_focus_move_up)
    val moveDownLabel = stringResource(R.string.a11y_focus_move_down)

    // Long-lived gesture/semantic handlers must observe the latest values without
    // restarting pointer detection on every recomposition.
    val currentOnFocusTap by rememberUpdatedState(onFocusTap)
    val currentFocusPoint by rememberUpdatedState(focusPoint)

    // TalkBack-reachable alternative to tap-to-reposition: directional focus
    // moves expressed in the same canvas-normalized space as [focusPoint].
    fun moveFocus(dx: Float, dy: Float): Boolean {
        val current = currentFocusPoint ?: return false
        val onFocus = currentOnFocusTap ?: return false
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onFocus(
            FocusPoint(
                xNormalized = (current.xNormalized + dx).coerceIn(0f, 1f),
                yNormalized = (current.yNormalized + dy).coerceIn(0f, 1f)
            )
        )
        return true
    }

    fun centerFocus(): Boolean {
        val onFocus = currentOnFocusTap ?: return false
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onFocus(FocusPoint(xNormalized = 0.5f, yNormalized = 0.5f))
        return true
    }

    val focusActions = if (onFocusTap != null) buildList {
        if (focusPoint != null) {
            add(CustomAccessibilityAction(moveLeftLabel) { moveFocus(-focusStep, 0f) })
            add(CustomAccessibilityAction(moveRightLabel) { moveFocus(focusStep, 0f) })
            add(CustomAccessibilityAction(moveUpLabel) { moveFocus(0f, -focusStep) })
            add(CustomAccessibilityAction(moveDownLabel) { moveFocus(0f, focusStep) })
        }
        // Establishes an initial focus when none exists, and resets to center otherwise.
        add(CustomAccessibilityAction(centerLabel) { centerFocus() })
    } else emptyList()

    Box(
        modifier = modifier
            .aspectRatio(deviceAspectRatio)
            // Soft elevation shadow instead of a hard border — modern photo-app look
            .shadow(
                elevation = if (bitmap != null) 16.dp else 4.dp,
                shape = frameShape,
                ambientColor = Color(0x33000000),
                spotColor = Color(0x44000000)
            )
            .clip(frameShape)
            .background(Color(0xFF1A1A1A))
            .then(
                if (onFocusTap != null)
                    Modifier.semantics(mergeDescendants = true) {
                        contentDescription = frameA11y
                        customActions = focusActions
                    }
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            val bitmapAspect = bitmap.width.toFloat() / bitmap.height.toFloat()

            Image(
                bitmap = bitmap.asImageBitmap(),
                // Announced once via the merged frame node above — a separate
                // image description would double the TalkBack announcement.
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (onFocusTap != null)
                            Modifier.pointerInput(bitmap.width, bitmap.height) {
                                detectTapGestures { offset ->
                                    val onTap = currentOnFocusTap ?: return@detectTapGestures
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val point = ViewportTransform.viewportToBitmap(
                                        x = offset.x / size.width,
                                        y = offset.y / size.height,
                                        bitmapAspect = bitmapAspect,
                                        viewportAspect = size.width.toFloat() / size.height.toFloat()
                                    )
                                    onTap(
                                        FocusPoint(
                                            xNormalized = point.x,
                                            yNormalized = point.y
                                        )
                                    )
                                }
                            }
                        else Modifier
                    )
            ) {
                if (showFocusMarker) {
                    focusPoint?.let { fp ->
                        val viewportPoint = ViewportTransform.bitmapToViewport(
                            x = fp.xNormalized,
                            y = fp.yNormalized,
                            bitmapAspect = bitmapAspect,
                            viewportAspect = size.width / size.height
                        )
                    val cx = viewportPoint.x * size.width
                    val cy = viewportPoint.y * size.height
                    val center = Offset(cx, cy)
                    // Density-scaled so the marker is identical in dp on every screen.
                    val ring = 11.dp.toPx()
                    val halo = ring + 3.dp.toPx()
                    val dot = 2.5.dp.toPx()

                    drawCircle(
                        color = Color.Black.copy(alpha = 0.45f),
                        radius = halo,
                        center = center
                    )
                    drawCircle(
                        color = Color.White,
                        radius = ring,
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    drawCircle(
                        color = Color.White,
                        radius = dot,
                        center = center
                    )
                }
            }
        }
        } else {
            Text(
                text = emptyText,
                color = Color(0xFF666666),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
