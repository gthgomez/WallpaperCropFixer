package com.wallpapercropfixer.presentation.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.wallpapercropfixer.domain.model.FocusPoint

/**
 * Phone-shaped preview frame with a soft drop shadow (no hard border).
 * Matches the aesthetic of Google Photos / Snapseed crop views.
 *
 * The overlay Canvas is sized identically to the image area, so tap coordinates
 * and drawn crosshair always align with what the user sees.
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

    Box(
        modifier = modifier
            .fillMaxWidth(0.55f)
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
                    Modifier.semantics { contentDescription = "Tap to reposition crop focus point" }
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Wallpaper preview",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (onFocusTap != null)
                            Modifier.pointerInput(onFocusTap) {
                                detectTapGestures { offset ->
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onFocusTap(
                                        FocusPoint(
                                            xNormalized = (offset.x / size.width).coerceIn(0f, 1f),
                                            yNormalized = (offset.y / size.height).coerceIn(0f, 1f)
                                        )
                                    )
                                }
                            }
                        else Modifier
                    )
            ) {
                focusPoint?.let { fp ->
                    val cx = fp.xNormalized * size.width
                    val cy = fp.yNormalized * size.height
                    val center = Offset(cx, cy)
                    val ring = 22f
                    val dot = 6f

                    drawCircle(
                        color = Color.Black.copy(alpha = 0.35f),
                        radius = ring + 6f,
                        center = center
                    )
                    drawCircle(
                        color = Color.White,
                        radius = ring,
                        center = center,
                        style = Stroke(width = 2.5f)
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
                text = "Select a photo to preview",
                color = Color(0xFF666666),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
