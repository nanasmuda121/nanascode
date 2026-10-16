package com.nanascode.app.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NanasDarkColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF262626),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFE4E4E7),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF262626),
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
    primary = Color.White,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF1E1E1E),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFE4E4E7),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF1E1E1E),
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
    primary = Color.Black,
    onPrimary = Color.White,
    secondary = Color(0xFF3F3F46),
    onSecondary = Color.White,
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF18181B),
    surface = Color.White,
    onSurface = Color(0xFF18181B),
    surfaceVariant = Color(0xFFF4F4F5),
    onSurfaceVariant = Color(0xFF71717A),
    outline = Color(0xFFE4E4E7),
    error = NanasError,
    onError = Color.White
)

@Composable
fun NanasCodeTheme(
    darkTheme: Boolean = true,
    amoled: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        amoled -> NanasAmoledColorScheme
        darkTheme -> NanasDarkColorScheme
        else -> NanasLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
