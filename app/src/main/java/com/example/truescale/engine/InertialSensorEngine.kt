package com.example.truescale.engine

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * 5 Physical Resting Profiles for Tare/Offset Compensation.
 * Allows zeroing camera bumps, protruding buttons, and case bevels.
 */
enum class TareProfile {
    FLAT_BACK,
    BOTTOM_EDGE,
    TOP_EDGE,
    LEFT_EDGE,
    RIGHT_EDGE
}

/**
 * Visual mode of the spirit level / clinometer.
 */
enum class LevelOrientationMode {
    BUBBLE_2D,         // Flat horizontal plane
    VERTICAL_CLINOMETER, // Vertical edge or wall placement
    HYBRID_RULER_LEVEL  // Left edge ruler + center clinometer
}

/**
 * Snapshot data class holding all processed metrology sensor metrics.
 */
data class InertialSensorData(
    val rawGx: Float = 0f,
    val rawGy: Float = 0f,
    val rawGz: Float = 9.81f,
    val filteredGx: Float = 0f,
    val filteredGy: Float = 0f,
    val filteredGz: Float = 9.81f,
    val rollDeg: Float = 0f,
    val pitchDeg: Float = 0f,
    val totalInclinationDeg: Float = 0f,
    val verticalAngleDeg: Float = 0f,
    val slopePercent: Float = 0f,
    val isLevelAligned: Boolean = false, // True if within +/- 0.1 degree
    val isStationary: Boolean = true,
    val detectedProfile: TareProfile = TareProfile.FLAT_BACK,
    val activeMode: LevelOrientationMode = LevelOrientationMode.BUBBLE_2D
)

/**
 * InertialSensorEngine
 *
 * Captures 6-axis gravity/acceleration data, executes adaptive low-pass EMA filtering,
 * applies a 5-face tare matrix, and calculates real-time pitch, roll, and inclination angles.
 */
class InertialSensorEngine(
    private val context: Context,
    private val sensorDelay: Int = SensorManager.SENSOR_DELAY_GAME
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val gravitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
        ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _sensorData = MutableStateFlow(InertialSensorData())
    val sensorData: StateFlow<InertialSensorData> = _sensorData.asStateFlow()

    // Adaptive EMA state
    private var filteredGx = 0f
    private var filteredGy = 0f
    private var filteredGz = 9.81f
    private var prevRawGx = 0f
    private var prevRawGy = 0f
    private var prevRawGz = 9.81f
    private var isFirstSample = true

    // 5-profile Tare Matrix (Pitch & Roll offsets in degrees)
    private val tareOffsetsPitch = mutableMapOf<TareProfile, Float>()
    private val tareOffsetsRoll = mutableMapOf<TareProfile, Float>()
    private val tareOffsetsAngle = mutableMapOf<TareProfile, Float>()

    // Haptic alignment listener
    var onAlignmentConfirmed: (() -> Unit)? = null
    private var wasAligned = false

    var manualModeOverride: LevelOrientationMode? = null

    init {
        // Initialize tare matrix with default zeros
        TareProfile.entries.forEach {
            tareOffsetsPitch[it] = 0.0f
            tareOffsetsRoll[it] = 0.0f
            tareOffsetsAngle[it] = 0.0f
        }
    }

    fun startListening() {
        gravitySensor?.let { sensor ->
            sensorManager?.registerListener(this, sensor, sensorDelay)
        }
    }

    fun stopListening() {
        sensorManager?.unregisterListener(this)
    }

    fun setTareOffsets(
        profile: TareProfile,
        pitchOffset: Float = 0f,
        rollOffset: Float = 0f,
        angleOffset: Float = 0f
    ) {
        tareOffsetsPitch[profile] = pitchOffset
        tareOffsetsRoll[profile] = rollOffset
        tareOffsetsAngle[profile] = angleOffset
    }

    fun getTareOffset(profile: TareProfile): Triple<Float, Float, Float> {
        return Triple(
            tareOffsetsPitch[profile] ?: 0f,
            tareOffsetsRoll[profile] ?: 0f,
            tareOffsetsAngle[profile] ?: 0f
        )
    }

    fun calibrateCurrentAsTare(profile: TareProfile) {
        val current = _sensorData.value
        when (profile) {
            TareProfile.FLAT_BACK -> {
                tareOffsetsPitch[profile] = (tareOffsetsPitch[profile] ?: 0f) + current.pitchDeg
                tareOffsetsRoll[profile] = (tareOffsetsRoll[profile] ?: 0f) + current.rollDeg
            }
            TareProfile.BOTTOM_EDGE, TareProfile.TOP_EDGE,
            TareProfile.LEFT_EDGE, TareProfile.RIGHT_EDGE -> {
                tareOffsetsAngle[profile] = (tareOffsetsAngle[profile] ?: 0f) + current.verticalAngleDeg
            }
        }
    }

    fun resetTare(profile: TareProfile) {
        tareOffsetsPitch[profile] = 0f
        tareOffsetsRoll[profile] = 0f
        tareOffsetsAngle[profile] = 0f
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.values.size < 3) return

        val gx = event.values[0]
        val gy = event.values[1]
        val gz = event.values[2]

        if (isFirstSample) {
            filteredGx = gx
            filteredGy = gy
            filteredGz = gz
            prevRawGx = gx
            prevRawGy = gy
            prevRawGz = gz
            isFirstSample = false
        } else {
            // Adaptive EMA: derive delta norm
            val deltaX = gx - prevRawGx
            val deltaY = gy - prevRawGy
            val deltaZ = gz - prevRawGz
            val deltaNorm = sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ)

            // Static threshold: 0.15 m/s²
            val isStationary = deltaNorm < 0.15f
            val alpha = if (isStationary) 0.06f else 0.25f

            filteredGx = alpha * gx + (1.0f - alpha) * filteredGx
            filteredGy = alpha * gy + (1.0f - alpha) * filteredGy
            filteredGz = alpha * gz + (1.0f - alpha) * filteredGz

            prevRawGx = gx
            prevRawGy = gy
            prevRawGz = gz
        }

        // Detect dominant orientation and profile
        val absGx = abs(filteredGx)
        val absGy = abs(filteredGy)
        val absGz = abs(filteredGz)

        val detectedProfile: TareProfile
        val autoMode: LevelOrientationMode

        if (absGz > 7.5f) {
            detectedProfile = TareProfile.FLAT_BACK
            autoMode = LevelOrientationMode.BUBBLE_2D
        } else {
            autoMode = LevelOrientationMode.VERTICAL_CLINOMETER
            detectedProfile = when {
                absGy >= absGx && filteredGy < 0f -> TareProfile.BOTTOM_EDGE
                absGy >= absGx && filteredGy >= 0f -> TareProfile.TOP_EDGE
                absGx > absGy && filteredGx < 0f -> TareProfile.LEFT_EDGE
                else -> TareProfile.RIGHT_EDGE
            }
        }

        val activeMode = manualModeOverride ?: autoMode

        // Calculate 2D Bubble angles: roll and pitch
        val rawRollDeg = (atan2(filteredGx, sqrt(filteredGy * filteredGy + filteredGz * filteredGz)) * 180f / PI.toFloat())
        val rawPitchDeg = (atan2(filteredGy, sqrt(filteredGx * filteredGx + filteredGz * filteredGz)) * 180f / PI.toFloat())

        val pitchOffset = tareOffsetsPitch[TareProfile.FLAT_BACK] ?: 0f
        val rollOffset = tareOffsetsRoll[TareProfile.FLAT_BACK] ?: 0f

        val calibratedRollDeg = rawRollDeg - rollOffset
        val calibratedPitchDeg = rawPitchDeg - pitchOffset
        val totalInclinationDeg = sqrt(calibratedRollDeg * calibratedRollDeg + calibratedPitchDeg * calibratedPitchDeg)

        // Calculate Vertical Clinometer angle
        val rawVerticalAngleDeg = when (detectedProfile) {
            TareProfile.BOTTOM_EDGE -> atan2(filteredGx, -filteredGy) * 180f / PI.toFloat()
            TareProfile.TOP_EDGE -> atan2(-filteredGx, filteredGy) * 180f / PI.toFloat()
            TareProfile.LEFT_EDGE -> atan2(-filteredGy, -filteredGx) * 180f / PI.toFloat()
            TareProfile.RIGHT_EDGE -> atan2(filteredGy, filteredGx) * 180f / PI.toFloat()
            else -> atan2(filteredGx, -filteredGy) * 180f / PI.toFloat()
        }

        val angleOffset = tareOffsetsAngle[detectedProfile] ?: 0f
        val calibratedVerticalAngleDeg = rawVerticalAngleDeg - angleOffset

        // Slope percentage calculation
        val angleRad = abs(calibratedVerticalAngleDeg) * PI.toFloat() / 180.0f
        val slopePercent = (tan(angleRad) * 100.0f).coerceIn(0f, 999.9f)

        // Alignment threshold check: +/- 0.1 degree
        val isAligned = when (activeMode) {
            LevelOrientationMode.BUBBLE_2D -> totalInclinationDeg <= 0.1f
            LevelOrientationMode.VERTICAL_CLINOMETER -> abs(calibratedVerticalAngleDeg) <= 0.1f
            LevelOrientationMode.HYBRID_RULER_LEVEL -> abs(calibratedVerticalAngleDeg) <= 0.1f
        }

        if (isAligned && !wasAligned) {
            onAlignmentConfirmed?.invoke()
        }
        wasAligned = isAligned

        _sensorData.value = InertialSensorData(
            rawGx = gx,
            rawGy = gy,
            rawGz = gz,
            filteredGx = filteredGx,
            filteredGy = filteredGy,
            filteredGz = filteredGz,
            rollDeg = calibratedRollDeg,
            pitchDeg = calibratedPitchDeg,
            totalInclinationDeg = totalInclinationDeg,
            verticalAngleDeg = calibratedVerticalAngleDeg,
            slopePercent = slopePercent,
            isLevelAligned = isAligned,
            isStationary = abs(gx - prevRawGx) < 0.15f,
            detectedProfile = detectedProfile,
            activeMode = activeMode
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }
}
