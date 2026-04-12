package com.example.wifiinsight.presentation.common.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
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
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Badge que indica cuando el modo Demo está activo.
 * Útil para presentaciones y testing.
 */
@Composable
fun DemoModeBadge(
    isDemoMode: Boolean,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    var isPulsing by remember { mutableStateOf(true) }
    
    // Efecto de pulso para llamar la atención
    LaunchedEffect(isDemoMode) {
        if (isDemoMode) {
            while (true) {
                isPulsing = !isPulsing
                delay(1000)
            }
        }
    }
    
    AnimatedVisibility(
        visible = isDemoMode,
        enter = fadeIn() + expandIn(),
        exit = fadeOut() + shrinkOut(),
        modifier = modifier
    ) {
        Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Indicador pulso
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (isPulsing) 
                                MaterialTheme.colorScheme.tertiary 
                            else 
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)
                        )
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Icon(
                    imageVector = Icons.Default.VideogameAsset,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                
                if (!compact) {
                    Spacer(modifier = Modifier.width(6.dp))
                    
                    Text(
                        text = "Modo Demo",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
    }
}

/**
 * Versión compacta del badge para usar en AppBar.
 */
@Composable
fun DemoModeBadgeCompact(
    isDemoMode: Boolean,
    modifier: Modifier = Modifier
) {
    DemoModeBadge(
        isDemoMode = isDemoMode,
        modifier = modifier,
        compact = true
    )
}

/**
 * Versión extendida con descripción completa.
 */
@Composable
fun DemoModeBadgeExtended(
    isDemoMode: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isDemoMode,
        enter = fadeIn() + expandIn(),
        exit = fadeOut() + shrinkOut(),
        modifier = modifier
    ) {
        Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.VideogameAsset,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = "Modo Demo Activo",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    
                    Text(
                        text = "Datos simulados - No reflejan redes reales",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}
