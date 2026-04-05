package com.example.wifiinsight.presentation.screens.scan.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.wifiinsight.data.model.WifiNetwork
import com.example.wifiinsight.domain.util.SignalCalculator
import com.example.wifiinsight.ui.theme.SignalExcellent
import com.example.wifiinsight.ui.theme.SignalFair
import com.example.wifiinsight.ui.theme.SignalGood
import com.example.wifiinsight.ui.theme.SignalPoor
import com.example.wifiinsight.ui.theme.SignalWeak
import com.example.wifiinsight.ui.theme.getSecurityColor

/**
 * Card premium para mostrar una red WiFi en la lista.
 * Diseño tipo material con elevación, ripple y chip de seguridad.
 */
@Composable
fun NetworkCard(
    network: WifiNetwork,
    isConnected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val signalPercentage = remember(network.rssi) {
        SignalCalculator.rssiToPercentage(network.rssi)
    }
    val signalColor = getSignalColor(signalPercentage)
    val securityColor = remember(network.securityType) {
        getSecurityColor(network.securityType.name)
    }
    val highlightColor = if (isConnected) Color(0xFF2E7D32) else Color.Transparent
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .border(
                width = if (isConnected) 2.dp else 0.dp,
                color = highlightColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (signalPercentage >= 60) 4.dp else 2.dp,
            pressedElevation = 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono de señal con color según intensidad
            SignalIcon(
                percentage = signalPercentage,
                color = signalColor,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Info de la red
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // SSID
                Text(
                    text = network.safeSsid,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Chips row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isConnected) {
                        Text(
                            text = "Conectada",
                            style = MaterialTheme.typography.labelMedium,
                            color = highlightColor,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Security Chip
                    SecurityChip(
                        securityType = network.securityType.name,
                        color = securityColor
                    )
                    
                    // Signal text
                    Text(
                        text = "${network.rssi} dBm",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Chevron
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun SignalIcon(
    percentage: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.SignalCellularAlt,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun SecurityChip(
    securityType: String,
    color: Color
) {
    val (icon, label) = when (securityType.uppercase()) {
        "WPA3" -> Icons.Default.Lock to "WPA3"
        "WPA2" -> Icons.Default.Lock to "WPA2"
        "WPA" -> Icons.Default.Lock to "WPA"
        "WEP" -> Icons.Default.Lock to "WEP"
        else -> Icons.Default.LockOpen to "Abierta"
    }
    
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun getSignalColor(percentage: Int): Color {
    return when {
        percentage >= 80 -> SignalExcellent
        percentage >= 60 -> SignalGood
        percentage >= 40 -> SignalFair
        percentage >= 20 -> SignalWeak
        else -> SignalPoor
    }
}
