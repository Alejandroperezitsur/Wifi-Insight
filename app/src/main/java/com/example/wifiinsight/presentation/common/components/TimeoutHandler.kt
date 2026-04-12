package com.example.wifiinsight.presentation.common.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect

/**
 * Estados del timeout handler.
 */
sealed class TimeoutState {
    data object Idle : TimeoutState()
    data object Loading : TimeoutState()
    data object Warning : TimeoutState()  // 5s - mostrar advertencia
    data object Error : TimeoutState()    // 10s+ - mostrar error con retry
}

/**
 * Maneja timeouts automáticos para operaciones largas.
 * Flujo: 0s -> Loading, 5s -> Warning, 10s -> Error
 */
@Composable
fun TimeoutHandler(
    isActive: Boolean,
    timeoutMs: Long = 10_000L,
    warningThresholdMs: Long = 5_000L,
    onTimeout: () -> Unit = {},
    onRetry: () -> Unit = {},
    content: @Composable (TimeoutState) -> Unit
) {
    var state by remember { mutableStateOf<TimeoutState>(TimeoutState.Idle) }

    LaunchedEffect(isActive) {
        if (isActive) {
            state = TimeoutState.Loading
            val warningDelay = warningThresholdMs.coerceAtMost(timeoutMs - 1000)
            
            // Esperar hasta el umbral de advertencia
            if (warningDelay > 0) {
                delay(warningDelay)
                if (isActive) {
                    state = TimeoutState.Warning
                }
            }
            
            // Esperar hasta el timeout total
            val remainingDelay = timeoutMs - warningDelay
            if (remainingDelay > 0 && isActive) {
                delay(remainingDelay)
                if (isActive) {
                    state = TimeoutState.Error
                    onTimeout()
                }
            }
        } else {
            state = TimeoutState.Idle
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            state = TimeoutState.Idle
        }
    }

    Column {
        // Mostrar banners según el estado
        AnimatedVisibility(
            visible = state == TimeoutState.Warning,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            TimeoutWarningBanner()
        }

        AnimatedVisibility(
            visible = state == TimeoutState.Error,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            TimeoutErrorBanner(onRetry = onRetry)
        }

        // Contenido principal
        content(state)
    }
}

/**
 * Banner de advertencia a los 5s.
 */
@Composable
fun TimeoutWarningBanner(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.HourglassEmpty,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Esto está tardando más de lo normal",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "Todavía estamos intentando...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

/**
 * Banner de error después de 10s con opción de retry.
 */
@Composable
fun TimeoutErrorBanner(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "La operación tardó demasiado",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Puede haber un problema de conexión o el sistema está ocupado. Intenta de nuevo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Intentar de nuevo")
            }
        }
    }
}

/**
 * Estado de carga extendido con shimmer effect.
 */
@Composable
fun ExtendedLoadingState(
    title: String = "Cargando...",
    subtitle: String = "Por favor espera un momento",
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ShimmerLoading(
                modifier = Modifier
                    .size(80.dp)
                    .padding(bottom = 16.dp)
            )
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                strokeWidth = 3.dp
            )
        }
    }
}
