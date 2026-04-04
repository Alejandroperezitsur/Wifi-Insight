package com.example.wifiinsight.presentation.common.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.min

@Composable
fun SignalChart(
    signalHistory: List<Int>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary
) {
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        if (signalHistory.isEmpty()) return@Canvas

        val padding = 8.dp.toPx()
        val width = size.width - 2 * padding
        val height = size.height - 2 * padding

        drawRect(
            color = surfaceColor,
            topLeft = Offset(padding, padding),
            size = androidx.compose.ui.geometry.Size(width, height)
        )

        drawGrid(padding, width, height)

        val maxRssi = 0f
        val minRssi = -100f
        val range = maxRssi - minRssi

        val points = signalHistory.takeLast(50).mapIndexed { index, rssi ->
            val x = padding + (index.toFloat() / min(49, signalHistory.size - 1).coerceAtLeast(1)) * width
            val y = padding + height - ((rssi - minRssi) / range) * height
            Offset(x, y.coerceIn(padding, padding + height))
        }

        if (points.size > 1) {
            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    lineTo(points[i].x, points[i].y)
                }
            }

            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        points.forEach { point ->
            drawCircle(
                color = lineColor,
                radius = 3.dp.toPx(),
                center = point
            )
        }
    }
}

private fun DrawScope.drawGrid(padding: Float, width: Float, height: Float) {
    val gridColor = Color.Gray.copy(alpha = 0.3f)
    val strokeWidth = 1.dp.toPx()

    for (i in 0..4) {
        val y = padding + (height / 4) * i
        drawLine(
            color = gridColor,
            start = Offset(padding, y),
            end = Offset(padding + width, y),
            strokeWidth = strokeWidth
        )
    }
}
