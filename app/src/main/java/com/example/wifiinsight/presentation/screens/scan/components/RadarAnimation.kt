package com.example.wifiinsight.presentation.screens.scan.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun RadarAnimation(
    isScanning: Boolean,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    val infiniteTransition = rememberInfiniteTransition(label = "radar")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val pulse1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse1"
    )

    val pulse2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1f)),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = androidx.compose.animation.core.StartOffset(1000)
        ),
        label = "pulse2"
    )

    Box(
        modifier = modifier
            .size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val maxRadius = size.minDimension / 2 - 8.dp.toPx()

            drawRadarGrid(center, maxRadius, primaryColor.copy(alpha = 0.3f))

            if (isScanning) {
                drawSweep(center, maxRadius, rotation, primaryColor.copy(alpha = 0.6f))
                drawPulse(center, maxRadius, pulse1, secondaryColor.copy(alpha = 0.4f))
                drawPulse(center, maxRadius, pulse2, tertiaryColor.copy(alpha = 0.3f))
            }

            drawCenterDot(center, primaryColor)
        }
    }
}

private fun DrawScope.drawRadarGrid(center: Offset, maxRadius: Float, color: Color) {
    val strokeWidth = 1.5f.dp.toPx()

    for (i in 1..3) {
        val radius = maxRadius * i / 3
        drawCircle(
            color = color,
            radius = radius,
            center = center,
            style = Stroke(width = strokeWidth)
        )
    }

    drawLine(
        color = color,
        start = Offset(center.x - maxRadius, center.y),
        end = Offset(center.x + maxRadius, center.y),
        strokeWidth = strokeWidth
    )

    drawLine(
        color = color,
        start = Offset(center.x, center.y - maxRadius),
        end = Offset(center.x, center.y + maxRadius),
        strokeWidth = strokeWidth
    )
}

private fun DrawScope.drawSweep(center: Offset, maxRadius: Float, rotation: Float, color: Color) {
    val sweepRadius = maxRadius * 0.95f
    val startAngle = rotation - 45f
    val sweepAngle = 60f

    drawArc(
        color = color,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = true,
        topLeft = Offset(center.x - sweepRadius, center.y - sweepRadius),
        size = Size(sweepRadius * 2, sweepRadius * 2),
        alpha = 0.5f
    )
}

private fun DrawScope.drawPulse(center: Offset, maxRadius: Float, progress: Float, color: Color) {
    val pulseRadius = maxRadius * progress
    val alpha = (1f - progress) * 0.5f

    if (pulseRadius > 0) {
        drawCircle(
            color = color.copy(alpha = alpha),
            radius = pulseRadius,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

private fun DrawScope.drawCenterDot(center: Offset, color: Color) {
    drawCircle(
        color = color,
        radius = 6.dp.toPx(),
        center = center
    )
}
