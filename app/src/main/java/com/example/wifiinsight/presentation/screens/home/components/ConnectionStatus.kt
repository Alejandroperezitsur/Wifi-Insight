package com.example.wifiinsight.presentation.screens.home.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

@Composable
fun ConnectionStatus(
    connectionState: ConnectionState,
    modifier: Modifier = Modifier
) {
    val (statusColor, statusText) = when (connectionState) {
        is ConnectionState.Connected -> Color(0xFF4CAF50) to "Conectado"
        is ConnectionState.Connecting -> Color(0xFFFFC107) to "Conectando..."
        is ConnectionState.Scanning -> Color(0xFF2196F3) to "Escaneando..."
        is ConnectionState.Error -> Color(0xFFF44336) to "Error"
        ConnectionState.Disconnected -> Color(0xFF9E9E9E) to "Desconectado"
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
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(animatedColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
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
