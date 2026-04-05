package com.example.wifiinsight.presentation.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NetworkWifi
import androidx.compose.material.icons.filled.NetworkWifi1Bar
import androidx.compose.material.icons.filled.NetworkWifi2Bar
import androidx.compose.material.icons.filled.NetworkWifi3Bar
import androidx.compose.material.icons.filled.SignalCellular0Bar
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SignalCellularConnectedNoInternet0Bar
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.wifiinsight.data.model.SignalLevel
import com.example.wifiinsight.domain.util.SignalCalculator

@Composable
fun SignalIndicator(
    rssi: Int,
    modifier: Modifier = Modifier,
    showBackground: Boolean = true,
    isWifi: Boolean = true
) {
    val signalLevel = SignalCalculator.rssiToSignalLevel(rssi)
    val percentage = SignalCalculator.rssiToPercentage(rssi)
    val color = getSignalColor(signalLevel)

    val icon = when {
        isWifi -> {
            when (signalLevel) {
                SignalLevel.EXCELLENT -> Icons.Default.NetworkWifi
                SignalLevel.GOOD -> Icons.Default.NetworkWifi3Bar
                SignalLevel.FAIR -> Icons.Default.NetworkWifi2Bar
                SignalLevel.WEAK -> Icons.Default.NetworkWifi1Bar
                SignalLevel.POOR -> Icons.Default.WifiOff
                SignalLevel.DEAD -> Icons.Default.WifiOff
            }
        }
        else -> {
            when (signalLevel) {
                SignalLevel.EXCELLENT -> Icons.Default.SignalCellularAlt
                SignalLevel.GOOD -> Icons.Default.SignalCellularAlt
                SignalLevel.FAIR -> Icons.Default.SignalCellularAlt
                SignalLevel.WEAK -> Icons.Default.SignalCellularConnectedNoInternet0Bar
                SignalLevel.POOR -> Icons.Default.SignalCellular0Bar
                SignalLevel.DEAD -> Icons.Default.SignalCellular0Bar
            }
        }
    }

    Box(
        modifier = modifier
            .then(
                if (showBackground) {
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f))
                } else {
                    Modifier
                }
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Signal strength: ${signalLevel.label} ($percentage%)",
            tint = color,
            modifier = Modifier.size(if (showBackground) 28.dp else 24.dp)
        )
    }
}

@Composable
fun getSignalColor(signalLevel: SignalLevel): Color {
    return when (signalLevel) {
        SignalLevel.EXCELLENT -> Color(0xFF4CAF50)
        SignalLevel.GOOD -> Color(0xFF8BC34A)
        SignalLevel.FAIR -> Color(0xFFFFC107)
        SignalLevel.WEAK -> Color(0xFFFF9800)
        SignalLevel.POOR -> Color(0xFFF44336)
        SignalLevel.DEAD -> Color(0xFF9E9E9E)
    }
}

@Composable
fun SignalStrengthBar(
    rssi: Int,
    modifier: Modifier = Modifier
) {
    val percentage = SignalCalculator.rssiToPercentage(rssi)
    val signalLevel = SignalCalculator.rssiToSignalLevel(rssi)
    val color = getSignalColor(signalLevel)

    Box(
        modifier = modifier
            .size(width = 60.dp, height = 8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(percentage / 100f)
                .background(color)
        )
    }
}
