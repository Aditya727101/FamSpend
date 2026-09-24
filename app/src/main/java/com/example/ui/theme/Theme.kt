package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF818CF8), // Radiant Indigo
    onPrimary = Color(0xFF0F172A),
    primaryContainer = Color(0xFF312E81),
    onPrimaryContainer = Color(0xFFEEF2FF),
    secondary = FamSuccess,
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFF064E3B),
    onSecondaryContainer = Color(0xFFD1FAE5),
    tertiary = FamWarning,
    onTertiary = Color(0xFF0F172A),
    tertiaryContainer = Color(0xFF78350F),
    onTertiaryContainer = Color(0xFFFEF3C7),
    background = FamDarkBackground,
    surface = FamDarkSurface,
    surfaceVariant = FamDarkSurfaceVariant,
    onBackground = FamDarkTextPrimary,
    onSurface = FamDarkTextPrimary,
    onSurfaceVariant = FamDarkTextSecondary,
    outline = FamDarkBorder,
    error = FamDanger
)

private val LightColorScheme = lightColorScheme(
    primary = FamPrimary, // #4F46E5
    onPrimary = Color.White,
    primaryContainer = FamPrimaryLight, // #EEF2FF
    onPrimaryContainer = FamPrimary,
    secondary = FamSuccess, // #10B981
    onSecondary = Color.White,
    secondaryContainer = FamSuccessLight,
    onSecondaryContainer = FamSuccessDark,
    tertiary = FamWarning, // #F59E0B
    onTertiary = Color.White,
    tertiaryContainer = FamWarningLight,
    onTertiaryContainer = Color(0xFFB45309),
    background = FamBackground, // #F8FAFC
    surface = FamCardBackground, // #FFFFFF
    surfaceVariant = Color(0xFFF1F5F9), // Slate 100
    onBackground = FamTextPrimary, // #0F172A
    onSurface = FamTextPrimary, // #0F172A
    onSurfaceVariant = FamTextSecondary, // #64748B
    outline = FamBorder, // #E2E8F0
    error = FamDanger // #EF4444
)

@Composable
fun FamSpendTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
