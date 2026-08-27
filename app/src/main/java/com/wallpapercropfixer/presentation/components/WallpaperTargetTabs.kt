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
import com.wallpapercropfixer.domain.model.WallpaperTarget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperTargetTabs(
    selected: WallpaperTarget,
    onSelect: (WallpaperTarget) -> Unit,
    modifier: Modifier = Modifier
) {
    val targets = listOf(WallpaperTarget.HOME, WallpaperTarget.LOCK, WallpaperTarget.BOTH)

    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        targets.forEachIndexed { index, target ->
            SegmentedButton(
                selected = target == selected,
                onClick = { onSelect(target) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = targets.size),
                label = { Text(stringResource(target.labelRes())) }
            )
        }
    }
}

private fun WallpaperTarget.labelRes(): Int = when (this) {
    WallpaperTarget.HOME -> R.string.target_home
    WallpaperTarget.LOCK -> R.string.target_lock
    WallpaperTarget.BOTH -> R.string.target_both
}