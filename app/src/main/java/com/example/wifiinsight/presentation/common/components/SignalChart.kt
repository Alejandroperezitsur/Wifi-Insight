package com.example.wifiinsight.presentation.common.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp

private const val CHART_MAX_RSSI = -30f
private const val CHART_MIN_RSSI = -90f
private const val CHART_BASELINE_RSSI = -70f

@Composable
fun SignalChart(
    signalHistory: List<Int>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary
) {
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
    val baselineColor = MaterialTheme.colorScheme.tertiary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
    ) {
        val validHistory = signalHistory
            .filter { it != -127 }
            .takeLast(50)

        if (validHistory.isEmpty()) {
            return@Canvas
        }

        val leftPadding = 42.dp.toPx()
        val topPadding = 12.dp.toPx()
        val rightPadding = 14.dp.toPx()
        val bottomPadding = 18.dp.toPx()
        val chartWidth = size.width - leftPadding - rightPadding
        val chartHeight = size.height - topPadding - bottomPadding

        drawRect(
            color = surfaceColor,
            topLeft = Offset(leftPadding, topPadding),
            size = Size(chartWidth, chartHeight)
        )

        drawGrid(
            leftPadding = leftPadding,
            topPadding = topPadding,
            chartWidth = chartWidth,
            chartHeight = chartHeight,
            gridColor = gridColor,
            labelColor = labelColor
        )

        drawBaseline(
            leftPadding = leftPadding,
            topPadding = topPadding,
            chartWidth = chartWidth,
            chartHeight = chartHeight,
            baselineColor = baselineColor
        )

        val points = validHistory.mapIndexed { index, rssi ->
            Offset(
                x = leftPadding + (index.toFloat() / (validHistory.size - 1).coerceAtLeast(1)) * chartWidth,
                y = mapRssiToY(
                    rssi = rssi.toFloat(),
                    topPadding = topPadding,
                    chartHeight = chartHeight
                )
            )
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

        points.dropLast(1).forEach { point ->
            drawCircle(
                color = lineColor.copy(alpha = 0.45f),
                radius = 3.dp.toPx(),
                center = point
            )
        }

        val lastPoint = points.last()
        drawCircle(
            color = lineColor,
            radius = 6.dp.toPx(),
            center = lastPoint
        )
        drawCircle(
            color = surfaceColor,
            radius = 2.5.dp.toPx(),
            center = lastPoint
        )

        drawContext.canvas.nativeCanvas.drawText(
            "${validHistory.last()} dBm",
            lastPoint.x - 28.dp.toPx(),
            topPadding - 2.dp.toPx(),
            Paint().apply {
                color = labelColor.toArgb()
                textSize = 11.dp.toPx()
                isAntiAlias = true
                textAlign = Paint.Align.LEFT
            }
        )
    }
}

private fun DrawScope.drawGrid(
    leftPadding: Float,
    topPadding: Float,
    chartWidth: Float,
    chartHeight: Float,
    gridColor: Color,
    labelColor: Color
) {
    val strokeWidth = 1.dp.toPx()
    val labelPaint = Paint().apply {
        color = labelColor.toArgb()
        textSize = 10.dp.toPx()
        isAntiAlias = true
        textAlign = Paint.Align.LEFT
    }

    listOf(-30f, -50f, -70f, -90f).forEach { rssi ->
        val y = mapRssiToY(rssi, topPadding, chartHeight)
        drawLine(
            color = gridColor,
            start = Offset(leftPadding, y),
            end = Offset(leftPadding + chartWidth, y),
            strokeWidth = strokeWidth
        )
        drawContext.canvas.nativeCanvas.drawText(
            rssi.toInt().toString(),
            4.dp.toPx(),
            y + 4.dp.toPx(),
            labelPaint
        )
    }
}

private fun DrawScope.drawBaseline(
    leftPadding: Float,
    topPadding: Float,
    chartWidth: Float,
    chartHeight: Float,
    baselineColor: Color
) {
    val y = mapRssiToY(CHART_BASELINE_RSSI, topPadding, chartHeight)
    drawLine(
        color = baselineColor.copy(alpha = 0.45f),
        start = Offset(leftPadding, y),
        end = Offset(leftPadding + chartWidth, y),
        strokeWidth = 2.dp.toPx()
    )
}

private fun mapRssiToY(
    rssi: Float,
    topPadding: Float,
    chartHeight: Float
): Float {
    val clamped = rssi.coerceIn(CHART_MIN_RSSI, CHART_MAX_RSSI)
    val range = CHART_MAX_RSSI - CHART_MIN_RSSI
    val normalized = (clamped - CHART_MIN_RSSI) / range
    return topPadding + chartHeight - (normalized * chartHeight)
}

private fun Color.toArgb(): Int {
    return android.graphics.Color.argb(
        (alpha * 255).toInt(),
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt()
    )
}
