package com.example.wifiinsight.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ============================================================================
// ESQUEMAS DE COLORES PREMIUM
// ============================================================================

private val DarkColorScheme = darkColorScheme(
    primary = GradientEnd,
    onPrimary = OnSurfaceHighDark,
    primaryContainer = SurfaceElevated2,
    onPrimaryContainer = OnSurfaceHighDark,
    secondary = AccentSecondary,
    onSecondary = OnSurfaceHighDark,
    secondaryContainer = SurfaceElevated2,
    onSecondaryContainer = OnSurfaceHighDark,
    tertiary = AccentTertiary,
    onTertiary = OnSurfaceHighDark,
    tertiaryContainer = SurfaceElevated2,
    onTertiaryContainer = OnSurfaceHighDark,
    error = AccentError,
    onError = OnSurfaceHighDark,
    errorContainer = SurfaceElevated2,
    onErrorContainer = OnSurfaceHighDark,
    background = SurfaceElevated0,
    onBackground = OnSurfaceHighDark,
    surface = SurfaceElevated1,
    onSurface = OnSurfaceHighDark,
    surfaceVariant = SurfaceElevated2,
    onSurfaceVariant = OnSurfaceMediumDark,
    outline = OnSurfaceLowDark,
    outlineVariant = SurfaceElevated3,
    scrim = SurfaceElevated0
)

private val LightColorScheme = lightColorScheme(
    primary = GradientStart,
    onPrimary = OnSurfaceHighLight,
    primaryContainer = SurfaceLight2,
    onPrimaryContainer = OnSurfaceHighLight,
    secondary = AccentSecondary,
    onSecondary = OnSurfaceHighLight,
    secondaryContainer = SurfaceLight2,
    onSecondaryContainer = OnSurfaceHighLight,
    tertiary = AccentTertiary,
    onTertiary = OnSurfaceHighLight,
    tertiaryContainer = SurfaceLight2,
    onTertiaryContainer = OnSurfaceHighLight,
    error = AccentError,
    onError = OnSurfaceHighLight,
    errorContainer = SurfaceLight2,
    onErrorContainer = OnSurfaceHighLight,
    background = SurfaceLight0,
    onBackground = OnSurfaceHighLight,
    surface = SurfaceLight1,
    onSurface = OnSurfaceHighLight,
    surfaceVariant = SurfaceLight2,
    onSurfaceVariant = OnSurfaceMediumLight,
    outline = OnSurfaceLowLight,
    outlineVariant = SurfaceLight3,
    scrim = SurfaceLight0
)

@Composable
fun WifiInsightTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disabled for consistent branding
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = WifiInsightTypography,
        content = content
    )
}