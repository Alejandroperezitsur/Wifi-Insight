package com.example.wifiinsight.presentation.screens.home.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.unit.dp
import com.example.wifiinsight.data.model.ConnectionState
import com.example.wifiinsight.data.model.InternetStatus

/**
 * Componente que muestra el estado REAL de conexión incluyendo:
 * - Estado de conexión WiFi
 * - Estado de internet (validado vs no validado vs sin internet)
 * - Mensajes descriptivos para el usuario
 */
@Composable
fun ConnectionStatus(
    connectionState: ConnectionState,
    modifier: Modifier = Modifier,
    showDetailedStatus: Boolean = true
) {
    val (statusColor, statusText, icon) = when (connectionState) {
        is ConnectionState.Connected -> {
            when (connectionState.internetStatus) {
                InternetStatus.VALIDATED -> 
                    Triple(Color(0xFF4CAF50), "Internet OK", Icons.Default.CheckCircle)
                InternetStatus.UNVALIDATED -> 
                    Triple(Color(0xFFFF9800), "Conectado (sin validar)", Icons.Default.Warning)
                InternetStatus.NONE -> 
                    Triple(Color(0xFF9E9E9E), "Conectado (sin internet)", Icons.Default.Error)
            }
        }
        is ConnectionState.Connecting -> 
            Triple(Color(0xFFFFC107), "Conectando...", null)
        is ConnectionState.Scanning -> 
            Triple(Color(0xFF2196F3), "Escaneando...", null)
        is ConnectionState.Error -> 
            Triple(Color(0xFFF44336), "Error", Icons.Default.Error)
        ConnectionState.Disconnected -> 
            Triple(Color(0xFF9E9E9E), "Desconectado", null)
    }

    val animatedColor by animateColorAsState(
        targetValue = statusColor,
        label = "statusColor"
    )

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = animatedColor.copy(alpha = 0.15f),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Indicador visual
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(animatedColor)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Icono de estado si existe
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = animatedColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            
            // Texto de estado
            AnimatedContent(
                targetState = statusText,
                label = "statusText"
            ) { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelMedium,
                    color = animatedColor
                )
            }
        }
    }
}

/**
 * Muestra un indicador detallado del estado de internet.
 * Útil para diagnosticar problemas de conectividad.
 */
@Composable
fun InternetStatusIndicator(
    status: InternetStatus,
    modifier: Modifier = Modifier
) {
    val (color, text, description) = when (status) {
        InternetStatus.VALIDATED -> Triple(
            Color(0xFF4CAF50),
            "Internet Validado",
            "Conexión a internet verificada y funcionando correctamente."
        )
        InternetStatus.UNVALIDATED -> Triple(
            Color(0xFFFF9800),
            "Internet No Validado",
            "Conectado pero sin acceso confirmado a internet. Posible portal cautivo."
        )
        InternetStatus.NONE -> Triple(
            Color(0xFFF44336),
            "Sin Internet",
            "Conectado a la red WiFi pero sin acceso a internet."
        )
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.1f),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = when (status) {
                    InternetStatus.VALIDATED -> Icons.Default.CheckCircle
                    InternetStatus.UNVALIDATED -> Icons.Default.Warning
                    InternetStatus.NONE -> Icons.Default.Error
                },
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    color = color,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
