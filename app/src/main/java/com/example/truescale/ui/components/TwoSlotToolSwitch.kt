package com.example.truescale.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.truescale.MainAppTab
import com.example.truescale.ui.theme.TabularNumberStyle
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Compact 2-Slot Vertical HUD Switch (Regolo / Livella).
 * Features an orange optical lens indicator that can be smoothly dragged up/down
 * like a physical on-off selector, or toggled with a single tap.
 */
@Composable
fun TwoSlotToolSwitch(
    selectedTab: MainAppTab,
    onTabSelected: (MainAppTab) -> Unit,
    onHapticTick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 142.dp,
    height: Dp = 74.dp
) {
    val isLevelSelected = selectedTab == MainAppTab.LEVEL
    val colorScheme = MaterialTheme.colorScheme
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    val outerPadding = 4.dp
    val slotHeight = (height - (outerPadding * 2)) / 2

    val slotHeightPx = with(density) { slotHeight.toPx() }
    val maxOffsetPx = slotHeightPx

    // Animated Y offset in pixels
    val animOffsetY = remember { Animatable(if (isLevelSelected) maxOffsetPx else 0f) }
    var isDragging by remember { mutableStateOf(false) }
    var dragProgressPx by remember { mutableFloatStateOf(if (isLevelSelected) maxOffsetPx else 0f) }

    // Sync animation with external tab changes (e.g. back gesture) when not dragging
    LaunchedEffect(isLevelSelected) {
        if (!isDragging) {
            val targetPx = if (isLevelSelected) maxOffsetPx else 0f
            animOffsetY.animateTo(
                targetValue = targetPx,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    Card(
        modifier = modifier
            .width(width)
            .height(height)
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    dragProgressPx = (dragProgressPx + delta).coerceIn(0f, maxOffsetPx)
                    coroutineScope.launch {
                        animOffsetY.snapTo(dragProgressPx)
                    }
                },
                onDragStarted = {
                    isDragging = true
                    dragProgressPx = animOffsetY.value
                },
                onDragStopped = {
                    isDragging = false
                    val midpoint = maxOffsetPx / 2f
                    val newIsLevel = dragProgressPx > midpoint
                    val targetPx = if (newIsLevel) maxOffsetPx else 0f

                    coroutineScope.launch {
                        animOffsetY.animateTo(
                            targetValue = targetPx,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                    }

                    val newTab = if (newIsLevel) MainAppTab.LEVEL else MainAppTab.RULER
                    if (newTab != selectedTab) {
                        onHapticTick()
                        onTabSelected(newTab)
                    }
                }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceVariant.copy(alpha = 0.16f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            colorScheme.outlineVariant.copy(alpha = 0.30f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(outerPadding)
        ) {
            // Sliding Orange Optical Highlight (The draggable ON/OFF selector indicator)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(slotHeight)
                    .offset { IntOffset(x = 0, y = animOffsetY.value.roundToInt()) }
                    .clip(RoundedCornerShape(12.dp))
                    .background(colorScheme.primary.copy(alpha = 0.20f))
                    .border(1.5.dp, colorScheme.primary.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
            )

            // Two Fixed Slots (Slot 1: Regolo | Slot 2: Livella)
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Slot: REGOLO
                val isRulerActive = !isLevelSelected
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(slotHeight)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (isLevelSelected) {
                                onHapticTick()
                                onTabSelected(MainAppTab.RULER)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = MainAppTab.RULER.icon,
                            contentDescription = null,
                            tint = if (isRulerActive) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.50f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(MainAppTab.RULER.titleRes),
                            style = TabularNumberStyle.copy(
                                fontSize = 13.sp,
                                fontWeight = if (isRulerActive) FontWeight.Bold else FontWeight.Normal,
                                color = if (isRulerActive) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.55f)
                            ),
                            maxLines = 1,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Bottom Slot: LIVELLA
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(slotHeight)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (!isLevelSelected) {
                                onHapticTick()
                                onTabSelected(MainAppTab.LEVEL)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = MainAppTab.LEVEL.icon,
                            contentDescription = null,
                            tint = if (isLevelSelected) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.50f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(MainAppTab.LEVEL.titleRes),
                            style = TabularNumberStyle.copy(
                                fontSize = 13.sp,
                                fontWeight = if (isLevelSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isLevelSelected) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.55f)
                            ),
                            maxLines = 1,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
