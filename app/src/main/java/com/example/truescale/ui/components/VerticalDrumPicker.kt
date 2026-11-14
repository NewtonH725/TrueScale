package com.example.truescale.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.truescale.ui.theme.LaserCyan
import com.example.truescale.ui.theme.MetrologyDarkSurface
import com.example.truescale.ui.theme.MetrologyDarkSurfaceVariant
import com.example.truescale.ui.theme.MetrologyOutline
import com.example.truescale.ui.theme.TabularNumberStyle
import com.example.truescale.ui.theme.TextPrimary
import com.example.truescale.ui.theme.TextSecondary
import com.example.truescale.ui.theme.TextTertiary

/**
 * Vertical Drum Picker (Selettore a Tamburo Rotante).
 * Displays tools on a vertical rotating cylinder with 3D perspective and snap selection.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun VerticalDrumPicker(
    currentMode: RulerMode,
    onModeSelected: (RulerMode) -> Unit,
    onHapticTick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = remember { RulerMode.entries }
    val total = modes.size
    val currentIndex = currentMode.ordinal

    // Previous and Next wrapped items on the drum
    val prevIndex = (currentIndex - 1 + total) % total
    val nextIndex = (currentIndex + 1) % total

    val prevMode = modes[prevIndex]
    val nextMode = modes[nextIndex]

    // Drag accumulation to trigger rotation on drag threshold
    var dragAccumulator by remember { mutableFloatStateOf(0f) }
    val dragThreshold = 36f

    Card(
        modifier = modifier
            .width(115.dp)
            .height(115.dp)
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    dragAccumulator += delta
                    if (dragAccumulator > dragThreshold) {
                        // Dragged downwards -> rotate drum down (select previous)
                        dragAccumulator = 0f
                        onHapticTick()
                        onModeSelected(prevMode)
                    } else if (dragAccumulator < -dragThreshold) {
                        // Dragged upwards -> rotate drum up (select next)
                        dragAccumulator = 0f
                        onHapticTick()
                        onModeSelected(nextMode)
                    }
                }
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MetrologyDarkSurface.copy(alpha = 0.94f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MetrologyOutline)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            // Central Lens / Drum Viewport Frame
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(LaserCyan.copy(alpha = 0.12f))
                    .border(1.dp, LaserCyan.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            )

            // Drum Items (Top: Prev, Center: Active, Bottom: Next)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Item (Tilted back in 3D perspective)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                        .graphicsLayer {
                            rotationX = 35f
                            scaleX = 0.85f
                            scaleY = 0.85f
                            alpha = 0.45f
                        }
                        .clickable {
                            onHapticTick()
                            onModeSelected(prevMode)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = prevMode.title,
                        style = TabularNumberStyle.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                            color = TextTertiary
                        ),
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }

                // Center Active Item
                AnimatedContent(
                    targetState = currentMode,
                    transitionSpec = {
                        if (targetState.ordinal > initialState.ordinal) {
                            slideInVertically { height -> height } togetherWith
                                    slideOutVertically { height -> -height }
                        } else {
                            slideInVertically { height -> -height } togetherWith
                                    slideOutVertically { height -> height }
                        }
                    },
                    label = "DrumCenterItem"
                ) { targetMode ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = targetMode.title,
                            style = TabularNumberStyle.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = LaserCyan
                            ),
                            maxLines = 1,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Bottom Item (Tilted forward in 3D perspective)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                        .graphicsLayer {
                            rotationX = -35f
                            scaleX = 0.85f
                            scaleY = 0.85f
                            alpha = 0.45f
                        }
                        .clickable {
                            onHapticTick()
                            onModeSelected(nextMode)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = nextMode.title,
                        style = TabularNumberStyle.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                            color = TextTertiary
                        ),
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Up / Down micro indicators
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Su",
                tint = TextTertiary,
                modifier = Modifier
                    .size(12.dp)
                    .align(Alignment.TopEnd)
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Giu",
                tint = TextTertiary,
                modifier = Modifier
                    .size(12.dp)
                    .align(Alignment.BottomEnd)
            )
        }
    }
}
