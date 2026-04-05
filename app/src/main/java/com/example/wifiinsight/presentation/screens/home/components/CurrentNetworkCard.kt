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
import androidx.compose.ui.unit.dp
import com.example.wifiinsight.data.model.ConnectionState
import com.example.wifiinsight.data.model.InternetStatus
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
    val statusMessage = when (connectionState.internetStatus) {
        InternetStatus.AVAILABLE -> "Internet funcionando"
        InternetStatus.CHECKING -> "Verificando conexión..."
        InternetStatus.UNAVAILABLE -> "Sin internet (posible portal cautivo)"
        InternetStatus.UNKNOWN -> "Verificando conexión..."
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
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
                        text = connectionState.safeSsid,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    StatusLabel(
                        color = signalColor,
                        text = statusMessage
                    )
                }

                Text(
                    text = "${connectionState.getSafeSignalPercentage()}%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = signalColor
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            DetailRow(
                label = "BSSID",
                value = connectionState.safeBssid
            )
            Spacer(modifier = Modifier.height(8.dp))
            DetailRow(
                label = "IP",
                value = connectionState.getSafeIpAddress()
            )
            Spacer(modifier = Modifier.height(8.dp))
            DetailRow(
                label = "Velocidad",
                value = connectionState.getSafeLinkSpeed()
            )
            Spacer(modifier = Modifier.height(8.dp))
            DetailRow(
                label = "Señal",
                value = "${connectionState.rssi ?: "—"} dBm"
            )
        }
    }
}

@Composable
private fun DisconnectedNetworkCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
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
