package com.wallpapercropfixer.presentation.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wallpapercropfixer.R

/**
 * Home / Lock preview selector. This chooses which render is *displayed* only —
 * it never changes the Apply destination and never triggers a render.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreenTabs(
    viewingLock: Boolean,
    onSelect: (viewingLock: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        SegmentedButton(
            selected = !viewingLock,
            onClick = { onSelect(false) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            label = { Text(stringResource(R.string.preview_screen_home)) }
        )
        SegmentedButton(
            selected = viewingLock,
            onClick = { onSelect(true) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            label = { Text(stringResource(R.string.preview_screen_lock)) }
        )
    }
}
