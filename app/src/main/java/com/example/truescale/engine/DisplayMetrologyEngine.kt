package com.example.truescale.engine

import android.content.Context
import android.os.Build
import android.view.Display
import android.view.WindowManager
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.roundToInt

/**
 * DisplayMetrologyEngine
 *
 * Deterministic metrology engine that computes physical conversions between pixels
 * and millimetric/centimetric units with sub-pixel precision.
 *
 * It strictly avoids logical density (`Dp`) and directly derives scale factors from
 * hardware `xdpi` and `ydpi` while compensating for software display resolution downscaling.
 */
class DisplayMetrologyEngine(
    val xdpi: Float,
    val ydpi: Float,
    val scaleX: Float = 1.0f,
    val scaleY: Float = 1.0f,
    val calibrationFactor: Float = 1.0000f
) {
    companion object {
        const val MM_PER_INCH = 25.4f
        const val DEFAULT_CALIBRATION_FACTOR = 1.0000f
        const val CALIBRATION_STEP = 0.0005f
        const val MIN_CALIBRATION_FACTOR = 0.8500f
        const val MAX_CALIBRATION_FACTOR = 1.1500f

        // Standard ISO/IEC 7810 ID-1 card dimensions (Payment cards, driver licenses)
        const val ISO_CARD_WIDTH_MM = 85.60f
        const val ISO_CARD_HEIGHT_MM = 53.98f
        const val ISO_CARD_CORNER_RADIUS_MM = 3.18f

        /**
         * Factory function to create an engine instance from Android context,
         * inspecting Display.Mode vs WindowMetrics to detect software downscaling.
         */
        fun fromContext(context: Context, calibrationFactor: Float = DEFAULT_CALIBRATION_FACTOR): DisplayMetrologyEngine {
            val metrics = context.resources.displayMetrics
            var rawXdpi = if (metrics.xdpi in 96.0f..1000.0f) metrics.xdpi else metrics.densityDpi.toFloat()
            var rawYdpi = if (metrics.ydpi in 96.0f..1000.0f) metrics.ydpi else metrics.densityDpi.toFloat()

            var scaleX = 1.0f
            var scaleY = 1.0f

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                val currentBounds = windowManager?.currentWindowMetrics?.bounds
                val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    try {
                        context.display
                    } catch (e: Exception) {
                        null
                    }
                } else null

                val mode = display?.mode
                if (mode != null && currentBounds != null && currentBounds.width() > 0 && currentBounds.height() > 0) {
                    val physW = mode.physicalWidth.toFloat()
                    val physH = mode.physicalHeight.toFloat()
                    val winW = currentBounds.width().toFloat()
                    val winH = currentBounds.height().toFloat()

                    // Ensure orientation alignment (portrait vs landscape)
                    val (alignedPhysW, alignedPhysH) = if (winW < winH && physW > physH) {
                        physH to physW
                    } else {
                        physW to physH
                    }

                    if (winW > 0f && alignedPhysW > 0f) {
                        scaleX = alignedPhysW / winW
                    }
                    if (winH > 0f && alignedPhysH > 0f) {
                        scaleY = alignedPhysH / winH
                    }
                }
            }

            return DisplayMetrologyEngine(
                xdpi = rawXdpi,
                ydpi = rawYdpi,
                scaleX = scaleX,
                scaleY = scaleY,
                calibrationFactor = calibrationFactor
            )
        }
    }

    // Physical pixels per millimeter on X axis (compensated for downscaling & calibration)
    val pxPerMmX: Float = ((xdpi * scaleX) / MM_PER_INCH) * calibrationFactor

    // Physical pixels per millimeter on Y axis (compensated for downscaling & calibration)
    val pxPerMmY: Float = ((ydpi * scaleY) / MM_PER_INCH) * calibrationFactor

    fun mmToPxX(mm: Float): Float = mm * pxPerMmX

    fun mmToPxY(mm: Float): Float = mm * pxPerMmY

    fun pxToMmX(px: Float): Float = if (pxPerMmX > 0f) px / pxPerMmX else 0f

    fun pxToMmY(px: Float): Float = if (pxPerMmY > 0f) px / pxPerMmY else 0f

    fun pxToCmX(px: Float): Float = pxToMmX(px) / 10.0f

    fun pxToCmY(px: Float): Float = pxToMmY(px) / 10.0f

    /**
     * Calculates the area in cm² from width and height in pixels.
     */
    fun calculateRectangleAreaCm2(widthPx: Float, heightPx: Float): Float {
        val baseCm = pxToCmX(abs(widthPx))
        val heightCm = pxToCmY(abs(heightPx))
        return baseCm * heightCm
    }

    /**
     * ISO 8653: The European ring size corresponds to the inner circumference in millimeters.
     * EU Size = round(Circumference_mm) = round(PI * diameter_mm).
     */
    fun getEuRingSize(innerDiameterMm: Float): Int {
        val circumferenceMm = innerDiameterMm * PI.toFloat()
        return circumferenceMm.roundToInt()
    }

    fun euRingSizeToDiameterMm(euSize: Int): Float {
        return euSize.toFloat() / PI.toFloat()
    }

    /**
     * Calculates the convex angle in degrees between two rays originating from a vertex.
     * Uses atan2 for numerical stability.
     */
    fun calculateProtractorAngle(
        vertexX: Float, vertexY: Float,
        p1X: Float, p1Y: Float,
        p2X: Float, p2Y: Float
    ): Float {
        val angle1 = atan2(p1Y - vertexY, p1X - vertexX)
        val angle2 = atan2(p2Y - vertexY, p2X - vertexX)
        var diff = abs(angle2 - angle1)
        if (diff > PI.toFloat()) {
            diff = (2.0f * PI.toFloat()) - diff
        }
        return (diff * 180.0f / PI.toFloat())
    }
}
