package com.example.truescale

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SquareFoot
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.truescale.data.CalibrationRepository
import com.example.truescale.engine.DisplayMetrologyEngine
import com.example.truescale.engine.InertialSensorData
import com.example.truescale.engine.InertialSensorEngine
import com.example.truescale.engine.LevelOrientationMode
import com.example.truescale.engine.TareProfile
import com.example.truescale.ui.components.IsoCardCalibrationSheet
import com.example.truescale.ui.components.LevelCanvas
import com.example.truescale.ui.components.RotaryDrumPicker
import com.example.truescale.ui.components.RulerCanvas
import com.example.truescale.ui.components.RulerMeasurementState
import com.example.truescale.ui.components.RulerMode
import com.example.truescale.ui.components.SettingsSheet
import com.example.truescale.ui.components.TwoSlotToolSwitch
import com.example.truescale.ui.theme.PrecisionGreen
import com.example.truescale.ui.theme.TabularHeroReadout
import com.example.truescale.ui.theme.TabularSubReadout
import com.example.truescale.ui.theme.ThemeMode
import com.example.truescale.ui.theme.TrueScaleTheme
import com.example.truescale.utils.LocaleManager
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * 4 Main Instrumentation Suite Tabs (accessible via Bottom Rotary Drum).
 */
enum class MainAppTab(val titleRes: Int, val icon: ImageVector) {
    RULER(R.string.tool_regolo, Icons.Default.SquareFoot),
    LEVEL(R.string.tool_livella, Icons.Default.Navigation),
    CALIBRATION(R.string.tool_taratura, Icons.Default.Scale),
    SETTINGS(R.string.tool_impostazioni, Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private lateinit var sensorEngine: InertialSensorEngine
    private lateinit var calibrationRepository: CalibrationRepository
    private var hapticsEnabled by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Enforce saved language before content inflation
        LocaleManager.applySavedLanguage(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = getSharedPreferences("true_scale_prefs", Context.MODE_PRIVATE)
        val savedThemeName = prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        val initialTheme = try {
            ThemeMode.valueOf(savedThemeName)
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }
        hapticsEnabled = prefs.getBoolean("haptics_enabled", true)

        sensorEngine = InertialSensorEngine(this)
        calibrationRepository = CalibrationRepository(this)

        setContent {
            var themeMode by remember { mutableStateOf(initialTheme) }
            var currentLanguage by remember { mutableStateOf(LocaleManager.getSavedLanguage(this)) }

            TrueScaleTheme(themeMode = themeMode) {
                MainScreen(
                    sensorEngine = sensorEngine,
                    calibrationRepository = calibrationRepository,
                    hapticsEnabled = hapticsEnabled,
                    onHapticsToggled = { enabled ->
                        hapticsEnabled = enabled
                        prefs.edit().putBoolean("haptics_enabled", enabled).apply()
                    },
                    currentThemeMode = themeMode,
                    onThemeModeChanged = { newTheme ->
                        themeMode = newTheme
                        prefs.edit().putString("theme_mode", newTheme.name).apply()
                    },
                    currentLanguage = currentLanguage,
                    onLanguageChanged = { newLang ->
                        currentLanguage = newLang
                        LocaleManager.setAppLanguage(this@MainActivity, newLang)
                        recreate()
                    },
                    onHapticMmTick = { if (hapticsEnabled) performMmTickHaptic() },
                    onHapticAlignConfirm = { if (hapticsEnabled) performAlignConfirmHaptic() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        sensorEngine.startListening()
    }

    override fun onPause() {
        super.onPause()
        sensorEngine.stopListening()
    }

    private fun performMmTickHaptic() {
        val hapticConstant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            HapticFeedbackConstants.SEGMENT_FREQUENT_TICK
        } else {
            HapticFeedbackConstants.CLOCK_TICK
        }
        window.decorView.performHapticFeedback(hapticConstant)
    }

    private fun performAlignConfirmHaptic() {
        window.decorView.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }
}

@Composable
fun MainScreen(
    sensorEngine: InertialSensorEngine,
    calibrationRepository: CalibrationRepository,
    hapticsEnabled: Boolean,
    onHapticsToggled: (Boolean) -> Unit,
    currentThemeMode: ThemeMode,
    onThemeModeChanged: (ThemeMode) -> Unit,
    currentLanguage: String,
    onLanguageChanged: (String) -> Unit,
    onHapticMmTick: () -> Unit,
    onHapticAlignConfirm: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme

    val calibrationFactor by calibrationRepository.calibrationFactorFlow.collectAsState(initial = 1.0000f)
    val tareMatrix by calibrationRepository.tareMatrixFlow.collectAsState(initial = CalibrationRepository.TareMatrix())
    val sensorData by sensorEngine.sensorData.collectAsState()

    // Sync tare matrix with sensor engine
    DisposableEffect(tareMatrix) {
        sensorEngine.setTareOffsets(TareProfile.FLAT_BACK, tareMatrix.flatPitch, tareMatrix.flatRoll, 0f)
        sensorEngine.setTareOffsets(TareProfile.BOTTOM_EDGE, 0f, 0f, tareMatrix.bottomAngle)
        sensorEngine.setTareOffsets(TareProfile.TOP_EDGE, 0f, 0f, tareMatrix.topAngle)
        sensorEngine.setTareOffsets(TareProfile.LEFT_EDGE, 0f, 0f, tareMatrix.leftAngle)
        sensorEngine.setTareOffsets(TareProfile.RIGHT_EDGE, 0f, 0f, tareMatrix.rightAngle)
        onDispose { }
    }

    val displayEngine = remember(calibrationFactor) {
        DisplayMetrologyEngine.fromContext(context, calibrationFactor)
    }

    var selectedTab by remember { mutableStateOf(MainAppTab.RULER) }
    var lastActiveMainTool by remember { mutableStateOf(MainAppTab.RULER) }
    var selectedRulerMode by remember { mutableStateOf(RulerMode.SINGLE_CURSOR) }
    var rulerMeasurement by remember { mutableStateOf(RulerMeasurementState(mainReadout = "0.00 cm", subReadout = "0.0 mm")) }
    var isProtractorMagnetEnabled by remember { mutableStateOf(true) }

    // Connect alignment confirmation callback EXCLUSIVELY when Level is active
    DisposableEffect(selectedTab, hapticsEnabled) {
        if (selectedTab == MainAppTab.LEVEL && hapticsEnabled) {
            sensorEngine.onAlignmentConfirmed = {
                onHapticAlignConfirm()
            }
        } else {
            sensorEngine.onAlignmentConfirmed = null
        }
        onDispose {
            sensorEngine.onAlignmentConfirmed = null
        }
    }

    // Track the last opened main measurement tool between Ruler and Level
    androidx.compose.runtime.LaunchedEffect(selectedTab) {
        if (selectedTab == MainAppTab.RULER || selectedTab == MainAppTab.LEVEL) {
            lastActiveMainTool = selectedTab
        }
    }

    // Predictive Back / Back gesture: Return to the last opened tool (Ruler or Level) from sub-screens
    BackHandler(enabled = selectedTab == MainAppTab.CALIBRATION || selectedTab == MainAppTab.SETTINGS) {
        selectedTab = lastActiveMainTool
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { _ ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Main Tool Viewport
            when (selectedTab) {
                MainAppTab.RULER -> {
                    RulerCanvas(
                        engine = displayEngine,
                        mode = selectedRulerMode,
                        modifier = Modifier.fillMaxSize(),
                        isMagnetEnabled = isProtractorMagnetEnabled,
                        onMmCrossed = onHapticMmTick,
                        onMeasurementChanged = { rulerMeasurement = it }
                    )
                }

                MainAppTab.LEVEL -> {
                    LevelCanvas(
                        sensorData = sensorData,
                        engine = displayEngine,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                MainAppTab.CALIBRATION -> {
                    IsoCardCalibrationSheet(
                        engine = displayEngine,
                        currentFactor = calibrationFactor,
                        onSaveFactor = { factor ->
                            coroutineScope.launch {
                                calibrationRepository.saveCalibrationFactor(factor)
                            }
                        },
                        onResetFactor = {
                            coroutineScope.launch {
                                calibrationRepository.resetCalibrationFactor()
                            }
                        },
                        onBack = { selectedTab = lastActiveMainTool },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                MainAppTab.SETTINGS -> {
                    SettingsSheet(
                        engine = displayEngine,
                        hapticsEnabled = hapticsEnabled,
                        onHapticsToggled = onHapticsToggled,
                        currentThemeMode = currentThemeMode,
                        onThemeModeChanged = onThemeModeChanged,
                        currentLanguage = currentLanguage,
                        onLanguageChanged = onLanguageChanged,
                        onResetAllTares = {
                            coroutineScope.launch {
                                calibrationRepository.resetAllTares()
                                TareProfile.entries.forEach { sensorEngine.resetTare(it) }
                            }
                        },
                        onResetDisplayFactor = {
                            coroutineScope.launch {
                                calibrationRepository.resetCalibrationFactor()
                            }
                        },
                        onBack = { selectedTab = lastActiveMainTool },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Upper-Third Centered HUD Area (Ruler and Level only)
            when (selectedTab) {
                MainAppTab.RULER -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .padding(top = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Frameless Clean Tabular Readout
                        Text(
                            text = rulerMeasurement.mainReadout.ifEmpty { "0.00 cm" },
                            style = TabularHeroReadout.copy(fontSize = 38.sp),
                            color = colorScheme.primary,
                            maxLines = 1
                        )
                        if (rulerMeasurement.subReadout.isNotEmpty()) {
                            Text(
                                text = rulerMeasurement.subReadout,
                                style = TabularSubReadout.copy(fontSize = 15.sp),
                                color = colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Central 3D Rotary Drum for Ruler functionalities + Magnet Toggle Button on the right
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            RotaryDrumPicker(
                                items = RulerMode.entries,
                                selectedItem = selectedRulerMode,
                                onItemSelected = { selectedRulerMode = it },
                                labelOf = { mode ->
                                    when (mode) {
                                        RulerMode.SINGLE_CURSOR -> context.getString(R.string.tool_single_cursor)
                                        RulerMode.DUAL_CALIPER -> context.getString(R.string.tool_calibro)
                                        RulerMode.RECTANGLE_2D -> context.getString(R.string.tool_area_2d)
                                        RulerMode.CIRCLE_RING -> context.getString(R.string.tool_anelli)
                                        RulerMode.PROTRACTOR_180 -> context.getString(R.string.tool_goniometro)
                                    }
                                },
                                onHapticTick = onHapticMmTick,
                                width = 170.dp,
                                height = 118.dp
                            )

                            // Magnet Toggle Button (Visible exclusively when Protractor 180 is active)
                            if (selectedRulerMode == RulerMode.PROTRACTOR_180) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .offset(x = 118.dp)
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isProtractorMagnetEnabled) colorScheme.primary.copy(alpha = 0.22f)
                                            else colorScheme.surfaceVariant.copy(alpha = 0.12f)
                                        )
                                        .border(
                                            1.dp,
                                            if (isProtractorMagnetEnabled) colorScheme.primary.copy(alpha = 0.85f)
                                            else colorScheme.outlineVariant.copy(alpha = 0.25f),
                                            CircleShape
                                        )
                                        .clickable {
                                            onHapticMmTick()
                                            isProtractorMagnetEnabled = !isProtractorMagnetEnabled
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_magnet),
                                        contentDescription = "Calamita Angoli Goniometro",
                                        tint = if (isProtractorMagnetEnabled) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.40f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                MainAppTab.LEVEL -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .padding(top = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val isAligned = sensorData.isLevelAligned
                        val activeColor = if (isAligned) PrecisionGreen else colorScheme.primary

                        val angleText = when (sensorData.activeMode) {
                            LevelOrientationMode.BUBBLE_2D -> String.format(Locale.US, "%.1f°", sensorData.totalInclinationDeg)
                            LevelOrientationMode.VERTICAL_CLINOMETER, LevelOrientationMode.HYBRID_RULER_LEVEL -> String.format(Locale.US, "%+.1f°", sensorData.verticalAngleDeg)
                        }
                        Text(
                            text = angleText,
                            style = TabularHeroReadout.copy(fontSize = 38.sp),
                            color = activeColor,
                            maxLines = 1
                        )
                        val subInfo = when (sensorData.activeMode) {
                            LevelOrientationMode.BUBBLE_2D -> String.format(Locale.US, "%s: %+.1f° | %s: %+.1f°", stringResource(R.string.roll_label), sensorData.rollDeg, stringResource(R.string.pitch_label), sensorData.pitchDeg)
                            else -> String.format(Locale.US, "%s: %.1f%%", stringResource(R.string.slope_label), sensorData.slopePercent)
                        }
                        Text(
                            text = if (isAligned) stringResource(R.string.level_aligned) else subInfo,
                            style = TabularSubReadout.copy(
                                fontSize = 14.sp,
                                color = if (isAligned) PrecisionGreen else colorScheme.onSurfaceVariant,
                                fontWeight = if (isAligned) FontWeight.Bold else FontWeight.Normal
                            ),
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Rotary drum for Level modes (Auto, Bolla 2D, Clinometro, Ibrido)
                        val autoTitle = stringResource(R.string.level_auto)
                        val bubbleTitle = stringResource(R.string.level_bubble)
                        val clinometerTitle = stringResource(R.string.level_clinometer)
                        val hybridTitle = stringResource(R.string.level_hybrid)

                        val levelModes = listOf(
                            null to autoTitle,
                            LevelOrientationMode.BUBBLE_2D to bubbleTitle,
                            LevelOrientationMode.VERTICAL_CLINOMETER to clinometerTitle,
                            LevelOrientationMode.HYBRID_RULER_LEVEL to hybridTitle
                        )
                        val currentLevelPair = levelModes.firstOrNull { it.first == sensorEngine.manualModeOverride } ?: levelModes[0]

                        RotaryDrumPicker(
                            items = levelModes,
                            selectedItem = currentLevelPair,
                            onItemSelected = { sensorEngine.manualModeOverride = it.first },
                            labelOf = { it.second },
                            onHapticTick = onHapticMmTick,
                            width = 170.dp,
                            height = 118.dp
                        )
                    }
                }

                MainAppTab.CALIBRATION, MainAppTab.SETTINGS -> {
                    // Full-screen dedicated views without HUD overlay
                }
            }

            // Bottom HUD Control Suite:
            // Left: Settings (Ultra-transparent Gear) | Center: Compact 2-Slot Switch (Ruler / Level) | Right: Calibration (Ultra-transparent Scale)
            // Visible exclusively on main tools. Smoothly hidden in Calibration and Settings.
            AnimatedVisibility(
                visible = selectedTab == MainAppTab.RULER || selectedTab == MainAppTab.LEVEL,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 32.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Ultra-transparent Settings Action Button (Gear Icon)
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(colorScheme.surfaceVariant.copy(alpha = 0.12f))
                            .border(1.dp, colorScheme.outlineVariant.copy(alpha = 0.20f), CircleShape)
                            .clickable {
                                onHapticMmTick()
                                selectedTab = MainAppTab.SETTINGS
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.tool_impostazioni),
                            tint = colorScheme.primary.copy(alpha = 0.85f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Center: Compact 2-Slot Vertical Switch (Regolo <-> Livella)
                    TwoSlotToolSwitch(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it },
                        onHapticTick = onHapticMmTick,
                        width = 142.dp,
                        height = 74.dp
                    )

                    // Right: Ultra-transparent Calibration Action Button (Scale / Bilancia Icon)
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(colorScheme.surfaceVariant.copy(alpha = 0.12f))
                            .border(1.dp, colorScheme.outlineVariant.copy(alpha = 0.20f), CircleShape)
                            .clickable {
                                onHapticMmTick()
                                selectedTab = MainAppTab.CALIBRATION
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Scale,
                            contentDescription = stringResource(R.string.tool_taratura),
                            tint = colorScheme.primary.copy(alpha = 0.85f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}
