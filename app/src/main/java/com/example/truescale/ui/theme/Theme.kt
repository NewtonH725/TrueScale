package com.example.truescale.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Official CrossFit Bull Moose Dark Theme (OLED / Antracite Caldo)
val DarkColorScheme = darkColorScheme(
    primary = BullMooseOrange,
    onPrimary = Color.White,
    primaryContainer = BullMooseOrangeContainerDark,
    onPrimaryContainer = OnBullMooseOrangeContainerDark,
    secondary = BullMooseWarmAmber,
    onSecondary = BullMooseBlack,
    secondaryContainer = BullMooseContainerLowDark,
    onSecondaryContainer = BullMooseWarmAmber,
    tertiary = BullMooseCoral,
    onTertiary = Color.White,
    background = BullMooseBlack,
    onBackground = BullMooseTextPrimaryDark,
    surface = BullMooseSurfaceDark,
    onSurface = BullMooseTextPrimaryDark,
    surfaceVariant = BullMooseContainerLowDark,
    onSurfaceVariant = BullMooseTextSecondaryDark,
    outline = BullMooseOutlineDark,
    outlineVariant = BullMooseOutlineVariantDark,
    error = DangerRed,
    onError = Color.White
)

// Official CrossFit Bull Moose Light Theme (Bianco Pietra / Antracite)
val LightColorScheme = lightColorScheme(
    primary = BullMooseOrange,
    onPrimary = Color.White,
    primaryContainer = BullMooseOrangeContainerLight,
    onPrimaryContainer = OnBullMooseOrangeContainerLight,
    secondary = BullMooseAmber,
    onSecondary = Color.White,
    secondaryContainer = BullMooseContainerLowLight,
    onSecondaryContainer = BullMooseAmber,
    tertiary = Color(0xFFD84315),
    onTertiary = Color.White,
    background = BullMooseBackgroundLight,
    onBackground = BullMooseTextPrimaryLight,
    surface = BullMooseSurfaceLight,
    onSurface = BullMooseTextPrimaryLight,
    surfaceVariant = BullMooseContainerLowLight,
    onSurfaceVariant = BullMooseTextSecondaryLight,
    outline = BullMooseOutlineLight,
    outlineVariant = BullMooseOutlineVariantLight,
    error = DangerRed,
    onError = Color.White
)

@Composable
fun TrueScaleTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDarkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (useDarkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                // Light status bars -> dark text on light backgrounds
                insetsController.isAppearanceLightStatusBars = !useDarkTheme
                insetsController.isAppearanceLightNavigationBars = !useDarkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
