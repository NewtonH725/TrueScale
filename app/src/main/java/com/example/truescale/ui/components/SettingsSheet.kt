package com.example.truescale.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.truescale.R
import com.example.truescale.engine.DisplayMetrologyEngine
import com.example.truescale.ui.theme.BullMooseOrange
import com.example.truescale.ui.theme.TabularNumberStyle
import com.example.truescale.ui.theme.TabularSubReadout
import com.example.truescale.ui.theme.ThemeMode
import com.example.truescale.utils.LocaleManager
import java.util.Locale

@Composable
fun SettingsSheet(
    engine: DisplayMetrologyEngine,
    hapticsEnabled: Boolean,
    onHapticsToggled: (Boolean) -> Unit,
    currentThemeMode: ThemeMode,
    onThemeModeChanged: (ThemeMode) -> Unit,
    currentLanguage: String,
    onLanguageChanged: (String) -> Unit,
    onResetAllTares: () -> Unit,
    onResetDisplayFactor: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar with Back Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Indietro",
                    tint = colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.settings_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Preferences Card
        SettingsCard(title = stringResource(R.string.settings_preferences), icon = Icons.Default.Vibration) {
            // Haptics row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_haptics),
                        style = TabularSubReadout,
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.settings_haptics_desc),
                        style = TabularNumberStyle.copy(fontSize = 11.sp),
                        color = colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = hapticsEnabled,
                    onCheckedChange = onHapticsToggled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = BullMooseOrange,
                        checkedTrackColor = BullMooseOrange.copy(alpha = 0.35f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Interface Theme Selector (Sistema / Chiaro / Scuro)
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.settings_theme),
                    style = TabularSubReadout,
                    color = colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ThemeMode.entries.forEach { mode ->
                        val isSelected = (mode == currentThemeMode)
                        val btnLabel = when (mode) {
                            ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
                            ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
                            ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
                        }
                        if (isSelected) {
                            Button(
                                onClick = { onThemeModeChanged(mode) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = BullMooseOrange)
                            ) {
                                Text(btnLabel, style = TabularNumberStyle.copy(fontSize = 11.sp))
                            }
                        } else {
                            OutlinedButton(
                                onClick = { onThemeModeChanged(mode) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(btnLabel, style = TabularNumberStyle.copy(fontSize = 11.sp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Language Selector (Italiano / English / Sistema)
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.settings_language),
                    style = TabularSubReadout,
                    color = colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val languages = listOf(
                        LocaleManager.LANG_SYSTEM to stringResource(R.string.settings_language_system),
                        LocaleManager.LANG_IT to stringResource(R.string.settings_language_it),
                        LocaleManager.LANG_EN to stringResource(R.string.settings_language_en)
                    )
                    languages.forEach { (code, label) ->
                        val isSelected = (code == currentLanguage)
                        if (isSelected) {
                            Button(
                                onClick = { onLanguageChanged(code) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = BullMooseOrange)
                            ) {
                                Text(label, style = TabularNumberStyle.copy(fontSize = 11.sp), maxLines = 1)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { onLanguageChanged(code) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(label, style = TabularNumberStyle.copy(fontSize = 11.sp), maxLines = 1)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Display Metrology Info Card
        SettingsCard(title = stringResource(R.string.settings_display_hardware), icon = Icons.Default.Smartphone) {
            InfoRow("DPI Orizzontale (xdpi)", String.format(Locale.US, "%.2f dpi", engine.xdpi))
            InfoRow("DPI Verticale (ydpi)", String.format(Locale.US, "%.2f dpi", engine.ydpi))
            InfoRow("Rapporto di Scala Hardware", String.format(Locale.US, "%.3fx / %.3fy", engine.scaleX, engine.scaleY))
            InfoRow("Fattore di Calibrazione", String.format(Locale.US, "%.4fx", engine.calibrationFactor))
            InfoRow("Densità Metrica X", String.format(Locale.US, "%.3f px/mm", engine.pxPerMmX))
            InfoRow("Densità Metrica Y", String.format(Locale.US, "%.3f px/mm", engine.pxPerMmY))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Inertial Sensor Hub Info Card
        SettingsCard(title = stringResource(R.string.settings_sensor_hub), icon = Icons.Default.Sensors) {
            InfoRow("Sensore Gravità Primario", "Sensor.TYPE_GRAVITY (Fallback Accel)")
            InfoRow("Frequenza Campionamento", "SENSOR_DELAY_GAME (~50–100 Hz)")
            InfoRow("Filtro Passa-Basso", "EMA Adattivo (α=0,06 rest / α=0,25 dyn)")
            InfoRow("Soglia di Blocco Precisione", "±0,1° (Conferma Aptica)")
            InfoRow("Matrice di Tara", "5 Profili Meccanici Indipendenti")
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quick Reset Actions Card
        SettingsCard(title = stringResource(R.string.settings_reset_actions), icon = Icons.Default.Refresh) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onResetAllTares,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.settings_reset_tares), style = TabularNumberStyle.copy(fontSize = 11.sp))
                }
                OutlinedButton(
                    onClick = onResetDisplayFactor,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.settings_reset_display), style = TabularNumberStyle.copy(fontSize = 11.sp))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // App & Framework Architecture Card
        SettingsCard(title = stringResource(R.string.settings_architecture), icon = Icons.Default.Info) {
            InfoRow("Brand Design", "CrossFit Bull Moose (www.crossfitbullmoose.com)")
            InfoRow("Versione App", "v1.3.0 (Build 10300)")
            InfoRow("Framework", "DOE (Directives, Orchestration, Execution)")
            InfoRow("Standard Metrologici", "ISO/IEC 7810 (ID-1), ISO 8653 (Anelli EU)")
            InfoRow("Rendering Pipeline", "120 Hz Zero-Allocation Canvas")
        }

        Spacer(modifier = Modifier.navigationBarsPadding().height(24.dp))
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = BullMooseOrange,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = TabularNumberStyle.copy(fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                    color = colorScheme.onSurface
                )
            }
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = TabularNumberStyle.copy(fontSize = 12.sp),
            color = colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = TabularNumberStyle.copy(fontSize = 12.sp, color = colorScheme.onSurface)
        )
    }
}
