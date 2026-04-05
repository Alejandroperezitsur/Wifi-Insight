package com.example.wifiinsight.presentation.screens.scan.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Radar WOW - Animación de escaneo tipo scanner real.
 * Ondas expansivas con alpha decay, partículas sutiles, y efectos premium.
 */
@Composable
fun RadarAnimation(
    isScanning: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")

    // Rotación del scanner - FIX #13: Reducido de 3000ms a 4000ms para menos CPU
    val scanAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_rotation"
    )

    // FIX #13: Solo una onda expansiva en lugar de dos
    val waveProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave"
    )

    // FIX #13: Eliminada animación de partículas (muy costosa en CPU)

    Box(
        modifier = modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val maxRadius = size.minDimension / 2 - 10f

            // Dibujar círculos concéntricos de fondo
            drawRadarCircles(centerX, centerY, maxRadius)

            // FIX #13: Solo una onda expansiva
            drawExpandingWave(centerX, centerY, maxRadius, waveProgress, 0)

            // FIX #13: Eliminado drawFloatingParticles (muy costoso)

            // Dibujar el scanner rotativo
            drawRotatingScanner(centerX, centerY, maxRadius, scanAngle)

            // Dibujar punto central pulsante
            drawCenterPoint(centerX, centerY)
        }
    }
}

private fun DrawScope.drawRadarCircles(
    centerX: Float,
    centerY: Float,
    maxRadius: Float
) {
    val circles = 4
    for (i in 1..circles) {
        val radius = maxRadius * i / circles
        val alpha = 0.1f + (i * 0.05f)
        drawCircle(
            color = Color(0xFF00B4D8).copy(alpha = alpha),
            radius = radius,
            center = Offset(centerX, centerY),
            style = Stroke(width = 1.5f)
        )
    }

    // Líneas cruzadas
    drawLine(
        color = Color(0xFF00B4D8).copy(alpha = 0.15f),
        start = Offset(centerX - maxRadius, centerY),
        end = Offset(centerX + maxRadius, centerY),
        strokeWidth = 1f
    )
    drawLine(
        color = Color(0xFF00B4D8).copy(alpha = 0.15f),
        start = Offset(centerX, centerY - maxRadius),
        end = Offset(centerX, centerY + maxRadius),
        strokeWidth = 1f
    )
}

private fun DrawScope.drawExpandingWave(
    centerX: Float,
    centerY: Float,
    maxRadius: Float,
    progress: Float,
    waveIndex: Int
) {
    val delay = waveIndex * 0.5f
    val adjustedProgress = (progress + delay) % 1f
    val radius = maxRadius * adjustedProgress
    val alpha = (1f - adjustedProgress) * 0.4f

    if (alpha > 0.01f) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF00B4D8).copy(alpha = alpha * 0.5f),
                    Color(0xFF00639A).copy(alpha = alpha),
                    Color.Transparent
                ),
                center = Offset(centerX, centerY),
                radius = radius
            ),
            radius = radius,
            center = Offset(centerX, centerY)
        )
    }
}

private fun DrawScope.drawRotatingScanner(
    centerX: Float,
    centerY: Float,
    maxRadius: Float,
    angle: Float
) {
    val radian = angle * (PI / 180f)
    val endX = centerX + cos(radian) * maxRadius
    val endY = centerY + sin(radian) * maxRadius

    // Línea del scanner con gradiente
    drawLine(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF00B4D8).copy(alpha = 0.9f),
                Color(0xFF00B4D8).copy(alpha = 0.3f),
                Color.Transparent
            ),
            start = Offset(centerX, centerY),
            end = Offset(endX.toFloat(), endY.toFloat())
        ),
        start = Offset(centerX, centerY),
        end = Offset(endX.toFloat(), endY.toFloat()),
        strokeWidth = 3f,
        cap = StrokeCap.Round
    )

    // Arco del scanner
    drawArc(
        color = Color(0xFF00B4D8).copy(alpha = 0.2f),
        startAngle = angle - 30f,
        sweepAngle = 60f,
        useCenter = true,
        topLeft = Offset(centerX - maxRadius, centerY - maxRadius),
        size = androidx.compose.ui.geometry.Size(maxRadius * 2, maxRadius * 2)
    )
}

private fun DrawScope.drawCenterPoint(
    centerX: Float,
    centerY: Float
) {
    // Círculo exterior
    drawCircle(
        color = Color(0xFF00B4D8).copy(alpha = 0.3f),
        radius = 12f,
        center = Offset(centerX, centerY)
    )

    // Círculo medio
    drawCircle(
        color = Color(0xFF00B4D8).copy(alpha = 0.6f),
        radius = 8f,
        center = Offset(centerX, centerY)
    )

    // Punto central brillante
    drawCircle(
        color = Color(0xFFFFFFFF),
        radius = 4f,
        center = Offset(centerX, centerY)
    )
}
