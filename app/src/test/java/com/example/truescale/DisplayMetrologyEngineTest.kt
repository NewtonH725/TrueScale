package com.example.truescale

import com.example.truescale.engine.DisplayMetrologyEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class DisplayMetrologyEngineTest {

    @Test
    fun testMmPxConversion_standardDisplay() {
        val engine = DisplayMetrologyEngine(
            xdpi = 400.0f,
            ydpi = 400.0f,
            scaleX = 1.0f,
            scaleY = 1.0f,
            calibrationFactor = 1.0000f
        )

        val expectedPxPerMm = 400.0f / 25.4f
        assertEquals(expectedPxPerMm, engine.pxPerMmX, 0.001f)
        assertEquals(expectedPxPerMm, engine.pxPerMmY, 0.001f)

        // 85.60 mm (ISO card width)
        val cardWidthPx = engine.mmToPxX(85.60f)
        val backToMm = engine.pxToMmX(cardWidthPx)
        assertEquals(85.60f, backToMm, 0.01f)

        val backToCm = engine.pxToCmX(cardWidthPx)
        assertEquals(8.56f, backToCm, 0.001f)
    }

    @Test
    fun testDownscalingCompensation() {
        // Physical QHD+ 1440x3120 running at FHD+ 1080x2340
        val scale = 1440.0f / 1080.0f
        val engine = DisplayMetrologyEngine(
            xdpi = 390.0f,
            ydpi = 390.0f,
            scaleX = scale,
            scaleY = scale,
            calibrationFactor = 1.0000f
        )

        val expected = ((390.0f * scale) / 25.4f)
        assertEquals(expected, engine.pxPerMmX, 0.001f)
    }

    @Test
    fun testMicrometricCalibration() {
        val baseEngine = DisplayMetrologyEngine(400f, 400f, 1f, 1f, 1.0000f)
        val calibratedEngine = DisplayMetrologyEngine(400f, 400f, 1f, 1f, 1.0150f)

        val basePx = baseEngine.mmToPxX(100f)
        val calibPx = calibratedEngine.mmToPxX(100f)

        assertEquals(basePx * 1.0150f, calibPx, 0.01f)
    }

    @Test
    fun testIso8653RingSizes() {
        val engine = DisplayMetrologyEngine(400f, 400f)

        // Test known EU sizes:
        // EU 50 -> diameter ~ 15.92 mm
        val diameter50 = engine.euRingSizeToDiameterMm(50)
        val calculatedSize50 = engine.getEuRingSize(diameter50)
        assertEquals(50, calculatedSize50)

        // EU 60 -> diameter ~ 19.10 mm
        val diameter60 = engine.euRingSizeToDiameterMm(60)
        val calculatedSize60 = engine.getEuRingSize(diameter60)
        assertEquals(60, calculatedSize60)
    }

    @Test
    fun testRectangleAreaCalculation() {
        val engine = DisplayMetrologyEngine(400f, 400f)
        // 50 mm by 40 mm = 5 cm by 4 cm = 20 cm²
        val widthPx = engine.mmToPxX(50f)
        val heightPx = engine.mmToPxY(40f)

        val areaCm2 = engine.calculateRectangleAreaCm2(widthPx, heightPx)
        assertEquals(20.0f, areaCm2, 0.01f)
    }

    @Test
    fun testProtractorAngleCalculation() {
        val engine = DisplayMetrologyEngine(400f, 400f)

        // 90 degree angle: vertex at (0,0), p1 at (100, 0), p2 at (0, 100)
        val angle90 = engine.calculateProtractorAngle(
            vertexX = 0f, vertexY = 0f,
            p1X = 100f, p1Y = 0f,
            p2X = 0f, p2Y = 100f
        )
        assertEquals(90.0f, angle90, 0.01f)

        // 45 degree angle: vertex at (0,0), p1 at (100, 0), p2 at (100, 100)
        val angle45 = engine.calculateProtractorAngle(
            vertexX = 0f, vertexY = 0f,
            p1X = 100f, p1Y = 0f,
            p2X = 100f, p2Y = 100f
        )
        assertEquals(45.0f, angle45, 0.01f)
    }
}
