package com.example.wifiinsight.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// PALETA PRINCIPAL - Gradientes Premium
// ============================================================================

// Gradientes Principales (Azul Profundo → Cyan Brillante)
val GradientStart = Color(0xFF00639A)
val GradientEnd = Color(0xFF00B4D8)
val GradientAccent = Color(0xFF5E60CE)

// Superficies Elevadas - Dark Mode con profundidad
val SurfaceElevated0 = Color(0xFF121212)  // Background
val SurfaceElevated1 = Color(0xFF1E1E1E)  // Cards base
val SurfaceElevated2 = Color(0xFF2C2C2C)  // Elevated
val SurfaceElevated3 = Color(0xFF353535)  // Más elevado

// Superficies - Light Mode
val SurfaceLight0 = Color(0xFFFAFAFA)
val SurfaceLight1 = Color(0xFFFFFFFF)
val SurfaceLight2 = Color(0xFFF5F5F5)
val SurfaceLight3 = Color(0xFFE0E0E0)

// ============================================================================
// COLORES DE SEÑAL - Gradientes según intensidad
// ============================================================================

val SignalExcellentStart = Color(0xFF00C853)
val SignalExcellentEnd = Color(0xFF64DD17)
val SignalGoodStart = Color(0xFF64DD17)
val SignalGoodEnd = Color(0xFFAEEA00)
val SignalFairStart = Color(0xFFFFD600)
val SignalFairEnd = Color(0xFFFFAB00)
val SignalWeakStart = Color(0xFFFF9100)
val SignalWeakEnd = Color(0xFFFF6D00)
val SignalPoorStart = Color(0xFFFF1744)
val SignalPoorEnd = Color(0xFFD50000)

// Colores sólidos para iconos y textos
val SignalExcellent = Color(0xFF00C853)
val SignalGood = Color(0xFF64DD17)
val SignalFair = Color(0xFFFFB300)
val SignalWeak = Color(0xFFFF9100)
val SignalPoor = Color(0xFFFF1744)

// ============================================================================
// ESTADOS DE CONEXIÓN
// ============================================================================

val StatusConnected = Color(0xFF00C853)
val StatusConnecting = Color(0xFFFFB300)
val StatusDisconnected = Color(0xFF9E9E9E)
val StatusError = Color(0xFFFF1744)
val StatusWarning = Color(0xFFFF9100)

// ============================================================================
// ACCENTOS Y ÉNFASIS
// ============================================================================

val AccentPrimary = Color(0xFF00639A)
val AccentSecondary = Color(0xFF00B4D8)
val AccentTertiary = Color(0xFF5E60CE)
val AccentSuccess = Color(0xFF00C853)
val AccentWarning = Color(0xFFFFB300)
val AccentError = Color(0xFFFF1744)
val AccentInfo = Color(0xFF2196F3)

// ============================================================================
// TEXTOS
// ============================================================================

// Dark Mode
val OnSurfaceHighDark = Color(0xFFFFFFFF)
val OnSurfaceMediumDark = Color(0xFFB3B3B3)
val OnSurfaceLowDark = Color(0xFF666666)

// Light Mode
val OnSurfaceHighLight = Color(0xFF1A1A1A)
val OnSurfaceMediumLight = Color(0xFF666666)
val OnSurfaceLowLight = Color(0xFF999999)

// ============================================================================
// GRADIENTES PREARMADOS (Para usar en cards y backgrounds)
// ============================================================================

val GradientPrimary = listOf(GradientStart, GradientEnd)
val GradientSignalExcellent = listOf(SignalExcellentStart, SignalExcellentEnd)
val GradientSignalGood = listOf(SignalGoodStart, SignalGoodEnd)
val GradientSignalFair = listOf(SignalFairStart, SignalFairEnd)
val GradientSignalWeak = listOf(SignalWeakStart, SignalWeakEnd)
val GradientSignalPoor = listOf(SignalPoorStart, SignalPoorEnd)

// ============================================================================
// COLORES DE SEGURIDAD
// ============================================================================

val SecurityWPA3 = Color(0xFF00C853)
val SecurityWPA2 = Color(0xFF64DD17)
val SecurityWPA = Color(0xFFFFB300)
val SecurityWEP = Color(0xFFFF9100)
val SecurityOpen = Color(0xFFFF1744)

// ============================================================================
// COLORES LEGACY (para compatibilidad)
// ============================================================================

val Primary40 = AccentPrimary
val Primary80 = Color(0xFF98CBFF)
val Primary90 = Color(0xFFD0E4FF)
val Secondary40 = Color(0xFF006C4A)
val Secondary80 = Color(0xFF6CDBAC)
val Tertiary40 = AccentTertiary
val Tertiary80 = Color(0xFFD3BBFF)

// Legacy Material 3 colors
val Purple80 = Primary80
val PurpleGrey80 = Secondary80
val Pink80 = Tertiary80
val Purple40 = Primary40
val PurpleGrey40 = Secondary40
val Pink40 = Tertiary40

// Surface legacy
val SurfaceLight = SurfaceLight0
val SurfaceDark = SurfaceElevated0
val DarkSurface1 = SurfaceElevated1
val DarkSurface2 = SurfaceElevated2
val DarkSurface3 = SurfaceElevated3

// Error legacy
val Error40 = Color(0xFFB3261E)
val Error80 = Color(0xFFFFB4AB)

// Extended legacy
val NeutralVariant30 = Color(0xFF49454F)
val NeutralVariant50 = Color(0xFF79747E)
val NeutralVariant80 = Color(0xFFCAC4D0)

// Accent legacy
val AccentBlue = Color(0xFF2196F3)
val AccentGreen = AccentSuccess
val AccentOrange = SignalWeak
val AccentRed = AccentError

// ============================================================================
// FUNCIONES HELPER
// ============================================================================

/**
 * Obtiene el gradiente de señal según el porcentaje (0-100)
 */
fun getSignalGradient(percentage: Int): List<Color> {
    return when {
        percentage >= 80 -> GradientSignalExcellent
        percentage >= 60 -> GradientSignalGood
        percentage >= 40 -> GradientSignalFair
        percentage >= 20 -> GradientSignalWeak
        else -> GradientSignalPoor
    }
}

/**
 * Obtiene el color sólido de señal según el porcentaje
 */
fun getSignalColor(percentage: Int): Color {
    return when {
        percentage >= 80 -> SignalExcellent
        percentage >= 60 -> SignalGood
        percentage >= 40 -> SignalFair
        percentage >= 20 -> SignalWeak
        else -> SignalPoor
    }
}

/**
 * Obtiene color de seguridad según tipo
 */
fun getSecurityColor(type: String): Color {
    return when (type.uppercase()) {
        "WPA3" -> SecurityWPA3
        "WPA2" -> SecurityWPA2
        "WPA" -> SecurityWPA
        "WEP" -> SecurityWEP
        else -> SecurityOpen
    }
}