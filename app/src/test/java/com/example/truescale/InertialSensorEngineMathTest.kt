package com.example.truescale

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class InertialSensorEngineMathTest {

    @Test
    fun testBubble2DRollPitch_flatZero() {
        val gx = 0.0f
        val gy = 0.0f
        val gz = 9.81f

        val roll = (atan2(gx, sqrt(gy * gy + gz * gz)) * 180f / PI.toFloat())
        val pitch = (atan2(gy, sqrt(gx * gx + gz * gz)) * 180f / PI.toFloat())
        val totalInc = sqrt(roll * roll + pitch * pitch)

        assertEquals(0.0f, roll, 0.001f)
        assertEquals(0.0f, pitch, 0.001f)
        assertEquals(0.0f, totalInc, 0.001f)
    }

    @Test
    fun testBubble2DRollPitch_tilted() {
        // 5 degrees roll
        val tiltRad = 5.0f * PI.toFloat() / 180f
        val gx = 9.81f * sin(tiltRad)
        val gz = 9.81f * cos(tiltRad)
        val gy = 0.0f

        val roll = (atan2(gx, sqrt(gy * gy + gz * gz)) * 180f / PI.toFloat())
        assertEquals(5.0f, roll, 0.01f)
    }

    @Test
    fun testVerticalClinometer_bottomEdge() {
        // Phone standing vertically on bottom edge, tilted by 7.5 degrees
        val tiltRad = 7.5f * PI.toFloat() / 180f
        val gx = 9.81f * sin(tiltRad)
        val gy = -9.81f * cos(tiltRad)

        val verticalAngle = (atan2(gx, -gy) * 180f / PI.toFloat())
        assertEquals(7.5f, verticalAngle, 0.01f)
    }

    @Test
    fun testTareCompensation() {
        val rawAngle = 1.25f // e.g. camera bump causes 1.25 degrees tilt
        val tareOffset = 1.25f
        val calibrated = rawAngle - tareOffset
        assertEquals(0.0f, calibrated, 0.001f)
    }

    @Test
    fun testAdaptiveEmaStep() {
        // Verify formula: y_k = alpha * x_k + (1 - alpha) * y_{k-1}
        val alpha = 0.06f
        var filtered = 0.0f
        val sample = 1.0f

        filtered = alpha * sample + (1f - alpha) * filtered
        assertEquals(0.06f, filtered, 0.0001f)

        filtered = alpha * sample + (1f - alpha) * filtered
        // 0.06 * 1 + 0.94 * 0.06 = 0.06 + 0.0564 = 0.1164
        assertEquals(0.1164f, filtered, 0.0001f)
    }
}
