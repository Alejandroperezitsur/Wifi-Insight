package com.example.wifiinsight.presentation.common.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.wifiinsight.data.model.ConnectionState
import com.example.wifiinsight.domain.util.SignalCalculator

/**
 * Card narrativa que convierte métricas en historia.
 * "Tu conexión es excelente para streaming", "Se detecta inestabilidad", etc.
 */
@Composable
fun StoryCard(
    connectionState: ConnectionState,
    signalHistory: List<Int>,
    modifier: Modifier = Modifier
) {
    val story = generateStory(connectionState, signalHistory)

    AnimatedVisibility(
        visible = story != null,
        enter = slideInVertically(
            initialOffsetY = { it / 2 }
        ) + fadeIn(
            animationSpec = tween(400, easing = FastOutSlowInEasing)
        ),
        exit = shrinkVertically() + fadeOut()
    ) {
        story?.let { (type, title, message, color, icon) ->
            Card(
                modifier = modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = color.copy(alpha = 0.1f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icono grande con fondo
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(color.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = color
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Texto
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Genera la historia basada en el estado de conexión y señal
 */
private fun generateStory(
    connectionState: ConnectionState,
    signalHistory: List<Int>
): StoryData? {
    return when (connectionState) {
        is ConnectionState.Connected -> {
            val rssi = connectionState.rssi ?: -100
            val percentage = SignalCalculator.rssiToPercentage(rssi)
            val isStable = signalHistory.size >= 3 && 
                signalHistory.takeLast(3).let { last3 ->
                    val max = last3.maxOrNull()
                    val min = last3.minOrNull()
                    max != null && min != null && (max - min) < 10
                }
            val hasInternet = connectionState.hasInternet
            val isValidated = connectionState.isValidated

            when {
                !hasInternet -> StoryData(
                    type = StoryType.WARNING,
                    title = "Sin acceso a internet",
                    message = "Estás conectado a la red pero sin acceso a internet. Verifica tu conexión.",
                    color = Color(0xFFFF9100),
                    icon = Icons.Default.WifiOff
                )
                !isValidated -> StoryData(
                    type = StoryType.INFO,
                    title = "Conexión no validada",
                    message = "Posible portal cautivo. Abre el navegador para completar la conexión.",
                    color = Color(0xFFFFB300),
                    icon = Icons.Default.Info
                )
                percentage >= 80 && isStable -> StoryData(
                    type = StoryType.EXCELLENT,
                    title = "Ideal para streaming",
                    message = "Tu conexión es excelente. Perfecta para video 4K, gaming y videollamadas.",
                    color = Color(0xFF00C853),
                    icon = Icons.Default.PlayArrow
                )
                percentage >= 60 && isStable -> StoryData(
                    type = StoryType.GOOD,
                    title = "Buena conexión",
                    message = "Navegación fluida y streaming HD sin problemas.",
                    color = Color(0xFF64DD17),
                    icon = Icons.Default.CheckCircle
                )
                percentage >= 40 -> StoryData(
                    type = StoryType.FAIR,
                    title = "Conexión aceptable",
                    message = "Suficiente para navegación básica. Considera acercarte al router.",
                    color = Color(0xFFFFB300),
                    icon = Icons.Default.Info
                )
                !isStable -> StoryData(
                    type = StoryType.WARNING,
                    title = "Señal inestable",
                    message = "Se detecta variación en la señal. Evita descargas grandes por ahora.",
                    color = Color(0xFFFF9100),
                    icon = Icons.Default.TrendingDown
                )
                else -> StoryData(
                    type = StoryType.POOR,
                    title = "Señal débil",
                    message = "Conexión lenta. Acércate al router o verifica interferencias.",
                    color = Color(0xFFFF1744),
                    icon = Icons.Default.Error
                )
            }
        }
        else -> null
    }
}

private data class StoryData(
    val type: StoryType,
    val title: String,
    val message: String,
    val color: Color,
    val icon: ImageVector
)

private enum class StoryType {
    EXCELLENT, GOOD, FAIR, WARNING, POOR, INFO
}

/**
 * Badge de recomendación rápida
 */
@Composable
fun RecommendationBadge(
    text: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.15f),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Lista de recomendaciones basadas en el estado
 */
@Composable
fun RecommendationsList(
    connectionState: ConnectionState,
    modifier: Modifier = Modifier
) {
    val recommendations = getRecommendations(connectionState)

    if (recommendations.isNotEmpty()) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            recommendations.forEach { (text, icon, color) ->
                RecommendationBadge(
                    text = text,
                    icon = icon,
                    color = color
                )
            }
        }
    }
}

private fun getRecommendations(connectionState: ConnectionState): List<Triple<String, ImageVector, Color>> {
    return when (connectionState) {
        is ConnectionState.Connected -> {
            val rssi = connectionState.rssi ?: -100
            val percentage = SignalCalculator.rssiToPercentage(rssi)
            val list = mutableListOf<Triple<String, ImageVector, Color>>()

            when {
                percentage >= 80 -> {
                    list.add(Triple("Streaming 4K", Icons.Default.PlayArrow, Color(0xFF00C853)))
                    list.add(Triple("Gaming online", Icons.Default.CheckCircle, Color(0xFF64DD17)))
                }
                percentage >= 60 -> {
                    list.add(Triple("Streaming HD", Icons.Default.PlayArrow, Color(0xFF64DD17)))
                    list.add(Triple("Videollamadas", Icons.Default.CheckCircle, Color(0xFF64DD17)))
                }
                percentage >= 40 -> {
                    list.add(Triple("Navegación web", Icons.Default.Info, Color(0xFFFFB300)))
                    list.add(Triple("Redes sociales", Icons.Default.CheckCircle, Color(0xFFFFB300)))
                }
                else -> {
                    list.add(Triple("Solo mensajes", Icons.Default.Warning, Color(0xFFFF9100)))
                    list.add(Triple("Acércate al router", Icons.Default.TrendingDown, Color(0xFFFF1744)))
                }
            }

            if (!connectionState.hasInternet) {
                list.add(Triple("Sin internet", Icons.Default.WifiOff, Color(0xFFFF1744)))
            }

            list
        }
        else -> emptyList()
    }
}
