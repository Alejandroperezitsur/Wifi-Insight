package com.example.wifiinsight.presentation.common.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.wifiinsight.ui.theme.getSignalGradient
import androidx.compose.ui.unit.sp

/**
 * Tile de métrica para dashboard premium.
 * Muestra un valor grande con label e icono.
 */
@Composable
fun MetricTile(
    value: String,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    isAnimated: Boolean = false,
    targetPercentage: Int? = null
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icono con fondo
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Valor principal
            Text(
                text = value,
                style = if (isAnimated && targetPercentage != null) {
                    MaterialTheme.typography.displaySmall
                } else {
                    MaterialTheme.typography.headlineSmall
                },
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Label
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.5.sp
            )
            
            // Barra de progreso si es animado
            if (isAnimated && targetPercentage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                AnimatedProgressBar(
                    targetPercentage = targetPercentage,
                    color = color
                )
            }
        }
    }
}

/**
 * Barra de progreso animada con gradiente según valor
 */
@Composable
private fun AnimatedProgressBar(
    targetPercentage: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val animatedProgress = remember { mutableStateOf(0f) }
    
    LaunchedEffect(targetPercentage) {
        animatedProgress.value = targetPercentage / 100f
    }
    
    val progress by animateFloatAsState(
        targetValue = animatedProgress.value,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "progress"
    )
    
    val gradient = getSignalGradient(targetPercentage)
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = gradient,
                        startX = 0f,
                        endX = 100f
                    )
                )
        )
    }
}

/**
 * Grid de métricas para dashboard
 */
@Composable
fun MetricGrid(
    metrics: List<MetricData>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        metrics.take(2).forEach { metric ->
            MetricTile(
                value = metric.value,
                label = metric.label,
                icon = metric.icon,
                color = metric.color,
                isAnimated = metric.isAnimated,
                targetPercentage = metric.targetPercentage,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Data class para métricas
 */
data class MetricData(
    val value: String,
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val isAnimated: Boolean = false,
    val targetPercentage: Int? = null
)
