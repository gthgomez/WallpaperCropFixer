package com.wallpapercropfixer.presentation.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.wallpapercropfixer.R
import com.wallpapercropfixer.domain.model.CropMode

@Composable
fun ModeChipRow(
    selected: CropMode,
    onSelect: (CropMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CropMode.entries.forEach { mode ->
            val label = stringResource(mode.labelRes())
            val a11y = stringResource(mode.a11yRes())
            FilterChip(
                selected = mode == selected,
                onClick = { onSelect(mode) },
                label = { Text(label) },
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .semantics { contentDescription = a11y }
            )
        }
    }
}

private fun CropMode.labelRes(): Int = when (this) {
    CropMode.SAFE_FIT -> R.string.crop_mode_safe_fit
    CropMode.BALANCED -> R.string.crop_mode_balanced
    CropMode.FILL -> R.string.crop_mode_fill
}

private fun CropMode.a11yRes(): Int = when (this) {
    CropMode.SAFE_FIT -> R.string.crop_mode_safe_fit_a11y
    CropMode.BALANCED -> R.string.crop_mode_balanced_a11y
    CropMode.FILL -> R.string.crop_mode_fill_a11y
}