package com.wallpapercropfixer.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The app's brand mark: a framed crop window with a focus point. Used on the
 * entry screen; the same silhouette anchors the launcher icon language.
 */
@Composable
fun CropMarkBadge(
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    markColor: Color = MaterialTheme.colorScheme.primary
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.3f))
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size * 0.52f)
                .border(2.dp, markColor, RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .size(size * 0.12f)
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(markColor)
            )
        }
    }
}
