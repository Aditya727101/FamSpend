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
    primary = Color(0xFF8B6BFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4524A6),
    onPrimaryContainer = Color(0xFFEDE9FF),
    secondary = FamSuccess,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF1B5E20),
    onSecondaryContainer = Color(0xFFC8E6C9),
    tertiary = FamWarning,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE65100),
    onTertiaryContainer = Color(0xFFFFE0B2),
    background = Color(0xFF13111C),
    surface = Color(0xFF1E1A2E),
    surfaceVariant = Color(0xFF2A253D),
    onBackground = Color(0xFFF8F7FF),
    onSurface = Color(0xFFF8F7FF),
    onSurfaceVariant = Color(0xFFBDB8D6),
    outline = Color(0xFF423B5C),
    error = FamDanger
)

private val LightColorScheme = lightColorScheme(
    primary = FamPrimary, // #5C35D4
    onPrimary = Color.White,
    primaryContainer = FamPrimaryLight, // #EDE9FF
    onPrimaryContainer = FamPrimary,
    secondary = FamSuccess, // #4CAF50
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8F5E9),
    onSecondaryContainer = Color(0xFF2E7D32),
    tertiary = FamWarning, // #FF9800
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFF3E0),
    onTertiaryContainer = Color(0xFFE65100),
    background = FamBackground, // #F8F7FF
    surface = FamCardBackground, // #FFFFFF
    surfaceVariant = Color(0xFFF3F1FA),
    onBackground = FamTextPrimary, // #1A1A2E
    onSurface = FamTextPrimary, // #1A1A2E
    onSurfaceVariant = FamTextSecondary, // #6B7280
    outline = FamBorder, // #E5E7EB
    error = FamDanger // #F44336
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
