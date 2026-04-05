package com.example.wifiinsight.presentation.common.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.wifiinsight.data.model.InternetStatus

/**
 * Badge de estado con indicador pulsante para mostrar estado de conexión.
 * Diseño premium con animación sutil.
 */
@Composable
fun StatusBadge(
    status: InternetStatus,
    modifier: Modifier = Modifier,
    showPulse: Boolean = true
) {
    val (backgroundColor, contentColor, icon, text) = when (status) {
        InternetStatus.VALIDATED -> listOf(
            Color(0xFF00C853).copy(alpha = 0.15f),
            Color(0xFF00C853),
            Icons.Default.CheckCircle,
            "Internet OK"
        )
        InternetStatus.UNVALIDATED -> listOf(
            Color(0xFFFFB300).copy(alpha = 0.15f),
            Color(0xFFFFB300),
            Icons.Default.Warning,
            "Sin validar"
        )
        InternetStatus.NONE -> listOf(
            Color(0xFFFF1744).copy(alpha = 0.15f),
            Color(0xFFFF1744),
            Icons.Default.Error,
            "Sin internet"
        )
    }.let { 
        StatusColors(
            background = it[0] as Color,
            content = it[1] as Color,
            icon = it[2] as ImageVector,
            text = it[3] as String
        )
    }

    val animatedBackground by animateColorAsState(
        targetValue = backgroundColor,
        label = "badge_background"
    )
    
    val animatedContent by animateColorAsState(
        targetValue = contentColor,
        label = "badge_content"
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = animatedBackground,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            // Pulse indicator
            if (showPulse && status == InternetStatus.VALIDATED) {
                PulsingDot(color = animatedContent)
                Spacer(modifier = Modifier.width(8.dp))
            }

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = animatedContent,
                modifier = Modifier.size(14.dp)
            )
            
            Spacer(modifier = Modifier.width(6.dp))
            
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = animatedContent
            )
        }
    }
}

/**
 * Indicador pulsante para mostrar actividad/estado activo
 */
@Composable
private fun PulsingDot(
    color: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = modifier
            .size(8.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha))
    )
}

private data class StatusColors(
    val background: Color,
    val content: Color,
    val icon: ImageVector,
    val text: String
)

/**
 * Badge genérico con icono para cualquier estado
 */
@Composable
fun IconBadge(
    text: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}
