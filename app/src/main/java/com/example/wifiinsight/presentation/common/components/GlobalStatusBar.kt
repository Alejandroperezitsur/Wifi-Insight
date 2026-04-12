package com.example.wifiinsight.presentation.common.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalWifiConnectedNoInternet4
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Estados globales de la aplicación.
 */
sealed class GlobalStatus {
    data object Scanning : GlobalStatus()
    data object Connected : GlobalStatus()
    data object Disconnected : GlobalStatus()
    data object WifiDisabled : GlobalStatus()
    data object PermissionDenied : GlobalStatus()
    data class Error(val message: String) : GlobalStatus()
    data object Loading : GlobalStatus()
    data object Timeout : GlobalStatus()
}

/**
 * Barra de estado global siempre visible que indica el estado actual de la app.
 */
@Composable
fun GlobalStatusBar(
    status: GlobalStatus,
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = when (status) {
            is GlobalStatus.Connected -> MaterialTheme.colorScheme.primaryContainer
            is GlobalStatus.Scanning -> MaterialTheme.colorScheme.tertiaryContainer
            is GlobalStatus.Loading -> MaterialTheme.colorScheme.secondaryContainer
            is GlobalStatus.Error, GlobalStatus.PermissionDenied -> MaterialTheme.colorScheme.errorContainer
            is GlobalStatus.Timeout -> MaterialTheme.colorScheme.errorContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        label = "status-background"
    )

    val contentColor by animateColorAsState(
        targetValue = when (status) {
            is GlobalStatus.Connected -> MaterialTheme.colorScheme.onPrimaryContainer
            is GlobalStatus.Scanning -> MaterialTheme.colorScheme.onTertiaryContainer
            is GlobalStatus.Loading -> MaterialTheme.colorScheme.onSecondaryContainer
            is GlobalStatus.Error, GlobalStatus.PermissionDenied -> MaterialTheme.colorScheme.onErrorContainer
            is GlobalStatus.Timeout -> MaterialTheme.colorScheme.onErrorContainer
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "status-content"
    )

    Crossfade(targetState = status, label = "global-status") { currentStatus ->
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(backgroundColor)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icono de estado
                StatusIcon(status = currentStatus, tint = contentColor)
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Texto descriptivo
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = getStatusTitle(currentStatus),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor
                    )
                    
                    val subtitle = getStatusSubtitle(currentStatus)
                    if (subtitle.isNotEmpty()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = contentColor.copy(alpha = 0.7f)
                        )
                    }
                }
                
                // Botón de acción si aplica
                if (shouldShowRetry(currentStatus)) {
                    TextButton(
                        onClick = onRetry,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = contentColor
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reintentar")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusIcon(status: GlobalStatus, tint: androidx.compose.ui.graphics.Color) {
    val icon = when (status) {
        is GlobalStatus.Connected -> Icons.Default.CheckCircle
        is GlobalStatus.Scanning -> Icons.Default.Wifi
        is GlobalStatus.Loading -> Icons.Default.HourglassEmpty
        is GlobalStatus.Error, GlobalStatus.PermissionDenied -> Icons.Default.Error
        is GlobalStatus.Timeout -> Icons.Default.HourglassEmpty
        is GlobalStatus.Disconnected -> Icons.Default.SignalWifiConnectedNoInternet4
        is GlobalStatus.WifiDisabled -> Icons.Default.WifiOff
    }
    
    val backgroundColor = when (status) {
        is GlobalStatus.Connected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        is GlobalStatus.Scanning -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
        is GlobalStatus.Loading -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
        is GlobalStatus.Error, GlobalStatus.PermissionDenied -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
        else -> tint.copy(alpha = 0.1f)
    }
    
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

private fun getStatusTitle(status: GlobalStatus): String {
    return when (status) {
        is GlobalStatus.Scanning -> "Escaneando redes..."
        is GlobalStatus.Connected -> "Conectado a WiFi"
        is GlobalStatus.Disconnected -> "Sin conexión WiFi"
        is GlobalStatus.WifiDisabled -> "WiFi desactivado"
        is GlobalStatus.PermissionDenied -> "Permisos requeridos"
        is GlobalStatus.Loading -> "Cargando..."
        is GlobalStatus.Timeout -> "Tiempo agotado"
        is GlobalStatus.Error -> "Error"
    }
}

private fun getStatusSubtitle(status: GlobalStatus): String {
    return when (status) {
        is GlobalStatus.Scanning -> "Buscando puntos de acceso cercanos"
        is GlobalStatus.Connected -> "Conexión activa y funcionando"
        is GlobalStatus.Disconnected -> "No hay red WiFi activa"
        is GlobalStatus.WifiDisabled -> "Activa WiFi para comenzar"
        is GlobalStatus.PermissionDenied -> "Se necesitan permisos para escanear"
        is GlobalStatus.Loading -> "Por favor espera un momento"
        is GlobalStatus.Timeout -> "La operación está tardando demasiado"
        is GlobalStatus.Error -> if (status.message.isNotEmpty()) status.message else "Algo salió mal"
    }
}

private fun shouldShowRetry(status: GlobalStatus): Boolean {
    return status is GlobalStatus.Error || 
           status is GlobalStatus.Timeout ||
           status is GlobalStatus.Disconnected
}
