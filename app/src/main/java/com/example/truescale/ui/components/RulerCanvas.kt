package com.example.truescale.ui.components

import android.graphics.Paint as AndroidPaint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.truescale.engine.DisplayMetrologyEngine
import com.example.truescale.ui.theme.BubbleCrosshair
import com.example.truescale.ui.theme.GraticuleMajor
import com.example.truescale.ui.theme.GraticuleMinor
import com.example.truescale.ui.theme.GraticuleSub
import com.example.truescale.ui.theme.LaserCyan
import com.example.truescale.ui.theme.LaserOrange
import com.example.truescale.ui.theme.MetrologyDarkBackground
import com.example.truescale.ui.theme.PrecisionGreen
import com.example.truescale.ui.theme.TextPrimary
import com.example.truescale.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 5 In 1 Metrological Ruler Modes.
 */
enum class RulerMode(val title: String) {
    SINGLE_CURSOR("Righello"),
    DUAL_CALIPER("Calibro"),
    RECTANGLE_2D("Area 2D"),
    CIRCLE_RING("Anelli & Ø"),
    PROTRACTOR_180("Goniometro")
}

data class RulerMeasurementState(
    val mainReadout: String = "",
    val subReadout: String = "",
    val tertiaryReadout: String = ""
)

@Composable
fun RulerCanvas(
    engine: DisplayMetrologyEngine,
    mode: RulerMode,
    modifier: Modifier = Modifier,
    isMagnetEnabled: Boolean = true,
    onMmCrossed: (() -> Unit)? = null,
    onMeasurementChanged: (RulerMeasurementState) -> Unit = {}
) {
    val colorScheme = androidx.compose.material3.MaterialTheme.colorScheme
    val primaryColor = colorScheme.primary
    val secondaryColor = colorScheme.secondary
    val surfaceColor = colorScheme.surface
    val onSurfaceColor = colorScheme.onSurface
    val onSurfaceVariantColor = colorScheme.onSurfaceVariant
    val outlineVariantColor = colorScheme.outlineVariant

    // Shared pre-allocated drawing styles (Zero allocations at 120Hz inside Canvas)
    val tickStrokeWidth = 1.5f
    val hairlineStroke = remember { Stroke(width = 1.5f) }
    val outlineStroke = remember { Stroke(width = 2.0f) }
    val cursorStroke = remember { Stroke(width = 3.0f) }
    val ringGuideStroke = remember { Stroke(width = 1.0f) }
    val protractorArcStroke = remember { Stroke(width = 2.5f) }

    // Pre-allocated native Android text paints for zero GC churn in DrawScope
    val graticuleTextPaint = remember(onSurfaceVariantColor) {
        AndroidPaint().apply {
            color = onSurfaceVariantColor.toArgb()
            textSize = 28f
            isAntiAlias = true
            typeface = Typeface.MONOSPACE
            textAlign = AndroidPaint.Align.LEFT
        }
    }

    val readoutTextPaint = remember {
        AndroidPaint().apply {
            color = LaserCyan.toArgb()
            textSize = 36f
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
            textAlign = AndroidPaint.Align.LEFT
        }
    }

    // State for Single Cursor
    var cursor1Y by remember { mutableFloatStateOf(engine.mmToPxY(80f)) }

    // State for Dual Caliper
    var caliperTopY by remember { mutableFloatStateOf(engine.mmToPxY(40f)) }
    var caliperBottomY by remember { mutableFloatStateOf(engine.mmToPxY(120f)) }

    // State for Rectangle 2D
    var rectLeft by remember { mutableFloatStateOf(engine.mmToPxX(15f)) }
    var rectTop by remember { mutableFloatStateOf(engine.mmToPxY(50f)) }
    var rectRight by remember { mutableFloatStateOf(engine.mmToPxX(65f)) }
    var rectBottom by remember { mutableFloatStateOf(engine.mmToPxY(130f)) }

    // State for Circle Ring
    var circleCenterX by remember { mutableFloatStateOf(engine.mmToPxX(35f)) }
    var circleCenterY by remember { mutableFloatStateOf(engine.mmToPxY(90f)) }
    var circleRadiusPx by remember { mutableFloatStateOf(engine.mmToPxY(10f)) } // Default ~20mm diameter (EU 63)

    // State for Protractor 180
    var vertexX by remember { mutableFloatStateOf(engine.mmToPxX(35f)) }
    var vertexY by remember { mutableFloatStateOf(engine.mmToPxY(100f)) }
    var arm1X by remember { mutableFloatStateOf(engine.mmToPxX(65f)) }
    var arm1Y by remember { mutableFloatStateOf(engine.mmToPxY(100f)) }
    var arm2X by remember { mutableFloatStateOf(engine.mmToPxX(50f)) }
    var arm2Y by remember { mutableFloatStateOf(engine.mmToPxY(70f)) }

    // Protractor Magnetic Snap to common angles (0°, 30°, 45°, 60°, 90°, 120°, 135°, 150°, 180°)
    var isProtractorSnapped by remember { mutableStateOf(false) }
    var lastSnappedAngle by remember { mutableFloatStateOf(-1f) }
    val commonProtractorAngles = remember { listOf(0f, 30f, 45f, 60f, 90f, 120f, 135f, 150f, 180f) }
    val snapToleranceDeg = 2.2f

    val currentMagnetEnabled by rememberUpdatedState(isMagnetEnabled)

    // Synchronize measurement readout on mode switch or magnet toggle
    LaunchedEffect(mode, isMagnetEnabled) {
        when (mode) {
            RulerMode.SINGLE_CURSOR -> {
                val mm = engine.pxToMmY(cursor1Y)
                val cm = mm / 10f
                onMeasurementChanged(
                    RulerMeasurementState(
                        mainReadout = String.format(Locale.US, "%.2f cm", cm),
                        subReadout = String.format(Locale.US, "%.1f mm", mm),
                        tertiaryReadout = "Punto Zero: 0.00 cm"
                    )
                )
            }
            RulerMode.DUAL_CALIPER -> {
                val deltaPx = abs(caliperBottomY - caliperTopY)
                val mm = engine.pxToMmY(deltaPx)
                val cm = mm / 10f
                onMeasurementChanged(
                    RulerMeasurementState(
                        mainReadout = String.format(Locale.US, "%.2f cm", cm),
                        subReadout = String.format(Locale.US, "Δ %.1f mm", mm),
                        tertiaryReadout = String.format(Locale.US, "Y1: %.1f mm | Y2: %.1f mm", engine.pxToMmY(caliperTopY), engine.pxToMmY(caliperBottomY))
                    )
                )
            }
            RulerMode.RECTANGLE_2D -> {
                val widthPx = abs(rectRight - rectLeft)
                val heightPx = abs(rectBottom - rectTop)
                val baseCm = engine.pxToCmX(widthPx)
                val heightCm = engine.pxToCmY(heightPx)
                val areaCm2 = baseCm * heightCm
                onMeasurementChanged(
                    RulerMeasurementState(
                        mainReadout = String.format(Locale.US, "%.2f cm²", areaCm2),
                        subReadout = String.format(Locale.US, "B: %.2f cm × H: %.2f cm", baseCm, heightCm),
                        tertiaryReadout = String.format(Locale.US, "%.1f mm × %.1f mm", engine.pxToMmX(widthPx), engine.pxToMmY(heightPx))
                    )
                )
            }
            RulerMode.CIRCLE_RING -> {
                val diameterMm = engine.pxToMmY(circleRadiusPx * 2f)
                val circMm = diameterMm * PI.toFloat()
                val euSize = engine.getEuRingSize(diameterMm)
                onMeasurementChanged(
                    RulerMeasurementState(
                        mainReadout = String.format(Locale.US, "Ø %.2f cm", diameterMm / 10f),
                        subReadout = String.format(Locale.US, "Taglia EU: %d (ISO 8653)", euSize),
                        tertiaryReadout = String.format(Locale.US, "Circonferenza: %.1f mm", circMm)
                    )
                )
            }
            RulerMode.PROTRACTOR_180 -> {
                val angleDeg = engine.calculateProtractorAngle(vertexX, vertexY, arm1X, arm1Y, arm2X, arm2Y)
                if (!isMagnetEnabled) {
                    isProtractorSnapped = false
                    lastSnappedAngle = -1f
                    onMeasurementChanged(
                        RulerMeasurementState(
                            mainReadout = String.format(Locale.US, "%.1f°", angleDeg),
                            subReadout = String.format(Locale.US, "Radianti: %.3f rad", angleDeg * PI.toFloat() / 180f),
                            tertiaryReadout = "Goniometro Libero 0°..180°"
                        )
                    )
                } else {
                    val matched = commonProtractorAngles.firstOrNull { abs(angleDeg - it) <= snapToleranceDeg }
                    if (matched != null) {
                        isProtractorSnapped = true
                        lastSnappedAngle = matched
                        onMeasurementChanged(
                            RulerMeasurementState(
                                mainReadout = String.format(Locale.US, "%.1f°", matched),
                                subReadout = "★ CALAMITA AGGANCIATA (%.0f°)".format(matched),
                                tertiaryReadout = "Angolo cardine: %.0f°".format(matched)
                            )
                        )
                    } else {
                        isProtractorSnapped = false
                        lastSnappedAngle = -1f
                        onMeasurementChanged(
                            RulerMeasurementState(
                                mainReadout = String.format(Locale.US, "%.1f°", angleDeg),
                                subReadout = String.format(Locale.US, "Radianti: %.3f rad", angleDeg * PI.toFloat() / 180f),
                                tertiaryReadout = "Calamita Attiva (0°, 30°, 45°, 60°, 90°, 120°, 135°, 150°, 180°)"
                            )
                        )
                    }
                }
            }
        }
    }

    // Haptic tick tracking
    var lastHapticMm by remember { mutableIntStateOf(-1) }

    fun checkHapticTick(currentMm: Float) {
        val mmInt = currentMm.roundToInt()
        if (mmInt != lastHapticMm) {
            lastHapticMm = mmInt
            onMmCrossed?.invoke()
        }
    }

    // Touch gesture selection tracking
    var activeHandle by remember { mutableIntStateOf(0) } // 0: None, 1: First, 2: Second, 3: Center/Third

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(mode, isMagnetEnabled) {
                detectDragGestures(
                    onDragStart = { offset ->
                        when (mode) {
                            RulerMode.SINGLE_CURSOR -> {
                                activeHandle = 1
                            }
                            RulerMode.DUAL_CALIPER -> {
                                val d1 = abs(offset.y - caliperTopY)
                                val d2 = abs(offset.y - caliperBottomY)
                                activeHandle = if (d1 < d2) 1 else 2
                            }
                            RulerMode.RECTANGLE_2D -> {
                                val dLeft = abs(offset.x - rectLeft)
                                val dRight = abs(offset.x - rectRight)
                                val dTop = abs(offset.y - rectTop)
                                val dBottom = abs(offset.y - rectBottom)
                                activeHandle = when {
                                    dRight < 60f && dBottom < 60f -> 1 // Bottom right corner
                                    dLeft < 60f && dTop < 60f -> 2 // Top left corner
                                    else -> 3 // Move entire rectangle
                                }
                            }
                            RulerMode.CIRCLE_RING -> {
                                val distToCenter = kotlin.math.hypot(offset.x - circleCenterX, offset.y - circleCenterY)
                                activeHandle = if (abs(distToCenter - circleRadiusPx) < 80f) 1 else 2 // 1: radius, 2: center
                            }
                            RulerMode.PROTRACTOR_180 -> {
                                val dV = kotlin.math.hypot(offset.x - vertexX, offset.y - vertexY)
                                val dA1 = kotlin.math.hypot(offset.x - arm1X, offset.y - arm1Y)
                                val dA2 = kotlin.math.hypot(offset.x - arm2X, offset.y - arm2Y)
                                activeHandle = when {
                                    dA1 < 90f -> 1 // Arm 1
                                    dA2 < 90f -> 2 // Arm 2
                                    dV < 90f -> 3  // Vertex
                                    dA1 < dA2 -> 1
                                    else -> 2
                                }
                            }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        when (mode) {
                            RulerMode.SINGLE_CURSOR -> {
                                cursor1Y = (cursor1Y + dragAmount.y).coerceAtLeast(0f)
                                val mm = engine.pxToMmY(cursor1Y)
                                checkHapticTick(mm)
                                val cm = mm / 10f
                                onMeasurementChanged(
                                    RulerMeasurementState(
                                        mainReadout = String.format(Locale.US, "%.2f cm", cm),
                                        subReadout = String.format(Locale.US, "%.1f mm", mm),
                                        tertiaryReadout = "Punto Zero: 0.00 cm"
                                    )
                                )
                            }
                            RulerMode.DUAL_CALIPER -> {
                                if (activeHandle == 1) {
                                    caliperTopY = (caliperTopY + dragAmount.y).coerceAtLeast(0f)
                                } else {
                                    caliperBottomY = (caliperBottomY + dragAmount.y).coerceAtLeast(0f)
                                }
                                val deltaPx = abs(caliperBottomY - caliperTopY)
                                val mm = engine.pxToMmY(deltaPx)
                                checkHapticTick(mm)
                                val cm = mm / 10f
                                onMeasurementChanged(
                                    RulerMeasurementState(
                                        mainReadout = String.format(Locale.US, "%.2f cm", cm),
                                        subReadout = String.format(Locale.US, "Δ %.1f mm", mm),
                                        tertiaryReadout = String.format(Locale.US, "Y1: %.1f mm | Y2: %.1f mm", engine.pxToMmY(caliperTopY), engine.pxToMmY(caliperBottomY))
                                    )
                                )
                            }
                            RulerMode.RECTANGLE_2D -> {
                                if (activeHandle == 1) {
                                    rectRight = (rectRight + dragAmount.x).coerceAtLeast(rectLeft + 20f)
                                    rectBottom = (rectBottom + dragAmount.y).coerceAtLeast(rectTop + 20f)
                                } else if (activeHandle == 2) {
                                    rectLeft = (rectLeft + dragAmount.x).coerceAtMost(rectRight - 20f).coerceAtLeast(0f)
                                    rectTop = (rectTop + dragAmount.y).coerceAtMost(rectBottom - 20f).coerceAtLeast(0f)
                                } else {
                                    val w = rectRight - rectLeft
                                    val h = rectBottom - rectTop
                                    rectLeft = (rectLeft + dragAmount.x).coerceAtLeast(0f)
                                    rectRight = rectLeft + w
                                    rectTop = (rectTop + dragAmount.y).coerceAtLeast(0f)
                                    rectBottom = rectTop + h
                                }
                                val widthPx = abs(rectRight - rectLeft)
                                val heightPx = abs(rectBottom - rectTop)
                                val baseCm = engine.pxToCmX(widthPx)
                                val heightCm = engine.pxToCmY(heightPx)
                                val areaCm2 = baseCm * heightCm
                                checkHapticTick(engine.pxToMmX(widthPx))
                                onMeasurementChanged(
                                    RulerMeasurementState(
                                        mainReadout = String.format(Locale.US, "%.2f cm²", areaCm2),
                                        subReadout = String.format(Locale.US, "B: %.2f cm × H: %.2f cm", baseCm, heightCm),
                                        tertiaryReadout = String.format(Locale.US, "%.1f mm × %.1f mm", engine.pxToMmX(widthPx), engine.pxToMmY(heightPx))
                                    )
                                )
                            }
                            RulerMode.CIRCLE_RING -> {
                                if (activeHandle == 1) {
                                    circleRadiusPx = (circleRadiusPx + dragAmount.y).coerceIn(engine.mmToPxY(3f), engine.mmToPxY(45f))
                                } else {
                                    circleCenterX = (circleCenterX + dragAmount.x).coerceAtLeast(0f)
                                    circleCenterY = (circleCenterY + dragAmount.y).coerceAtLeast(0f)
                                }
                                val diameterMm = engine.pxToMmY(circleRadiusPx * 2f)
                                val circMm = diameterMm * PI.toFloat()
                                val euSize = engine.getEuRingSize(diameterMm)
                                checkHapticTick(diameterMm)
                                onMeasurementChanged(
                                    RulerMeasurementState(
                                        mainReadout = String.format(Locale.US, "Ø %.2f cm", diameterMm / 10f),
                                        subReadout = String.format(Locale.US, "Taglia EU: %d (ISO 8653)", euSize),
                                        tertiaryReadout = String.format(Locale.US, "Circonferenza: %.1f mm", circMm)
                                    )
                                )
                            }
                            RulerMode.PROTRACTOR_180 -> {
                                val currentMagnet = currentMagnetEnabled
                                when (activeHandle) {
                                    1 -> {
                                        arm1X += dragAmount.x
                                        arm1Y += dragAmount.y

                                        if (currentMagnet) {
                                            val len1 = kotlin.math.hypot(arm1X - vertexX, arm1Y - vertexY).coerceAtLeast(40f)
                                            val theta2 = kotlin.math.atan2(arm2Y - vertexY, arm2X - vertexX)
                                            val theta1 = kotlin.math.atan2(arm1Y - vertexY, arm1X - vertexX)
                                            var diffRad = theta1 - theta2
                                            while (diffRad > PI.toFloat()) diffRad -= 2f * PI.toFloat()
                                            while (diffRad < -PI.toFloat()) diffRad += 2f * PI.toFloat()
                                            val currentAngleDeg = abs(diffRad) * 180f / PI.toFloat()
                                            val sign = if (diffRad >= 0f) 1f else -1f

                                            val matched = commonProtractorAngles.firstOrNull { abs(currentAngleDeg - it) <= snapToleranceDeg }
                                            if (matched != null) {
                                                val snappedTheta1 = theta2 + (sign * matched * PI.toFloat() / 180f)
                                                arm1X = vertexX + len1 * kotlin.math.cos(snappedTheta1)
                                                arm1Y = vertexY + len1 * kotlin.math.sin(snappedTheta1)
                                                if (lastSnappedAngle != matched) {
                                                    lastSnappedAngle = matched
                                                    onMmCrossed?.invoke()
                                                }
                                                isProtractorSnapped = true
                                            } else {
                                                lastSnappedAngle = -1f
                                                isProtractorSnapped = false
                                            }
                                        } else {
                                            lastSnappedAngle = -1f
                                            isProtractorSnapped = false
                                        }
                                    }
                                    2 -> {
                                        arm2X += dragAmount.x
                                        arm2Y += dragAmount.y

                                        if (currentMagnet) {
                                            val len2 = kotlin.math.hypot(arm2X - vertexX, arm2Y - vertexY).coerceAtLeast(40f)
                                            val theta1 = kotlin.math.atan2(arm1Y - vertexY, arm1X - vertexX)
                                            val theta2 = kotlin.math.atan2(arm2Y - vertexY, arm2X - vertexX)
                                            var diffRad = theta2 - theta1
                                            while (diffRad > PI.toFloat()) diffRad -= 2f * PI.toFloat()
                                            while (diffRad < -PI.toFloat()) diffRad += 2f * PI.toFloat()
                                            val currentAngleDeg = abs(diffRad) * 180f / PI.toFloat()
                                            val sign = if (diffRad >= 0f) 1f else -1f

                                            val matched = commonProtractorAngles.firstOrNull { abs(currentAngleDeg - it) <= snapToleranceDeg }
                                            if (matched != null) {
                                                val snappedTheta2 = theta1 + (sign * matched * PI.toFloat() / 180f)
                                                arm2X = vertexX + len2 * kotlin.math.cos(snappedTheta2)
                                                arm2Y = vertexY + len2 * kotlin.math.sin(snappedTheta2)
                                                if (lastSnappedAngle != matched) {
                                                    lastSnappedAngle = matched
                                                    onMmCrossed?.invoke()
                                                }
                                                isProtractorSnapped = true
                                            } else {
                                                lastSnappedAngle = -1f
                                                isProtractorSnapped = false
                                            }
                                        } else {
                                            lastSnappedAngle = -1f
                                            isProtractorSnapped = false
                                        }
                                    }
                                    3 -> {
                                        val dX1 = arm1X - vertexX
                                        val dY1 = arm1Y - vertexY
                                        val dX2 = arm2X - vertexX
                                        val dY2 = arm2Y - vertexY
                                        vertexX += dragAmount.x
                                        vertexY += dragAmount.y
                                        arm1X = vertexX + dX1
                                        arm1Y = vertexY + dY1
                                        arm2X = vertexX + dX2
                                        arm2Y = vertexY + dY2
                                    }
                                }
                                val angleDeg = engine.calculateProtractorAngle(vertexX, vertexY, arm1X, arm1Y, arm2X, arm2Y)
                                val isEffectivelySnapped = isProtractorSnapped && currentMagnet
                                onMeasurementChanged(
                                    RulerMeasurementState(
                                        mainReadout = String.format(Locale.US, "%.1f°", angleDeg),
                                        subReadout = if (isEffectivelySnapped) "★ CALAMITA AGGANCIATA (%.0f°)".format(angleDeg) else String.format(Locale.US, "Radianti: %.3f rad", angleDeg * PI.toFloat() / 180f),
                                        tertiaryReadout = if (isEffectivelySnapped) "Angolo cardine: %.0f°".format(angleDeg) else "Goniometro Libero 0°..180°"
                                    )
                                )
                            }
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw Left Millimeter Graduation Scale
            drawGraduatedScale(
                engine = engine,
                tickStrokeWidth = tickStrokeWidth,
                graticulePaint = graticuleTextPaint,
                majorColor = onSurfaceColor,
                minorColor = onSurfaceVariantColor,
                subColor = outlineVariantColor
            )

            // Draw active mode graphics (Zero allocations)
            when (mode) {
                RulerMode.SINGLE_CURSOR -> {
                    // Horizontal laser guide line (Flame Orange)
                    drawLine(
                        color = primaryColor,
                        start = Offset(0f, cursor1Y),
                        end = Offset(size.width, cursor1Y),
                        strokeWidth = cursorStroke.width
                    )
                    // Cursor handle indicator
                    drawCircle(
                        color = primaryColor,
                        radius = 24f,
                        center = Offset(size.width - 48f, cursor1Y)
                    )
                    drawCircle(
                        color = surfaceColor,
                        radius = 12f,
                        center = Offset(size.width - 48f, cursor1Y)
                    )
                }

                RulerMode.DUAL_CALIPER -> {
                    // Top caliper arm (Flame Orange)
                    drawLine(
                        color = primaryColor,
                        start = Offset(0f, caliperTopY),
                        end = Offset(size.width, caliperTopY),
                        strokeWidth = cursorStroke.width
                    )
                    // Bottom caliper arm (Warm Amber)
                    drawLine(
                        color = secondaryColor,
                        start = Offset(0f, caliperBottomY),
                        end = Offset(size.width, caliperBottomY),
                        strokeWidth = cursorStroke.width
                    )
                    // Connecting vertical span
                    val spanX = size.width * 0.75f
                    drawLine(
                        color = primaryColor.copy(alpha = 0.6f),
                        start = Offset(spanX, caliperTopY),
                        end = Offset(spanX, caliperBottomY),
                        strokeWidth = hairlineStroke.width
                    )
                    // Handles
                    drawCircle(color = primaryColor, radius = 20f, center = Offset(size.width - 40f, caliperTopY))
                    drawCircle(color = secondaryColor, radius = 20f, center = Offset(size.width - 40f, caliperBottomY))
                }

                RulerMode.RECTANGLE_2D -> {
                    val left = minOf(rectLeft, rectRight)
                    val right = maxOf(rectLeft, rectRight)
                    val top = minOf(rectTop, rectBottom)
                    val bottom = maxOf(rectTop, rectBottom)
                    val rectSize = Size(right - left, bottom - top)

                    // Fill translucent
                    drawRect(
                        color = primaryColor.copy(alpha = 0.16f),
                        topLeft = Offset(left, top),
                        size = rectSize
                    )
                    // Outline
                    drawRect(
                        color = primaryColor,
                        topLeft = Offset(left, top),
                        size = rectSize,
                        style = outlineStroke
                    )
                    // Corner resize handle
                    drawCircle(color = primaryColor, radius = 20f, center = Offset(right, bottom))
                    drawCircle(color = secondaryColor, radius = 16f, center = Offset(left, top))
                }

                RulerMode.CIRCLE_RING -> {
                    // Outer ring
                    drawCircle(
                        color = primaryColor,
                        radius = circleRadiusPx,
                        center = Offset(circleCenterX, circleCenterY),
                        style = cursorStroke
                    )
                    // Translucent fill
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.14f),
                        radius = circleRadiusPx,
                        center = Offset(circleCenterX, circleCenterY)
                    )
                    // 45° Crosshair
                    val diagOffset = circleRadiusPx * 0.7071f
                    drawLine(
                        color = onSurfaceVariantColor,
                        start = Offset(circleCenterX - diagOffset, circleCenterY - diagOffset),
                        end = Offset(circleCenterX + diagOffset, circleCenterY + diagOffset),
                        strokeWidth = hairlineStroke.width
                    )
                    drawLine(
                        color = onSurfaceVariantColor,
                        start = Offset(circleCenterX - diagOffset, circleCenterY + diagOffset),
                        end = Offset(circleCenterX + diagOffset, circleCenterY - diagOffset),
                        strokeWidth = hairlineStroke.width
                    )
                    // Center point
                    drawCircle(color = secondaryColor, radius = 8f, center = Offset(circleCenterX, circleCenterY))
                    // Radius drag handle
                    drawCircle(color = primaryColor, radius = 20f, center = Offset(circleCenterX, circleCenterY + circleRadiusPx))
                }

                RulerMode.PROTRACTOR_180 -> {
                    val isVisualSnapped = isProtractorSnapped && isMagnetEnabled
                    val ray1Color = if (isVisualSnapped) PrecisionGreen else primaryColor
                    val ray2Color = if (isVisualSnapped) PrecisionGreen else secondaryColor
                    val vertexColor = if (isVisualSnapped) PrecisionGreen else secondaryColor

                    // Ray 1
                    drawLine(
                        color = ray1Color,
                        start = Offset(vertexX, vertexY),
                        end = Offset(arm1X, arm1Y),
                        strokeWidth = cursorStroke.width
                    )
                    // Ray 2
                    drawLine(
                        color = ray2Color,
                        start = Offset(vertexX, vertexY),
                        end = Offset(arm2X, arm2Y),
                        strokeWidth = cursorStroke.width
                    )

                    // Connecting Arc between the two rays
                    val arcRadius = 75f
                    val theta1Deg = (kotlin.math.atan2(arm1Y - vertexY, arm1X - vertexX) * 180f / PI.toFloat()).let { if (it < 0f) it + 360f else it }
                    val theta2Deg = (kotlin.math.atan2(arm2Y - vertexY, arm2X - vertexX) * 180f / PI.toFloat()).let { if (it < 0f) it + 360f else it }
                    var sweepDeg = theta2Deg - theta1Deg
                    while (sweepDeg > 180f) sweepDeg -= 360f
                    while (sweepDeg < -180f) sweepDeg += 360f

                    drawArc(
                        color = if (isVisualSnapped) PrecisionGreen.copy(alpha = 0.65f) else primaryColor.copy(alpha = 0.35f),
                        startAngle = theta1Deg,
                        sweepAngle = sweepDeg,
                        useCenter = false,
                        topLeft = Offset(vertexX - arcRadius, vertexY - arcRadius),
                        size = Size(arcRadius * 2f, arcRadius * 2f),
                        style = protractorArcStroke
                    )

                    // Vertex
                    drawCircle(color = vertexColor, radius = 22f, center = Offset(vertexX, vertexY))
                    drawCircle(color = surfaceColor, radius = 10f, center = Offset(vertexX, vertexY))
                    // Ray handles
                    drawCircle(color = ray1Color, radius = 20f, center = Offset(arm1X, arm1Y))
                    drawCircle(color = ray2Color, radius = 20f, center = Offset(arm2X, arm2Y))
                }
            }
        }
    }
}

/**
 * Draws the physical millimeter and centimeter graduation along the left edge.
 * Zero allocations inside loop.
 */
private fun DrawScope.drawGraduatedScale(
    engine: DisplayMetrologyEngine,
    tickStrokeWidth: Float,
    graticulePaint: AndroidPaint,
    majorColor: Color,
    minorColor: Color,
    subColor: Color
) {
    val totalHeightPx = size.height
    val pxPerMmY = engine.pxPerMmY
    if (pxPerMmY <= 0f) return

    val totalMm = (totalHeightPx / pxPerMmY).toInt()

    // Length of tick marks in pixels
    val tick1mmPx = engine.mmToPxX(4.0f)
    val tick5mmPx = engine.mmToPxX(7.0f)
    val tick10mmPx = engine.mmToPxX(10.0f)

    for (mm in 0..totalMm) {
        val y = mm * pxPerMmY
        val isCm = (mm % 10 == 0)
        val isHalfCm = (mm % 5 == 0 && !isCm)

        val tickLengthPx = when {
            isCm -> tick10mmPx
            isHalfCm -> tick5mmPx
            else -> tick1mmPx
        }

        val tickColor = when {
            isCm -> majorColor
            isHalfCm -> minorColor
            else -> subColor
        }

        drawLine(
            color = tickColor,
            start = Offset(0f, y),
            end = Offset(tickLengthPx, y),
            strokeWidth = tickStrokeWidth
        )

        // Draw centimeter numeric labels
        if (isCm && mm > 0) {
            val cmVal = mm / 10
            val textX = tick10mmPx + 12f
            val textY = y + 10f
            drawContext.canvas.nativeCanvas.drawText(
                cmVal.toString(),
                textX,
                textY,
                graticulePaint
            )
        }
    }
}
