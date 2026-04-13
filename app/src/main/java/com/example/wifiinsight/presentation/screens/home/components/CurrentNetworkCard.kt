package com.example.wifiinsight.presentation.screens.home.components

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.wifiinsight.data.model.ConnectionQuality
import com.example.wifiinsight.data.model.ConnectionState
import com.example.wifiinsight.domain.util.SignalCalculator
import com.example.wifiinsight.presentation.common.components.SignalIndicator
import com.example.wifiinsight.presentation.common.components.getSignalColor

@Composable
fun CurrentNetworkCard(
    connectionState: ConnectionState,
    modifier: Modifier = Modifier
) {
    when (connectionState) {
        is ConnectionState.Connected -> ConnectedNetworkCard(
            connectionState = connectionState,
            modifier = modifier
        )

        else -> DisconnectedNetworkCard(modifier = modifier)
    }
}

@Composable
private fun ConnectedNetworkCard(
    connectionState: ConnectionState.Connected,
    modifier: Modifier = Modifier
) {
    val rssi = connectionState.rssi ?: -100
    val signalLevel = SignalCalculator.rssiToSignalLevel(rssi)
    val signalColor = getSignalColor(signalLevel)
    val statusMessage = connectionStatusMessage(connectionState)
    val statusColor = connectionStatusColor(connectionState.connectionQuality, signalColor)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SignalIndicator(
                    rssi = rssi,
                    modifier = Modifier.size(56.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "CONECTADO A",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                    Text(
                        text = connectionState.safeSsid,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    StatusLabel(
                        color = MaterialTheme.colorScheme.primary,
                        text = statusMessage
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${connectionState.getSafeSignalPercentage()}%",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "POTENCIA",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                    .padding(12.dp)
            ) {
                DetailRow(
                    label = "BSSID",
                    value = connectionState.safeBssid
                )
                Spacer(modifier = Modifier.height(8.dp))
                DetailRow(
                    label = "IP Address",
                    value = connectionState.getSafeIpAddress()
                )
                Spacer(modifier = Modifier.height(8.dp))
                DetailRow(
                    label = "Link Speed",
                    value = connectionState.getSafeLinkSpeed()
                )
                Spacer(modifier = Modifier.height(8.dp))
                DetailRow(
                    label = "Status",
                    value = signalConfidenceMessage(connectionState.rssi)
                )
            }
        }
    }
}

private fun connectionStatusMessage(connectionState: ConnectionState.Connected): String {
    return when (connectionState.connectionQuality) {
        ConnectionQuality.CONNECTING -> "Conectando..."
        ConnectionQuality.CONNECTED_NO_INTERNET -> "Conectado, pero esta red no tiene acceso a internet"
        ConnectionQuality.CONNECTED_INTERNET -> "Conectado con internet"
        ConnectionQuality.DISCONNECTED -> "Conectado"
    }
}

private fun signalConfidenceMessage(rssi: Int?): String {
    return when {
        rssi == null -> "Señal no disponible todavía"
        rssi >= -50 -> "Excelente (estable para videollamadas)"
        rssi >= -60 -> "Buena (estable para navegación)"
        rssi >= -70 -> "Regular (puede variar)"
        else -> "Mala (puede fallar la conexión)"
    }
}

@Composable
private fun connectionStatusColor(
    quality: ConnectionQuality,
    fallback: Color
): Color {
    return when (quality) {
        ConnectionQuality.CONNECTING -> MaterialTheme.colorScheme.secondary
        ConnectionQuality.CONNECTED_NO_INTERNET -> MaterialTheme.colorScheme.error
        ConnectionQuality.CONNECTED_INTERNET -> MaterialTheme.colorScheme.primary
        ConnectionQuality.DISCONNECTED -> fallback
    }
}

@Composable
private fun DisconnectedNetworkCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Sin conexión WiFi",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Conéctate a una red para ver métricas reales de señal e internet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatusLabel(
    color: Color,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = color
        )
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
