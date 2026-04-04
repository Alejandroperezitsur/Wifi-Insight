package com.example.wifiinsight.presentation.common.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SettingsEthernet
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    actionButton: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        if (actionButton != null) {
            Spacer(modifier = Modifier.height(24.dp))
            actionButton()
        }
    }
}

@Composable
fun EmptyNetworksState(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyState(
        icon = Icons.Default.SignalWifiOff,
        title = "No se encontraron redes",
        description = "No se detectaron redes WiFi disponibles. Asegúrate de que el WiFi esté activado y vuelve a intentarlo.",
        modifier = modifier,
        actionButton = {
            Button(onClick = onRetry) {
                Text("Escanear de nuevo")
            }
        }
    )
}

@Composable
fun WifiDisabledState(
    onEnableWifi: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyState(
        icon = Icons.Default.WifiOff,
        title = "WiFi desactivado",
        description = "Activa el WiFi para escanear y conectarte a redes disponibles.",
        modifier = modifier,
        actionButton = {
            Button(onClick = onEnableWifi) {
                Text("Activar WiFi")
            }
        }
    )
}

@Composable
fun PermissionDeniedState(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyState(
        icon = Icons.Outlined.SettingsEthernet,
        title = "Permisos requeridos",
        description = "Se necesitan permisos de ubicación para escanear redes WiFi. Esta información no se almacena ni se comparte.",
        modifier = modifier,
        actionButton = {
            Button(onClick = onRequestPermission) {
                Text("Conceder permisos")
            }
        }
    )
}

@Composable
fun SearchingState(
    modifier: Modifier = Modifier
) {
    EmptyState(
        icon = Icons.Outlined.Search,
        title = "Buscando redes...",
        description = "Escaneando redes WiFi disponibles en tu área.",
        modifier = modifier
    )
}
