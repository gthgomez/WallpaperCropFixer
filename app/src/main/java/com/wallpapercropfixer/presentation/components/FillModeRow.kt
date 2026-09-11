package com.wallpapercropfixer.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wallpapercropfixer.R
import com.wallpapercropfixer.domain.model.BackgroundFillMode

/**
 * Blur / Solid / Gradient finish selector with labeled color swatches.
 *
 * Callers should only show this row when the selected crop mode can expose
 * background (a plan without padding leaves nothing for the finish to paint).
 */
@Composable
fun FillModeRow(
    selected: BackgroundFillMode,
    onSelect: (BackgroundFillMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BackgroundFillMode.entries.forEach { mode ->
            FilterChip(
                selected = mode == selected,
                onClick = { onSelect(mode) },
                leadingIcon = { FillSwatch(mode) },
                label = { Text(stringResource(mode.labelRes())) },
                modifier = Modifier
                    .padding(vertical = 2.dp)
                    .heightIn(min = 48.dp)
            )
        }
    }
}

/** Illustrative swatches suggesting what each finish paints behind the photo. */
@Composable
private fun FillSwatch(mode: BackgroundFillMode) {
    val brush = when (mode) {
        BackgroundFillMode.BLUR -> Brush.linearGradient(
            listOf(Color(0xFFA2BBA9), Color(0xFFD7B9A0))
        )
        BackgroundFillMode.SOLID -> Brush.linearGradient(
            listOf(Color(0xFF4A4440), Color(0xFF4A4440))
        )
        BackgroundFillMode.GRADIENT -> Brush.linearGradient(
            listOf(Color(0xFF597773), Color(0xFFD1AA88))
        )
    }
    Box(
        modifier = Modifier
            .size(14.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(brush)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
    )
}

private fun BackgroundFillMode.labelRes(): Int = when (this) {
    BackgroundFillMode.BLUR -> R.string.fill_blur
    BackgroundFillMode.SOLID -> R.string.fill_solid
    BackgroundFillMode.GRADIENT -> R.string.fill_gradient
}
