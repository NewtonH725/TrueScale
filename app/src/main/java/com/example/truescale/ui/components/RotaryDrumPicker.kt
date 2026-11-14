package com.example.truescale.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.truescale.ui.theme.TabularNumberStyle

/**
 * Reusable 3D Rotary Drum Picker (Selettore a Tamburo Cilindrico 3D).
 * By default (isCircular = false), preserves the exact sequential order of items:
 * Top = Previous item (empty when at index 0)
 * Center = Active item
 * Bottom = Next item (empty when at last index)
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun <T> RotaryDrumPicker(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    labelOf: (T) -> String,
    onHapticTick: () -> Unit,
    modifier: Modifier = Modifier,
    iconOf: ((T) -> ImageVector)? = null,
    width: Dp = 175.dp,
    height: Dp = 120.dp,
    isCircular: Boolean = false
) {
    val total = items.size
    if (total == 0) return

    val currentIndex = items.indexOf(selectedItem).let { if (it >= 0) it else 0 }
    val hasPrev = if (isCircular) true else currentIndex > 0
    val hasNext = if (isCircular) true else currentIndex < total - 1

    val prevItem = if (hasPrev) {
        val prevIdx = if (isCircular) (currentIndex - 1 + total) % total else currentIndex - 1
        items[prevIdx]
    } else null

    val nextItem = if (hasNext) {
        val nextIdx = if (isCircular) (currentIndex + 1) % total else currentIndex + 1
        items[nextIdx]
    } else null

    val colorScheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val swipeThresholdPx = remember(density) { with(density) { 28.dp.toPx() } }

    var dragAccumulator by remember { mutableFloatStateOf(0f) }
    var hasTriggeredInDrag by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .width(width)
            .height(height)
            .pointerInput(currentIndex, items, isCircular) {
                detectVerticalDragGestures(
                    onDragStart = {
                        dragAccumulator = 0f
                        hasTriggeredInDrag = false
                    },
                    onDragEnd = {
                        dragAccumulator = 0f
                        hasTriggeredInDrag = false
                    },
                    onDragCancel = {
                        dragAccumulator = 0f
                        hasTriggeredInDrag = false
                    },
                    onVerticalDrag = { change, dragAmount ->
                        dragAccumulator += dragAmount
                        if (!hasTriggeredInDrag) {
                            if (dragAccumulator > swipeThresholdPx && prevItem != null) {
                                // Dragged down -> navigate to previous item in sequential order
                                hasTriggeredInDrag = true
                                change.consume()
                                onHapticTick()
                                onItemSelected(prevItem)
                            } else if (dragAccumulator < -swipeThresholdPx && nextItem != null) {
                                // Dragged up -> navigate to next item in sequential order
                                hasTriggeredInDrag = true
                                change.consume()
                                onHapticTick()
                                onItemSelected(nextItem)
                            }
                        }
                    }
                )
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            colorScheme.outlineVariant.copy(alpha = 0.45f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            // Central Lens Optical Highlight
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colorScheme.primary.copy(alpha = 0.16f))
                    .border(1.dp, colorScheme.primary.copy(alpha = 0.65f), RoundedCornerShape(10.dp))
            )

            // 3-Row Vertical Cylinder Column
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Row: Previous Item (Tilted backwards)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .then(
                            if (prevItem != null) {
                                Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    onHapticTick()
                                    onItemSelected(prevItem)
                                }
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (prevItem != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    rotationX = 28f
                                    scaleX = 0.88f
                                    scaleY = 0.88f
                                    alpha = 0.55f
                                }
                        ) {
                            if (iconOf != null) {
                                Icon(
                                    imageVector = iconOf(prevItem),
                                    contentDescription = null,
                                    tint = colorScheme.onSurface,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                            }
                            Text(
                                text = labelOf(prevItem),
                                style = TabularNumberStyle.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = colorScheme.onSurface
                                ),
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Middle Row: Active Central Item
                AnimatedContent(
                    targetState = selectedItem,
                    transitionSpec = {
                        val toIndex = items.indexOf(targetState)
                        val fromIndex = items.indexOf(initialState)
                        if (toIndex > fromIndex) {
                            slideInVertically { h -> h } togetherWith
                                    slideOutVertically { h -> -h }
                        } else {
                            slideInVertically { h -> -h } togetherWith
                                    slideOutVertically { h -> h }
                        }
                    },
                    label = "DrumCenterItem"
                ) { targetItem ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        ) {
                            if (iconOf != null) {
                                Icon(
                                    imageVector = iconOf(targetItem),
                                    contentDescription = null,
                                    tint = colorScheme.primary,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = labelOf(targetItem),
                                style = TabularNumberStyle.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.primary
                                ),
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Bottom Row: Next Item (Tilted forwards)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .then(
                            if (nextItem != null) {
                                Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    onHapticTick()
                                    onItemSelected(nextItem)
                                }
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (nextItem != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    rotationX = -28f
                                    scaleX = 0.88f
                                    scaleY = 0.88f
                                    alpha = 0.55f
                                }
                        ) {
                            if (iconOf != null) {
                                Icon(
                                    imageVector = iconOf(nextItem),
                                    contentDescription = null,
                                    tint = colorScheme.onSurface,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                            }
                            Text(
                                text = labelOf(nextItem),
                                style = TabularNumberStyle.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = colorScheme.onSurface
                                ),
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Up micro chevron (clickable only if previous item exists)
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Su",
                tint = if (prevItem != null) colorScheme.primary.copy(alpha = 0.70f) else colorScheme.onSurface.copy(alpha = 0.15f),
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.TopEnd)
                    .padding(end = 2.dp, top = 2.dp)
                    .then(
                        if (prevItem != null) {
                            Modifier.clickable {
                                onHapticTick()
                                onItemSelected(prevItem)
                            }
                        } else Modifier
                    )
            )

            // Down micro chevron (clickable only if next item exists)
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Giu",
                tint = if (nextItem != null) colorScheme.primary.copy(alpha = 0.70f) else colorScheme.onSurface.copy(alpha = 0.15f),
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.BottomEnd)
                    .padding(end = 2.dp, bottom = 2.dp)
                    .then(
                        if (nextItem != null) {
                            Modifier.clickable {
                                onHapticTick()
                                onItemSelected(nextItem)
                            }
                        } else Modifier
                    )
            )
        }
    }
}
