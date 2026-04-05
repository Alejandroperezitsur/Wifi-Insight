package com.example.wifiinsight.presentation.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkWifi
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.wifiinsight.data.model.WifiNetwork
import com.example.wifiinsight.domain.util.SignalCalculator
import com.example.wifiinsight.presentation.common.components.FeedbackTone
import com.example.wifiinsight.presentation.common.components.LoadingState
import com.example.wifiinsight.presentation.common.components.SecurityBadge
import com.example.wifiinsight.presentation.common.components.SignalIndicator
import com.example.wifiinsight.presentation.common.components.StateFeedbackCard
import com.example.wifiinsight.presentation.common.components.getSignalColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkDetailScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NetworkDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Detalle de red") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingState()
                }

                uiState.network == null -> {
                    ErrorContent(
                        message = uiState.errorMessage ?: "No se pudo cargar la red.",
                        onBack = onNavigateBack
                    )
                }

                else -> {
                    NetworkDetailContent(
                        state = uiState,
                        onPasswordChange = viewModel::updatePassword,
                        onConnect = viewModel::connectToNetwork,
                        onDismissResult = viewModel::dismissConnectionResult,
                        onNavigateBack = onNavigateBack
                    )
                }
            }
        }
    }
}

@Composable
private fun NetworkDetailContent(
    state: NetworkDetailUiState,
    onPasswordChange: (String) -> Unit,
    onConnect: () -> Unit,
    onDismissResult: () -> Unit,
    onNavigateBack: () -> Unit
    ) {
    val network = state.network ?: return
    val percentage = SignalCalculator.rssiToPercentage(network.rssi)
    val signalLevel = SignalCalculator.rssiToSignalLevel(network.rssi)
    val signalColor = getSignalColor(signalLevel)
    val bandName = SignalCalculator.getBandName(network.frequency)
    val channel = SignalCalculator.frequencyToChannel(network.frequency)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (state.isStale) {
            StateFeedbackCard(
                title = "Red fuera de alcance",
                message = "Mostrando último estado conocido.",
                tone = FeedbackTone.Warning
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        when (val result = state.connectionResult) {
            ConnectionResultState.Idle -> Unit
            ConnectionResultState.Loading -> {
                StateFeedbackCard(
                    title = "Conectando",
                    message = "Procesando solicitud de conexión.",
                    tone = FeedbackTone.Info
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            is ConnectionResultState.Success -> {
                StateFeedbackCard(
                    title = "Conexión iniciada",
                    message = result.message,
                    tone = FeedbackTone.Success,
                    actionLabel = "Cerrar",
                    onAction = onDismissResult
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            is ConnectionResultState.Error -> {
                StateFeedbackCard(
                    title = "No se pudo conectar",
                    message = result.message,
                    tone = FeedbackTone.Error,
                    actionLabel = "Cerrar",
                    onAction = onDismissResult
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = signalColor.copy(alpha = 0.1f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(signalColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    SignalIndicator(
                        rssi = network.rssi,
                        modifier = Modifier.size(48.dp),
                        showBackground = false
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = network.safeSsid,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                SecurityBadge(securityType = network.securityType)

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "$percentage%",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = signalColor
                )

                Text(
                    text = signalLevel.label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = signalColor
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Información de red",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        DetailInfoCard(
            icon = Icons.Default.Wifi,
            label = "BSSID",
            value = network.safeBssid
        )

        Spacer(modifier = Modifier.height(8.dp))

        DetailInfoCard(
            icon = Icons.Default.SignalCellularAlt,
            label = "Intensidad",
            value = "${network.rssi} dBm"
        )

        Spacer(modifier = Modifier.height(8.dp))

        DetailInfoCard(
            icon = Icons.Default.NetworkWifi,
            label = "Frecuencia",
            value = "${network.frequency} MHz ($bandName)"
        )

        Spacer(modifier = Modifier.height(8.dp))

        DetailInfoCard(
            icon = Icons.Default.Link,
            label = "Canal",
            value = channel.toString()
        )

        Spacer(modifier = Modifier.height(8.dp))

        DetailInfoCard(
            icon = Icons.Default.Lock,
            label = "Seguridad",
            value = network.securityType.displayName()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Conectar",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (network.securityType.isSecure()) {
            OutlinedTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                label = { Text("Contraseña") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(
            onClick = onConnect,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.connectionResult != ConnectionResultState.Loading &&
                (state.password.isNotBlank() || !network.securityType.isSecure())
        ) {
            if (state.connectionResult == ConnectionResultState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("Intentar conexión")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Volver")
        }
    }
}

@Composable
private fun DetailInfoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Detalle no disponible",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onBack) {
            Text("Volver")
        }
    }
}
