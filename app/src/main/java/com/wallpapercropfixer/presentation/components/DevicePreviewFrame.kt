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
 * map back to the correct bitmap position. The caller sizes the frame through
 * its modifier (explicit width); height follows [deviceAspectRatio].
 */
@Composable
fun DevicePreviewFrame(
    bitmap: Bitmap?,
    modifier: Modifier = Modifier,
    deviceAspectRatio: Float = 9f / 19f,
    focusPoint: FocusPoint? = null,
    onFocusTap: ((FocusPoint) -> Unit)? = null
) {
    val frameShape = RoundedCornerShape(28.dp)
    val haptic = LocalHapticFeedback.current
    // Keep the latest callback without restarting the tap detector on recomposition.
    val latestOnFocusTap by rememberUpdatedState(onFocusTap)
    val frameA11y = stringResource(R.string.preview_frame_a11y)
    val emptyText = stringResource(R.string.preview_empty)
    val moveLeftLabel = stringResource(R.string.a11y_focus_move_left)
    val moveRightLabel = stringResource(R.string.a11y_focus_move_right)
    val moveUpLabel = stringResource(R.string.a11y_focus_move_up)
    val moveDownLabel = stringResource(R.string.a11y_focus_move_down)

    // TalkBack-reachable alternative to tap-to-reposition: directional focus
    // moves expressed in the same canvas-normalized space as [focusPoint].
    fun moveFocus(dx: Float, dy: Float): Boolean {
        val current = focusPoint ?: return false
        val onFocus = onFocusTap ?: return false
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onFocus(
            FocusPoint(
                xNormalized = (current.xNormalized + dx).coerceIn(0f, 1f),
                yNormalized = (current.yNormalized + dy).coerceIn(0f, 1f)
            )
        )
        return true
    }

    val focusActions = if (onFocusTap != null) listOf(
        CustomAccessibilityAction(moveLeftLabel) { moveFocus(-focusStep, 0f) },
        CustomAccessibilityAction(moveRightLabel) { moveFocus(focusStep, 0f) },
        CustomAccessibilityAction(moveUpLabel) { moveFocus(0f, -focusStep) },
        CustomAccessibilityAction(moveDownLabel) { moveFocus(0f, focusStep) }
    ) else emptyList()

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
                    // Merge descendants so the frame description is announced once
                    // (the image itself carries no description).
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
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val point = ViewportTransform.viewportToBitmap(
                                        x = offset.x / size.width,
                                        y = offset.y / size.height,
                                        bitmapAspect = bitmapAspect,
                                        viewportAspect = size.width.toFloat() / size.height.toFloat()
                                    )
                                    latestOnFocusTap?.invoke(
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
                    // Density-independent crosshair so it keeps its size across screens.
                    val ring = 22.dp.toPx()
                    val dot = 6.dp.toPx()

                    drawCircle(
                        color = Color.Black.copy(alpha = 0.35f),
                        radius = ring + dot,
                        center = center
                    )
                    drawCircle(
                        color = Color.White,
                        radius = ring,
                        center = center,
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                    drawCircle(
                        color = Color.White,
                        radius = dot,
                        center = center
                    )
                }
            }
        } else {
            Text(
                text = emptyText,
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}