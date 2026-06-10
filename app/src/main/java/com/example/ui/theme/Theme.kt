package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ElegantDarkColorScheme = darkColorScheme(
    primary = ElegantDarkPrimary,
    secondary = ElegantDarkSecondary,
    tertiary = ElegantDarkTertiary,
    background = ElegantDarkBackground,
    surface = ElegantDarkSurface,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = ElegantDarkOnBackground,
    onSurface = ElegantDarkOnSurface,
    outline = ElegantDarkBorder
)

private val CosmicColorScheme = darkColorScheme(
    primary = CosmicPrimary,
    secondary = CosmicSecondary,
    tertiary = CosmicTertiary,
    background = CosmicBackground,
    surface = CosmicSurface,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color(0xFFF3F4F6),
    onSurface = Color(0xFFF3F4F6)
)

private val EmeraldColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    secondary = EmeraldSecondary,
    tertiary = EmeraldTertiary,
    background = EmeraldBackground,
    surface = EmeraldSurface,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color(0xFFECFDF5),
    onSurface = Color(0xFFECFDF5)
)

private val PureDarkColorScheme = darkColorScheme(
    primary = PureDarkPrimary,
    secondary = PureDarkSecondary,
    tertiary = PureDarkTertiary,
    background = PureDarkBackground,
    surface = PureDarkSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = LightModePrimary,
    secondary = LightModeSecondary,
    tertiary = LightModeTertiary,
    background = LightModeBackground,
    surface = LightModeSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A)
)

@Composable
fun MyApplicationTheme(
    themeName: String = "Emerald Green",
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeName) {
        "Cosmic Dark" -> CosmicColorScheme
        "Emerald Green", "Emerald" -> EmeraldColorScheme
        "Pure Dark" -> PureDarkColorScheme
        "Light Mode" -> LightColorScheme
        else -> ElegantDarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
