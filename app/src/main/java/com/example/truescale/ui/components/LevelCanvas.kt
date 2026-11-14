package com.example.truescale.ui.components

import android.graphics.Paint as AndroidPaint
import android.graphics.Typeface
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.material3.MaterialTheme
import com.example.truescale.engine.DisplayMetrologyEngine
import com.example.truescale.engine.InertialSensorData
import com.example.truescale.engine.LevelOrientationMode
import com.example.truescale.ui.theme.PrecisionGreen
import com.example.truescale.ui.theme.PrecisionGreenGlow
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LevelCanvas(
    sensorData: InertialSensorData,
    engine: DisplayMetrologyEngine,
    modifier: Modifier = Modifier,
    forcedMode: LevelOrientationMode? = null
) {
    val activeMode = forcedMode ?: sensorData.activeMode

    val colorScheme = MaterialTheme.colorScheme
    val outlineColor = colorScheme.outline
    val outlineVariantColor = colorScheme.outlineVariant
    val onSurfaceVariantColor = colorScheme.onSurfaceVariant
    val surfaceColor = colorScheme.surface
    val primaryColor = colorScheme.primary
    val secondaryColor = colorScheme.secondary

    // Expressive physics animation: Spring damping without jitter
    val animatedRoll by animateFloatAsState(
        targetValue = sensorData.rollDeg,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "animatedRoll"
    )

    val animatedPitch by animateFloatAsState(
        targetValue = sensorData.pitchDeg,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "animatedPitch"
    )

    val animatedVerticalAngle by animateFloatAsState(
        targetValue = sensorData.verticalAngleDeg,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "animatedVerticalAngle"
    )

    // Pre-allocated graphic objects (Zero GC churn in Canvas loop)
    val hairlineStroke = remember { Stroke(width = 1.5f) }
    val reticleStroke = remember { Stroke(width = 2.0f) }
    val pointerStroke = remember { Stroke(width = 4.0f) }
    val wedgeStroke = remember { Stroke(width = 28.0f) }

    val scaleTextPaint = remember(onSurfaceVariantColor) {
        AndroidPaint().apply {
            color = onSurfaceVariantColor.toArgb()
            textSize = 28f
            isAntiAlias = true
            typeface = Typeface.MONOSPACE
            textAlign = AndroidPaint.Align.CENTER
        }
    }

    val graticulePaint = remember(onSurfaceVariantColor) {
        AndroidPaint().apply {
            color = onSurfaceVariantColor.toArgb()
            textSize = 28f
            isAntiAlias = true
            typeface = Typeface.MONOSPACE
            textAlign = AndroidPaint.Align.LEFT
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)

            when (activeMode) {
                LevelOrientationMode.BUBBLE_2D -> {
                    draw2DBubble(
                        center = center,
                        rollDeg = animatedRoll,
                        pitchDeg = animatedPitch,
                        isAligned = sensorData.isLevelAligned,
                        reticleStroke = reticleStroke,
                        hairlineStroke = hairlineStroke,
                        outlineColor = outlineColor,
                        outlineVariantColor = outlineVariantColor,
                        surfaceColor = surfaceColor,
                        primaryColor = primaryColor,
                        secondaryColor = secondaryColor
                    )
                }

                LevelOrientationMode.VERTICAL_CLINOMETER -> {
                    drawVerticalClinometer(
                        center = center,
                        angleDeg = animatedVerticalAngle,
                        isAligned = sensorData.isLevelAligned,
                        scalePaint = scaleTextPaint,
                        hairlineStroke = hairlineStroke,
                        pointerStroke = pointerStroke,
                        wedgeStroke = wedgeStroke,
                        outlineColor = outlineColor,
                        outlineVariantColor = outlineVariantColor,
                        surfaceColor = surfaceColor,
                        primaryColor = primaryColor
                    )
                }

                LevelOrientationMode.HYBRID_RULER_LEVEL -> {
                    // Left edge graduated scale
                    drawHybridScale(
                        engine = engine,
                        tickStrokeWidth = 1.5f,
                        graticulePaint = graticulePaint,
                        majorTickColor = outlineColor,
                        minorTickColor = outlineVariantColor,
                        subTickColor = outlineVariantColor.copy(alpha = 0.5f)
                    )

                    // Shifted center clinometer
                    val hybridCenter = Offset(size.width * 0.58f, size.height / 2f)
                    drawVerticalClinometer(
                        center = hybridCenter,
                        angleDeg = animatedVerticalAngle,
                        isAligned = sensorData.isLevelAligned,
                        scalePaint = scaleTextPaint,
                        hairlineStroke = hairlineStroke,
                        pointerStroke = pointerStroke,
                        wedgeStroke = wedgeStroke,
                        outlineColor = outlineColor,
                        outlineVariantColor = outlineVariantColor,
                        surfaceColor = surfaceColor,
                        primaryColor = primaryColor
                    )
                }
            }
        }
    }
}

/**
 * Renders 2D Bullseye Spirit Level.
 * Zero allocations inside draw loop.
 */
private fun DrawScope.draw2DBubble(
    center: Offset,
    rollDeg: Float,
    pitchDeg: Float,
    isAligned: Boolean,
    reticleStroke: Stroke,
    hairlineStroke: Stroke,
    outlineColor: Color,
    outlineVariantColor: Color,
    surfaceColor: Color,
    primaryColor: Color,
    secondaryColor: Color
) {
    val outerReticleRadius = size.width * 0.42f
    val targetRadius = outerReticleRadius * 0.22f
    val bubbleRadius = outerReticleRadius * 0.18f

    // Outer boundary ring
    drawCircle(
        color = outlineColor,
        radius = outerReticleRadius,
        center = center,
        style = reticleStroke
    )

    // Intermediate 1.0 deg and 5.0 deg concentric rings
    drawCircle(
        color = outlineVariantColor.copy(alpha = 0.5f),
        radius = outerReticleRadius * 0.65f,
        center = center,
        style = hairlineStroke
    )
    drawCircle(
        color = if (isAligned) PrecisionGreen else outlineVariantColor,
        radius = targetRadius,
        center = center,
        style = hairlineStroke
    )

    // Crosshairs
    drawLine(
        color = outlineVariantColor,
        start = Offset(center.x - outerReticleRadius, center.y),
        end = Offset(center.x + outerReticleRadius, center.y),
        strokeWidth = hairlineStroke.width
    )
    drawLine(
        color = outlineVariantColor,
        start = Offset(center.x, center.y - outerReticleRadius),
        end = Offset(center.x, center.y + outerReticleRadius),
        strokeWidth = hairlineStroke.width
    )

    // Bubble position mapping: 10 degrees tilt maps to full radius
    val maxDeg = 8.0f
    val normX = (rollDeg / maxDeg).coerceIn(-1.0f, 1.0f)
    val normY = (pitchDeg / maxDeg).coerceIn(-1.0f, 1.0f)

    val maxTravel = outerReticleRadius - bubbleRadius
    val bubblePos = Offset(
        x = center.x + normX * maxTravel,
        y = center.y + normY * maxTravel
    )

    // Bubble glow when perfectly aligned
    if (isAligned) {
        drawCircle(
            color = PrecisionGreenGlow,
            radius = bubbleRadius * 1.6f,
            center = bubblePos
        )
    }

    // Active bubble body
    val bubbleColor = if (isAligned) PrecisionGreen else primaryColor
    drawCircle(
        color = bubbleColor,
        radius = bubbleRadius,
        center = bubblePos
    )

    // Bubble center highlight
    drawCircle(
        color = surfaceColor,
        radius = 4f,
        center = bubblePos
    )
}

/**
 * Renders Vertical Clinometer with dynamic Expressive wedge and scale -10°..0..+10°.
 * Zero allocations inside draw loop.
 */
private fun DrawScope.drawVerticalClinometer(
    center: Offset,
    angleDeg: Float,
    isAligned: Boolean,
    scalePaint: AndroidPaint,
    hairlineStroke: Stroke,
    pointerStroke: Stroke,
    wedgeStroke: Stroke,
    outlineColor: Color,
    outlineVariantColor: Color,
    surfaceColor: Color,
    primaryColor: Color
) {
    val dialRadius = size.width * 0.40f

    // Arc base scale: -10° to +10° mapped across 120 degrees of arc at bottom
    val arcSweep = 100f
    val startAngle = 90f - (arcSweep / 2f)

    // Dial background arc
    drawArc(
        color = outlineColor,
        startAngle = startAngle,
        sweepAngle = arcSweep,
        useCenter = false,
        topLeft = Offset(center.x - dialRadius, center.y - dialRadius),
        size = Size(dialRadius * 2f, dialRadius * 2f),
        style = hairlineStroke
    )

    // Dynamic wedge from 0 deg (plumb line down: 90 deg) to current angle
    val currentArcOffset = (angleDeg / 10.0f).coerceIn(-1.0f, 1.0f) * (arcSweep / 2f)
    val wedgeColor = if (isAligned) PrecisionGreen else primaryColor

    if (kotlin.math.abs(currentArcOffset) > 0.5f) {
        val sweep = currentArcOffset
        val wedgeStart = 90f
        drawArc(
            color = if (isAligned) PrecisionGreenGlow else primaryColor.copy(alpha = 0.25f),
            startAngle = if (sweep >= 0f) wedgeStart else wedgeStart + sweep,
            sweepAngle = kotlin.math.abs(sweep),
            useCenter = false,
            topLeft = Offset(center.x - dialRadius + 14f, center.y - dialRadius + 14f),
            size = Size((dialRadius - 14f) * 2f, (dialRadius - 14f) * 2f),
            style = wedgeStroke
        )
    }

    // Dial tick marks (-10°, -5°, 0°, +5°, +10°)
    for (deg in -10..10) {
        val frac = deg / 10.0f
        val tickAngleDeg = 90f + frac * (arcSweep / 2f)
        val tickAngleRad = tickAngleDeg * PI.toFloat() / 180f

        val isMajor = (deg % 5 == 0)
        val tickLen = if (isMajor) 22f else 10f
        val tickColor = when {
            deg == 0 -> PrecisionGreen
            isMajor -> outlineColor
            else -> outlineVariantColor.copy(alpha = 0.6f)
        }

        val startX = center.x + (dialRadius - tickLen) * cos(tickAngleRad)
        val startY = center.y + (dialRadius - tickLen) * sin(tickAngleRad)
        val endX = center.x + dialRadius * cos(tickAngleRad)
        val endY = center.y + dialRadius * sin(tickAngleRad)

        drawLine(
            color = tickColor,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = hairlineStroke.width
        )

        // Labels for major ticks
        if (isMajor) {
            val labelRadius = dialRadius + 28f
            val labelX = center.x + labelRadius * cos(tickAngleRad)
            val labelY = center.y + labelRadius * sin(tickAngleRad) + 8f
            val labelText = if (deg == 0) "0°" else "${if (deg > 0) "+" else ""}$deg°"
            drawContext.canvas.nativeCanvas.drawText(
                labelText,
                labelX,
                labelY,
                scalePaint
            )
        }
    }

    // Active plumb needle
    val needleAngleRad = (90f + currentArcOffset) * PI.toFloat() / 180f
    val needleLen = dialRadius - 8f
    val needleEndX = center.x + needleLen * cos(needleAngleRad)
    val needleEndY = center.y + needleLen * sin(needleAngleRad)

    drawLine(
        color = wedgeColor,
        start = center,
        end = Offset(needleEndX, needleEndY),
        strokeWidth = pointerStroke.width
    )

    // Pivot center
    drawCircle(
        color = if (isAligned) PrecisionGreen else primaryColor,
        radius = 16f,
        center = center
    )
    drawCircle(
        color = surfaceColor,
        radius = 8f,
        center = center
    )
}

/**
 * Graduated scale for Hybrid Mode.
 */
private fun DrawScope.drawHybridScale(
    engine: DisplayMetrologyEngine,
    tickStrokeWidth: Float,
    graticulePaint: AndroidPaint,
    majorTickColor: Color,
    minorTickColor: Color,
    subTickColor: Color
) {
    val totalHeightPx = size.height
    val pxPerMmY = engine.pxPerMmY
    if (pxPerMmY <= 0f) return

    val totalMm = (totalHeightPx / pxPerMmY).toInt()
    val tick1mmPx = engine.mmToPxX(3.0f)
    val tick5mmPx = engine.mmToPxX(5.0f)
    val tick10mmPx = engine.mmToPxX(8.0f)

    for (mm in 0..totalMm) {
        val y = mm * pxPerMmY
        val isCm = (mm % 10 == 0)
        val isHalfCm = (mm % 5 == 0 && !isCm)

        val tickLen = when {
            isCm -> tick10mmPx
            isHalfCm -> tick5mmPx
            else -> tick1mmPx
        }

        val tickColor = when {
            isCm -> majorTickColor
            isHalfCm -> minorTickColor
            else -> subTickColor
        }

        drawLine(
            color = tickColor,
            start = Offset(0f, y),
            end = Offset(tickLen, y),
            strokeWidth = tickStrokeWidth
        )

        if (isCm && mm > 0) {
            drawContext.canvas.nativeCanvas.drawText(
                (mm / 10).toString(),
                tick10mmPx + 8f,
                y + 8f,
                graticulePaint
            )
        }
    }
}
