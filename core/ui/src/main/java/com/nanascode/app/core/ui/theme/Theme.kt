package com.nanascode.app.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NanasDarkColorScheme = darkColorScheme(
    primary = NanasYellow,
    onPrimary = Color.Black,
    primaryContainer = NanasYellowDark,
    onPrimaryContainer = Color.White,
    secondary = NanasGreen,
    onSecondary = Color.White,
    secondaryContainer = NanasGreenLight,
    onSecondaryContainer = Color.White,
    background = NanasDarkBg,
    onBackground = NanasTextPrimary,
    surface = NanasDarkSurface,
    onSurface = NanasTextPrimary,
    surfaceVariant = NanasDarkSurfaceLight,
    onSurfaceVariant = NanasTextSecondary,
    outline = NanasDarkBorder,
    error = NanasError,
    onError = Color.White
)

private val NanasAmoledColorScheme = darkColorScheme(
    primary = NanasYellow,
    onPrimary = Color.Black,
    primaryContainer = NanasYellowDark,
    onPrimaryContainer = Color.White,
    secondary = NanasGreen,
    onSecondary = Color.White,
    secondaryContainer = NanasGreenLight,
    onSecondaryContainer = Color.White,
    background = NanasAmoledBg,
    onBackground = NanasTextPrimary,
    surface = NanasAmoledSurface,
    onSurface = NanasTextPrimary,
    surfaceVariant = NanasDarkSurface,
    onSurfaceVariant = NanasTextSecondary,
    outline = NanasDarkBorder,
    error = NanasError,
    onError = Color.White
)

private val NanasLightColorScheme = lightColorScheme(
    primary = NanasYellowDark,
    onPrimary = Color.White,
    secondary = NanasGreen,
    onSecondary = Color.White,
    background = Color(0xFFF9F9F9),
    onBackground = Color(0xFF212121),
    surface = Color.White,
    onSurface = Color(0xFF212121),
    surfaceVariant = Color(0xFFEEEEEE),
    onSurfaceVariant = Color(0xFF616161),
    outline = Color(0xFFCCCCCC),
    error = NanasError,
    onError = Color.White
)

@Composable
fun NanasCodeTheme(
    isAmoled: Boolean = false,
    darkTheme: Boolean = true, // Default dark mode ala Acode
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        isAmoled -> NanasAmoledColorScheme
        darkTheme -> NanasDarkColorScheme
        else -> NanasLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
