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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
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
            FilterChip(
                selected = mode == selected,
                onClick = { onSelect(mode) },
                label = { Text(mode.label()) },
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .semantics { contentDescription = mode.accessibilityDescription() }
            )
        }
    }
}

private fun CropMode.label() = when (this) {
    CropMode.SAFE_FIT -> "Safe Fit"
    CropMode.BALANCED -> "Balanced"
    CropMode.FILL -> "Fill"
}

private fun CropMode.accessibilityDescription() = when (this) {
    CropMode.SAFE_FIT -> "Crop mode: Safe Fit — preserves the full photo, adds padding if needed"
    CropMode.BALANCED -> "Crop mode: Balanced — balances coverage and subject preservation"
    CropMode.FILL -> "Crop mode: Fill — covers the entire screen, may crop edges"
}
