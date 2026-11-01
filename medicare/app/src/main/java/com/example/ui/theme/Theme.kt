package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = HealthBluePrimaryDark,
    onPrimary = HealthBlueOnPrimaryDark,
    primaryContainer = HealthBlueContainerDark,
    onPrimaryContainer = HealthBlueOnContainerDark,
    secondary = HealthTealSecondaryDark,
    onSecondary = HealthTealOnSecondaryDark,
    secondaryContainer = HealthTealContainerDark,
    onSecondaryContainer = HealthTealOnContainerDark,
    tertiary = HealthGreenTertiaryDark,
    onTertiary = HealthGreenOnTertiaryDark,
    tertiaryContainer = HealthGreenContainerDark,
    onTertiaryContainer = HealthGreenOnContainerDark,
    background = HealthBackgroundDark,
    onBackground = HealthOnSurfaceDark,
    surface = HealthSurfaceDark,
    onSurface = HealthOnSurfaceDark,
    surfaceVariant = HealthSurfaceVariantDark,
    outline = HealthOutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = HealthBluePrimary,
    onPrimary = HealthBlueOnPrimary,
    primaryContainer = HealthBlueContainer,
    onPrimaryContainer = HealthBlueOnContainer,
    secondary = HealthTealSecondary,
    onSecondary = HealthTealOnSecondary,
    secondaryContainer = HealthTealContainer,
    onSecondaryContainer = HealthTealOnContainer,
    tertiary = HealthGreenTertiary,
    onTertiary = HealthGreenOnTertiary,
    tertiaryContainer = HealthGreenContainer,
    onTertiaryContainer = HealthGreenOnContainer,
    background = HealthBackgroundLight,
    onBackground = HealthOnSurfaceLight,
    surface = HealthSurfaceLight,
    onSurface = HealthOnSurfaceLight,
    surfaceVariant = HealthSurfaceVariantLight,
    outline = HealthOutlineLight
)

@Composable
fun MediCareTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
