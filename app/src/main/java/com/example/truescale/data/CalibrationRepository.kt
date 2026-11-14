package com.example.truescale.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.truescale.engine.TareProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "true_scale_calibration")

class CalibrationRepository(private val context: Context) {

    companion object {
        val KEY_CALIBRATION_FACTOR = floatPreferencesKey("calibration_factor")

        // 5 Tare Profile Keys
        val KEY_FLAT_PITCH = floatPreferencesKey("tare_flat_pitch")
        val KEY_FLAT_ROLL = floatPreferencesKey("tare_flat_roll")
        val KEY_BOTTOM_ANGLE = floatPreferencesKey("tare_bottom_angle")
        val KEY_TOP_ANGLE = floatPreferencesKey("tare_top_angle")
        val KEY_LEFT_ANGLE = floatPreferencesKey("tare_left_angle")
        val KEY_RIGHT_ANGLE = floatPreferencesKey("tare_right_angle")
    }

    val calibrationFactorFlow: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[KEY_CALIBRATION_FACTOR] ?: 1.0000f
    }

    suspend fun saveCalibrationFactor(factor: Float) {
        context.dataStore.edit { preferences ->
            preferences[KEY_CALIBRATION_FACTOR] = factor
        }
    }

    suspend fun resetCalibrationFactor() {
        context.dataStore.edit { preferences ->
            preferences[KEY_CALIBRATION_FACTOR] = 1.0000f
        }
    }

    data class TareMatrix(
        val flatPitch: Float = 0f,
        val flatRoll: Float = 0f,
        val bottomAngle: Float = 0f,
        val topAngle: Float = 0f,
        val leftAngle: Float = 0f,
        val rightAngle: Float = 0f
    )

    val tareMatrixFlow: Flow<TareMatrix> = context.dataStore.data.map { preferences ->
        TareMatrix(
            flatPitch = preferences[KEY_FLAT_PITCH] ?: 0f,
            flatRoll = preferences[KEY_FLAT_ROLL] ?: 0f,
            bottomAngle = preferences[KEY_BOTTOM_ANGLE] ?: 0f,
            topAngle = preferences[KEY_TOP_ANGLE] ?: 0f,
            leftAngle = preferences[KEY_LEFT_ANGLE] ?: 0f,
            rightAngle = preferences[KEY_RIGHT_ANGLE] ?: 0f
        )
    }

    suspend fun saveTareOffset(profile: TareProfile, pitch: Float = 0f, roll: Float = 0f, angle: Float = 0f) {
        context.dataStore.edit { preferences ->
            when (profile) {
                TareProfile.FLAT_BACK -> {
                    preferences[KEY_FLAT_PITCH] = pitch
                    preferences[KEY_FLAT_ROLL] = roll
                }
                TareProfile.BOTTOM_EDGE -> preferences[KEY_BOTTOM_ANGLE] = angle
                TareProfile.TOP_EDGE -> preferences[KEY_TOP_ANGLE] = angle
                TareProfile.LEFT_EDGE -> preferences[KEY_LEFT_ANGLE] = angle
                TareProfile.RIGHT_EDGE -> preferences[KEY_RIGHT_ANGLE] = angle
            }
        }
    }

    suspend fun resetTareProfile(profile: TareProfile) {
        saveTareOffset(profile, 0f, 0f, 0f)
    }

    suspend fun resetAllTares() {
        context.dataStore.edit { preferences ->
            preferences[KEY_FLAT_PITCH] = 0f
            preferences[KEY_FLAT_ROLL] = 0f
            preferences[KEY_BOTTOM_ANGLE] = 0f
            preferences[KEY_TOP_ANGLE] = 0f
            preferences[KEY_LEFT_ANGLE] = 0f
            preferences[KEY_RIGHT_ANGLE] = 0f
        }
    }
}
