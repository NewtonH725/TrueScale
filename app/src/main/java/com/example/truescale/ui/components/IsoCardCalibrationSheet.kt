package com.example.truescale.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.truescale.R
import com.example.truescale.engine.DisplayMetrologyEngine
import com.example.truescale.engine.TareProfile
import com.example.truescale.ui.theme.BullMooseOrange
import com.example.truescale.ui.theme.TabularHeroReadout
import com.example.truescale.ui.theme.TabularNumberStyle
import com.example.truescale.ui.theme.TabularSubReadout
import java.util.Locale

@Composable
fun IsoCardCalibrationSheet(
    engine: DisplayMetrologyEngine,
    currentFactor: Float,
    onSaveFactor: (Float) -> Unit,
    onResetFactor: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var draftFactor by remember(currentFactor) { mutableFloatStateOf(currentFactor) }

    // Pre-allocated stroke styles (Zero Canvas allocations at 120 Hz)
    val cardStroke = remember { Stroke(width = 2.5f) }
    val hairlineStroke = remember { Stroke(width = 1.0f) }
    val guideCollimationStroke = remember { Stroke(width = 3.0f) }

    // Temporary engine reflecting draft factor for real-time visual adjustment
    val previewEngine = remember(draftFactor, engine) {
        DisplayMetrologyEngine(
            xdpi = engine.xdpi,
            ydpi = engine.ydpi,
            scaleX = engine.scaleX,
            scaleY = engine.scaleY,
            calibrationFactor = draftFactor
        )
    }

    val colorScheme = MaterialTheme.colorScheme

    // ISO/IEC 7810 ID-1 standard dimensions oriented VERTICALLY to physically fit portrait phone screens:
    // Width = 53.98 mm, Height = 85.60 mm
    val cardWidthPx = previewEngine.mmToPxX(DisplayMetrologyEngine.ISO_CARD_HEIGHT_MM) // 53.98 mm width in portrait
    val cardHeightPx = previewEngine.mmToPxY(DisplayMetrologyEngine.ISO_CARD_WIDTH_MM) // 85.60 mm height in portrait
    val cornerRadiusPx = previewEngine.mmToPxX(DisplayMetrologyEngine.ISO_CARD_CORNER_RADIUS_MM)

    // Strict 3-zone non-overlapping vertical column
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ====================================================================
        // Zone 1: Ultra-compact Header (Title + 1-line instructions + back button)
        // ====================================================================
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
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.calibration_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.calibration_instructions),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // ====================================================================
        // Zone 2: Dedicated Physical Card Placement Area (weight(1f), clipToBounds)
        // ====================================================================
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clipToBounds()
                .background(colorScheme.surface, RoundedCornerShape(16.dp))
                .border(1.dp, colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val startX = (size.width - cardWidthPx) / 2f
                val startY = (size.height - cardHeightPx) / 2f

                // Translucent card physical body
                drawRoundRect(
                    color = BullMooseOrange.copy(alpha = 0.12f),
                    topLeft = Offset(startX, startY),
                    size = Size(cardWidthPx, cardHeightPx),
                    cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                )

                // Fixed reference edge (Top/Left)
                drawLine(
                    color = colorScheme.onSurface,
                    start = Offset(startX, startY),
                    end = Offset(startX + cardWidthPx, startY),
                    strokeWidth = 2.0f
                )

                // Card border outline
                drawRoundRect(
                    color = colorScheme.outline,
                    topLeft = Offset(startX, startY),
                    size = Size(cardWidthPx, cardHeightPx),
                    cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                    style = cardStroke
                )

                // Active Flame Orange Collimation Guide (Bottom edge: 85.60 mm)
                val bottomY = startY + cardHeightPx
                drawLine(
                    color = BullMooseOrange,
                    start = Offset(startX - 12f, bottomY),
                    end = Offset(startX + cardWidthPx + 12f, bottomY),
                    strokeWidth = guideCollimationStroke.width
                )

                // Diagonal alignment guides
                drawLine(
                    color = colorScheme.outlineVariant.copy(alpha = 0.35f),
                    start = Offset(startX, startY),
                    end = Offset(startX + cardWidthPx, startY + cardHeightPx),
                    strokeWidth = hairlineStroke.width
                )
                drawLine(
                    color = colorScheme.outlineVariant.copy(alpha = 0.35f),
                    start = Offset(startX, startY + cardHeightPx),
                    end = Offset(startX + cardWidthPx, startY),
                    strokeWidth = hairlineStroke.width
                )
            }

            // Dimension overlay indicators
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.card_dimensions_label),
                    style = TabularSubReadout.copy(fontWeight = FontWeight.Bold),
                    color = BullMooseOrange
                )
                Text(
                    text = String.format(Locale.US, "%.1f px × %.1f px", cardWidthPx, cardHeightPx),
                    style = TabularNumberStyle.copy(fontSize = 11.sp),
                    color = colorScheme.onSurfaceVariant
                )
            }
        }

        // ====================================================================
        // Zone 3: Compact Bottom Controls Card (Zero Overlap with Card Zone)
        // ====================================================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant),
            border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Value & delta row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.calibration_factor_label),
                        style = TabularNumberStyle.copy(fontSize = 12.sp),
                        color = colorScheme.onSurfaceVariant
                    )
                    val percentDelta = (draftFactor - 1.0000f) * 100f
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = String.format(Locale.US, "%.4fx", draftFactor),
                            style = TabularHeroReadout.copy(fontSize = 20.sp),
                            color = BullMooseOrange
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = String.format(Locale.US, "(%+.2f%%)", percentDelta),
                            style = TabularSubReadout.copy(fontSize = 12.sp),
                            color = if (kotlin.math.abs(percentDelta) < 0.01f) BullMooseOrange else colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Fine slider
                Slider(
                    value = draftFactor,
                    onValueChange = { draftFactor = it },
                    valueRange = DisplayMetrologyEngine.MIN_CALIBRATION_FACTOR..DisplayMetrologyEngine.MAX_CALIBRATION_FACTOR,
                    steps = 600,
                    colors = SliderDefaults.colors(
                        thumbColor = BullMooseOrange,
                        activeTrackColor = BullMooseOrange,
                        inactiveTrackColor = colorScheme.outlineVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Steppers row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = { draftFactor = (draftFactor - 0.005f).coerceAtLeast(0.85f) },
                        content = { Text("-0.5%", style = TabularNumberStyle.copy(fontSize = 11.sp)) }
                    )
                    IconButton(
                        onClick = { draftFactor = (draftFactor - 0.0005f).coerceAtLeast(0.85f) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Step -0.05%", tint = BullMooseOrange)
                    }
                    OutlinedButton(
                        onClick = { draftFactor = 1.0000f }
                    ) {
                        Text("1.0000", style = TabularNumberStyle.copy(fontSize = 11.sp))
                    }
                    IconButton(
                        onClick = { draftFactor = (draftFactor + 0.0005f).coerceAtMost(1.15f) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Step +0.05%", tint = BullMooseOrange)
                    }
                    FilledTonalButton(
                        onClick = { draftFactor = (draftFactor + 0.005f).coerceAtMost(1.15f) },
                        content = { Text("+0.5%", style = TabularNumberStyle.copy(fontSize = 11.sp)) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom action buttons: Reset and Confirm
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            draftFactor = 1.0000f
                            onResetFactor()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.btn_reset), style = TabularNumberStyle.copy(fontSize = 13.sp))
                    }
                    Button(
                        onClick = { onSaveFactor(draftFactor) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BullMooseOrange,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.btn_save), style = TabularNumberStyle.copy(fontSize = 13.sp))
                    }
                }
            }
        }
    }
}
