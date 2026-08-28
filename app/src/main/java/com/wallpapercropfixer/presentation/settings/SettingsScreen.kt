package com.wallpapercropfixer.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wallpapercropfixer.R
import com.wallpapercropfixer.presentation.components.ModeChipRow
import com.wallpapercropfixer.presentation.components.WallpaperTargetTabs

/**
 * Canonical public privacy policy URL (GitHub Pages). The repo-side publishing
 * workflow (see .github/workflows/pages.yml) renders PRIVACY.md to
 * /PRIVACY.html. Enabling GitHub Pages for the repository is an OWNER ACTION.
 */
internal const val PRIVACY_URL = "https://gthgomez.github.io/WallpaperCropFixer/PRIVACY.html"

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    Scaffold(containerColor = Color(0xFFFAFAFA)) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Custom top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = Color(0xFF333333)
                    )
                }
                Text(
                    stringResource(R.string.settings),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF111111)
                )
            }

            Spacer(Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SettingsCard(title = stringResource(R.string.settings_default_crop_mode)) {
                    ModeChipRow(
                        selected = settings.defaultCropMode,
                        onSelect = { viewModel.update(settings.copy(defaultCropMode = it)) }
                    )
                }

                SettingsCard(title = stringResource(R.string.settings_default_target)) {
                    WallpaperTargetTabs(
                        selected = settings.defaultWallpaperTarget,
                        onSelect = { viewModel.update(settings.copy(defaultWallpaperTarget = it)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                SettingsCard(title = stringResource(R.string.settings_face_aware_default)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            stringResource(R.string.settings_face_aware_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF888888)
                        )
                        val settingsFaceOn = stringResource(R.string.editor_face_on)
                        val settingsFaceOff = stringResource(R.string.editor_face_off)
                        val settingsFaceToggleA11y = stringResource(
                            R.string.settings_face_aware_toggle,
                            if (settings.defaultFaceAwareEnabled) settingsFaceOn else settingsFaceOff
                        )
                        Switch(
                            checked = settings.defaultFaceAwareEnabled,
                            onCheckedChange = { viewModel.update(settings.copy(defaultFaceAwareEnabled = it)) },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.semantics {
                                contentDescription = settingsFaceToggleA11y
                            }
                        )
                    }
                }

                // Export quality card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        // Local state drives the slider during drag; syncs from external settings changes
                        // only when the delta is meaningful (> 0.5) to avoid resetting mid-drag from
                        // our own onValueChangeFinished write flowing back through the DataStore.
                        var localQuality by remember { mutableFloatStateOf(settings.exportJpegQuality.toFloat()) }
                        LaunchedEffect(settings.exportJpegQuality) {
                            if (kotlin.math.abs(localQuality - settings.exportJpegQuality) > 0.5f) {
                                localQuality = settings.exportJpegQuality.toFloat()
                            }
                        }
                        val qualityDescription = stringResource(
                            R.string.settings_export_quality_value,
                            localQuality.toInt()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.settings_export_quality),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = Color(0xFF111111),
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    stringResource(R.string.settings_export_quality_value, localQuality.toInt()),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Slider(
                            value = localQuality,
                            onValueChange = { localQuality = it },
                            onValueChangeFinished = {
                                viewModel.update(settings.copy(exportJpegQuality = localQuality.toInt()))
                            },
                            valueRange = 60f..100f,
                            steps = 39,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics {
                                     contentDescription = qualityDescription
                                 }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(stringResource(R.string.settings_export_quality_min), style = MaterialTheme.typography.labelSmall, color = Color(0xFFBBBBBB))
                            Text(stringResource(R.string.settings_export_quality_max), style = MaterialTheme.typography.labelSmall, color = Color(0xFFBBBBBB))
                        }
                    }
                }

                TextButton(
                    onClick = { uriHandler.openUri(PRIVACY_URL) },
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                ) {
                    Text(stringResource(R.string.settings_privacy_policy))
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFF111111)
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}
