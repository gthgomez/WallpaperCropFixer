package com.wallpapercropfixer.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect

/**
 * Honest, labeled illustration of the app's outcome: a portrait screen where the
 * photo is contained with visible background bands instead of cropped away.
 * Deliberately abstract — the caption must say it is an example, not a promise
 * about every launcher.
 */
@Composable
fun FitDemoCanvas(modifier: Modifier = Modifier) {
    Canvas(modifier.aspectRatio(9f / 19f)) {
        val w = size.width
        val h = size.height

        // Device bezel
        drawRoundRect(
            color = Color(0xFF1A1A1A),
            cornerRadius = CornerRadius(w * 0.09f, w * 0.09f)
        )

        // Screen with a warm finish band that stays visible around the photo
        val inset = w * 0.05f
        val screenLeft = inset
        val screenTop = inset
        val screenW = w - inset * 2
        val screenH = h - inset * 2
        drawRoundRect(
            color = Color(0xFFE3D8C6),
            topLeft = Offset(screenLeft, screenTop),
            size = Size(screenW, screenH),
            cornerRadius = CornerRadius(w * 0.055f, w * 0.055f)
        )

        // The "photo", contained (not cropped) with margins on every side
        val marginX = screenW * 0.17f
        val marginY = screenH * 0.085f
        val photoLeft = screenLeft + marginX
        val photoTop = screenTop + marginY
        val photoW = screenW - marginX * 2
        val photoH = screenH - marginY * 2

        clipRect(photoLeft, photoTop, photoLeft + photoW, photoTop + photoH) {
            drawRect(
                color = Color(0xFFDCE4DE),
                topLeft = Offset(photoLeft, photoTop),
                size = Size(photoW, photoH)
            )
            drawCircle(
                color = Color(0xFFDC9673),
                radius = photoW * 0.20f,
                center = Offset(photoLeft + photoW * 0.64f, photoTop + photoH * 0.30f)
            )
            drawCircle(
                color = Color(0xFF365E5E),
                radius = photoW * 0.62f,
                center = Offset(photoLeft + photoW * 0.10f, photoTop + photoH * 1.12f)
            )
            drawCircle(
                color = Color(0xFF9BB2A0),
                radius = photoW * 0.52f,
                center = Offset(photoLeft + photoW * 0.98f, photoTop + photoH * 1.24f)
            )
        }
    }
}
