package com.example.wifiinsight.presentation.screens.home.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NetworkWifi
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.wifiinsight.data.model.ConnectionState
import com.example.wifiinsight.domain.util.SignalCalculator
import com.example.wifiinsight.presentation.common.components.SignalIndicator
import com.example.wifiinsight.presentation.common.components.getSignalQuality
import com.example.wifiinsight.ui.theme.StatusConnected
import com.example.wifiinsight.ui.theme.StatusDisconnected
import com.example.wifiinsight.ui.theme.getSignalColor
import com.example.wifiinsight.ui.theme.getSignalGradient

/**
 * Hero Card premium con experiencia viva.
 * Fondo dinámico animado, glow pulsante, y narrativa visual.
 */
@Composable
fun CurrentNetworkCard(
    connectionState: ConnectionState,
    signalHistory: List<Int>,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hero_pulse")

    // Pulso sutil del card completo
    val cardScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "card_pulse"
    )

    val result = when (connectionState) {
        is ConnectionState.Connected -> {
            val rssi = connectionState.rssi ?: -100
            val percentage = SignalCalculator.rssiToPercentage(rssi)
            val quality = getSignalQuality(percentage)

            when (quality) {
                com.example.wifiinsight.presentation.common.components.SignalQuality.EXCELLENT ->
                    Triple(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF00639A).copy(alpha = 0.2f),
                                Color(0xFF00B4D8).copy(alpha = 0.1f),
                                Color(0xFF5E60CE).copy(alpha = 0.05f)
                            )
                        ),
                        StatusConnected,
                        Color(0xFF00C853)
                    )
                com.example.wifiinsight.presentation.common.components.SignalQuality.GOOD ->
                    Triple(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF00C853).copy(alpha = 0.15f),
                                Color(0xFF64DD17).copy(alpha = 0.08f)
                            )
                        ),
                        StatusConnected,
                        Color(0xFF64DD17)
                    )
                com.example.wifiinsight.presentation.common.components.SignalQuality.FAIR ->
                    Triple(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFFB300).copy(alpha = 0.15f),
                                Color(0xFFFF9100).copy(alpha = 0.08f)
                            )
                        ),
                        Color(0xFFFFB300),
                        Color(0xFFFFB300)
                    )
                com.example.wifiinsight.presentation.common.components.SignalQuality.POOR ->
                    Triple(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFF1744).copy(alpha = 0.15f),
                                Color(0xFFD50000).copy(alpha = 0.08f)
                            )
                        ),
                        Color(0xFFFF1744),
                        Color(0xFFFF1744)
                    )
            }
        }
        else -> Triple(
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF9E9E9E).copy(alpha = 0.15f),
                    Color(0xFFBDBDBD).copy(alpha = 0.05f)
                )
            ),
            StatusDisconnected,
            Color(0xFF9E9E9E)
        )
    }
    
    val backgroundBrush = result.first
    val statusColor = result.second
    val glowColor = result.third

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 12.dp,
            pressedElevation = 8.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundBrush)
                .drawBehind {
                    drawCircle(
                        color = glowColor.copy(alpha = 0.1f),
                        radius = size.width * 0.8f,
                        center = center
                    )
                }
                .padding(28.dp)
        ) {
            when (connectionState) {
                is ConnectionState.Connected -> {
                    ConnectedHeroContent(
                        ssid = connectionState.ssid,
                        ipAddress = connectionState.ipAddress,
                        linkSpeed = connectionState.linkSpeed,
                        rssi = connectionState.rssi,
                        statusColor = statusColor,
                        glowColor = glowColor
                    )
                }
                else -> {
                    DisconnectedHeroContent()
                }
            }
        }
    }
}

@Composable
private fun ConnectedHeroContent(
    ssid: String,
    ipAddress: String?,
    linkSpeed: Int?,
    rssi: Int?,
    statusColor: Color,
    glowColor: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "content_animations")

    // Glow pulsante para el icono
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "icon_glow"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "icon_glow_alpha"
    )

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier.size(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .graphicsLayer {
                            scaleX = glowScale
                            scaleY = glowScale
                            alpha = glowAlpha
                        }
                        .clip(RoundedCornerShape(20.dp))
                        .background(glowColor.copy(alpha = 0.3f))
                )
                
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(statusColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NetworkWifi,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(20.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = ssid,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                StatusBadgeConnected(color = statusColor, isPulsing = true)
            }

            rssi?.let { signal ->
                AnimatedSignalIndicator(
                    rssi = signal,
                    statusColor = statusColor
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        AnimatedMetricsRow(
            linkSpeed = linkSpeed,
            ipAddress = ipAddress
        )
    }
}

@Composable
private fun StatusBadgeConnected(
    color: Color,
    isPulsing: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "status_pulse")
    
    val dotScale by if (isPulsing) {
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.4f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "status_dot_pulse"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(1f) }
    }

    val dotAlpha by if (isPulsing) {
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 0.5f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "status_dot_alpha"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(1f) }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .graphicsLayer {
                    scaleX = dotScale
                    scaleY = dotScale
                    alpha = dotAlpha
                }
                .clip(RoundedCornerShape(5.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "Conectado",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

@Composable
private fun AnimatedSignalIndicator(
    rssi: Int,
    statusColor: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "signal_pulse")
    
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "signal_ring"
    )

    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "signal_ring_alpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(64.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .graphicsLayer {
                        scaleX = ringScale
                        scaleY = ringScale
                        alpha = ringAlpha
                    }
                    .clip(RoundedCornerShape(30.dp))
                    .background(statusColor.copy(alpha = 0.2f))
            )
            
            SignalIndicator(
                rssi = rssi,
                modifier = Modifier.size(56.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        val percentage = SignalCalculator.rssiToPercentage(rssi)
        Text(
            text = "$percentage%",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = statusColor
        )
    }
}

@Composable
private fun AnimatedMetricsRow(
    linkSpeed: Int?,
    ipAddress: String?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AnimatedMetricChip(
            icon = Icons.Default.Speed,
            label = "Velocidad",
            value = linkSpeed?.let { "$it Mbps" } ?: "—",
            delayMillis = 0,
            modifier = Modifier.weight(1f)
        )
        AnimatedMetricChip(
            icon = Icons.Default.NetworkWifi,
            label = "Dirección IP",
            value = ipAddress ?: "—",
            delayMillis = 100,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AnimatedMetricChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    delayMillis: Int,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "metric_pulse_$delayMillis")
    
    val elevation by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000 + delayMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "metric_elevation"
    )

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = elevation
                scaleY = elevation
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DisconnectedHeroContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(StatusDisconnected.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.WifiOff,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = StatusDisconnected
            )
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Text(
            text = "Sin conexión WiFi",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Conecta a una red para ver información detallada",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
