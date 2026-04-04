package com.example.wifiinsight.presentation.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.wifiinsight.data.model.SecurityType

@Composable
fun SecurityBadge(
    securityType: SecurityType,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (securityType) {
        SecurityType.OPEN -> MaterialTheme.colorScheme.errorContainer
        SecurityType.WEP -> MaterialTheme.colorScheme.errorContainer
        SecurityType.WPA -> MaterialTheme.colorScheme.tertiaryContainer
        SecurityType.WPA2 -> MaterialTheme.colorScheme.secondaryContainer
        SecurityType.WPA3 -> MaterialTheme.colorScheme.primaryContainer
    }

    val contentColor = when (securityType) {
        SecurityType.OPEN -> MaterialTheme.colorScheme.error
        SecurityType.WEP -> MaterialTheme.colorScheme.error
        SecurityType.WPA -> MaterialTheme.colorScheme.onTertiaryContainer
        SecurityType.WPA2 -> MaterialTheme.colorScheme.onSecondaryContainer
        SecurityType.WPA3 -> MaterialTheme.colorScheme.onPrimaryContainer
    }

    val icon = when (securityType) {
        SecurityType.OPEN -> Icons.Default.LockOpen
        else -> Icons.Default.Lock
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        androidx.compose.foundation.layout.Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = securityType.displayName(),
                style = MaterialTheme.typography.labelSmall,
                color = contentColor
            )
        }
    }
}

@Composable
fun SecurityIndicator(
    isSecure: Boolean,
    modifier: Modifier = Modifier
) {
    val color = if (isSecure) {
        Color(0xFF4CAF50)
    } else {
        Color(0xFFF44336)
    }

    Box(
        modifier = modifier
            .size(8.dp)
            .background(color, RoundedCornerShape(50))
    )
}
