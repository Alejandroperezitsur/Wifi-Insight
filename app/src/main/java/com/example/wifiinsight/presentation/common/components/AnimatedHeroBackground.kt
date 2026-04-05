package com.example.wifiinsight.presentation.common.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Fondo dinámico animado que cambia según la calidad de señal.
 * Incluye gradientes fluidos, partículas sutiles y efectos de glow.
 */
@Composable
fun AnimatedHeroBackground(
    signalQuality: SignalQuality,
    modifier: Modifier = Modifier
) {
    val colors = when (signalQuality) {
        SignalQuality.EXCELLENT -> listOf(
            Color(0xFF00639A),
            Color(0xFF00B4D8),
            Color(0xFF5E60CE)
        )
        SignalQuality.GOOD -> listOf(
            Color(0xFF00C853),
            Color(0xFF64DD17),
            Color(0xFF00B4D8)
        )
        SignalQuality.FAIR -> listOf(
            Color(0xFFFFB300),
            Color(0xFFFF9100),
            Color(0xFFFF6D00)
        )
        SignalQuality.POOR -> listOf(
            Color(0xFFFF1744),
            Color(0xFFD50000),
            Color(0xFFB71C1C)
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "background")

    // Animación de gradiente fluido
    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradient_rotation"
    )

    // Pulso sutil de brillo
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.sweepGradient(
                    colors = colors,
                    center = Offset(0.5f, 0.5f)
                )
            )
            .graphicsLayer {
                rotationZ = animatedOffset
                scaleX = glowScale
                scaleY = glowScale
                alpha = glowAlpha
            }
    )
}

/**
 * Efecto de partículas flotantes sutiles
 */
@Composable
fun FloatingParticles(
    particleCount: Int = 20,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "particles")

    // Animar todos los ángulos juntos
    val animatedAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particles_rotation"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                val width = size.width
                val height = size.height

                for (i in 0 until particleCount) {
                    val offset = (i * 137.5f) % 360f
                    val radius = (i * 15f) % (width.coerceAtMost(height) / 2)
                    val angle = (offset + animatedAngle) * (PI / 180f)
                    
                    val x = width / 2 + cos(angle) * radius
                    val y = height / 2 + sin(angle) * radius

                    drawCircle(
                        color = Color.White.copy(alpha = 0.1f),
                        radius = 3f + (i % 3),
                        center = Offset(x.toFloat(), y.toFloat())
                    )
                }
            }
    )
}

/**
 * Efecto de glow pulsante para elementos destacados
 */
@Composable
fun PulsingGlow(
    color: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_scale"
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .background(
                color = color.copy(alpha = 0.3f),
                shape = androidx.compose.foundation.shape.CircleShape
            )
    )
}

enum class SignalQuality {
    EXCELLENT, GOOD, FAIR, POOR
}

/**
 * Obtiene la calidad de señal basada en porcentaje
 */
fun getSignalQuality(percentage: Int): SignalQuality {
    return when {
        percentage >= 80 -> SignalQuality.EXCELLENT
        percentage >= 60 -> SignalQuality.GOOD
        percentage >= 40 -> SignalQuality.FAIR
        else -> SignalQuality.POOR
    }
}
