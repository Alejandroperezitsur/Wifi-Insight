package com.example.wifiinsight.presentation.common.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Estados globales de la app para mostrar en la UI
 */
sealed class GlobalStatus {
    data object Scanning : GlobalStatus()
    data object Connected : GlobalStatus()
    data object Disconnected : GlobalStatus()
    data object WifiDisabled : GlobalStatus()
    data object PermissionDenied : GlobalStatus()
    data object Error : GlobalStatus()
    data object Loading : GlobalStatus()
    data object Timeout : GlobalStatus()
}

/**
 * Barra de estado global siempre visible que indica el estado actual de la app.
 * Nunca deja al usuario adivinando qué está pasando.
 */
@Composable
fun GlobalStatusBar(
    status: GlobalStatus,
    message: String? = null,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = when (status) {
            is GlobalStatus.Scanning -> MaterialTheme.colorScheme.primaryContainer
            is GlobalStatus.Connected -> Color(0xFFE8F5E9)
            is GlobalStatus.Disconnected -> MaterialTheme.colorScheme.surfaceVariant
            is GlobalStatus.WifiDisabled -> MaterialTheme.colorScheme.errorContainer
            is GlobalStatus.PermissionDenied -> MaterialTheme.colorScheme.errorContainer
            is GlobalStatus.Error -> MaterialTheme.colorScheme.errorContainer
            is GlobalStatus.Loading -> MaterialTheme.colorScheme.surfaceVariant
            is GlobalStatus.Timeout -> MaterialTheme.colorScheme.tertiaryContainer
        },
        label = "status_background"
    )

    val contentColor by animateColorAsState(
        targetValue = when (status) {
            is GlobalStatus.Scanning -> MaterialTheme.colorScheme.onPrimaryContainer
            is GlobalStatus.Connected -> Color(0xFF2E7D32)
            is GlobalStatus.Disconnected -> MaterialTheme.colorScheme.onSurfaceVariant
            is GlobalStatus.WifiDisabled -> MaterialTheme.colorScheme.onErrorContainer
            is GlobalStatus.PermissionDenied -> MaterialTheme.colorScheme.onErrorContainer
            is GlobalStatus.Error -> MaterialTheme.colorScheme.onErrorContainer
            is GlobalStatus.Loading -> MaterialTheme.colorScheme.onSurfaceVariant
            is GlobalStatus.Timeout -> MaterialTheme.colorScheme.onTertiaryContainer
        },
        label = "status_content"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Icon/Indicator
            when (status) {
                is GlobalStatus.Scanning -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = contentColor,
                        strokeWidth = 2.dp
                    )
                }
                is GlobalStatus.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = contentColor,
                        strokeWidth = 2.dp
                    )
                }
                else -> {
                    val icon = when (status) {
                        is GlobalStatus.Connected -> Icons.Default.CheckCircle
                        is GlobalStatus.Disconnected -> Icons.Default.Wifi
                        is GlobalStatus.WifiDisabled -> Icons.Default.SignalWifiOff
                        is GlobalStatus.PermissionDenied -> Icons.Default.Error
                        is GlobalStatus.Error -> Icons.Default.Error
                        is GlobalStatus.Timeout -> Icons.Default.Warning
                        else -> Icons.Default.Refresh
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Status Text
            Column(modifier = Modifier.weight(1f)) {
                val statusText = when (status) {
                    is GlobalStatus.Scanning -> "Buscando redes..."
                    is GlobalStatus.Connected -> "Conectado"
                    is GlobalStatus.Disconnected -> "Sin conexión"
                    is GlobalStatus.WifiDisabled -> "WiFi desactivado"
                    is GlobalStatus.PermissionDenied -> "Permisos requeridos"
                    is GlobalStatus.Error -> "Error"
                    is GlobalStatus.Loading -> "Cargando..."
                    is GlobalStatus.Timeout -> "Esto está tardando..."
                }

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor
                )

                message?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.8f)
                    )
                }
            }

            // Retry button for timeout/error states
            if ((status is GlobalStatus.Timeout || status is GlobalStatus.Error) && onRetry != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(contentColor.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Reintentar",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = contentColor
                    )
                }
            }
        }
    }
}

/**
 * Mini indicador de estado para mostrar en esquinas o headers
 */
@Composable
fun StatusIndicatorDot(
    status: GlobalStatus,
    modifier: Modifier = Modifier
) {
    val color = when (status) {
        is GlobalStatus.Scanning -> MaterialTheme.colorScheme.primary
        is GlobalStatus.Connected -> Color(0xFF4CAF50)
        is GlobalStatus.Disconnected -> MaterialTheme.colorScheme.outline
        is GlobalStatus.WifiDisabled -> MaterialTheme.colorScheme.error
        is GlobalStatus.PermissionDenied -> MaterialTheme.colorScheme.error
        is GlobalStatus.Error -> MaterialTheme.colorScheme.error
        is GlobalStatus.Loading -> MaterialTheme.colorScheme.primary
        is GlobalStatus.Timeout -> MaterialTheme.colorScheme.tertiary
    }

    Box(
        modifier = modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color)
    )
}
