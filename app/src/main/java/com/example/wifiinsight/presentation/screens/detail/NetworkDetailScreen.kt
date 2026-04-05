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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkWifi
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Speed
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import com.example.wifiinsight.data.model.WifiNetwork
import com.example.wifiinsight.data.repository.WifiRepositoryImpl
import com.example.wifiinsight.domain.usecase.ConnectToNetworkUseCase
import com.example.wifiinsight.domain.util.SignalCalculator
import com.example.wifiinsight.presentation.common.components.LoadingState
import com.example.wifiinsight.presentation.common.components.SecurityBadge
import com.example.wifiinsight.presentation.common.components.SignalIndicator
import com.example.wifiinsight.presentation.common.components.getSignalColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkDetailScreen(
    networkId: String,
    savedStateHandle: SavedStateHandle,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = remember { WifiRepositoryImpl(context) }
    val connectUseCase = remember { ConnectToNetworkUseCase(repository) }
    // FIX CRÍTICO #5: Pasar SavedStateHandle real al ViewModel
    val viewModel: NetworkDetailViewModel = viewModel(
        factory = NetworkDetailViewModel.provideFactory(repository, connectUseCase, savedStateHandle)
    )

    val uiState by viewModel.uiState.collectAsState()
    val password by viewModel.password.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState) {
        if (uiState is DetailUiState.ConnectionResult) {
            val result = uiState as DetailUiState.ConnectionResult
            snackbarHostState.showSnackbar(result.message)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Detalles de Red") },
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is DetailUiState.Loading -> {
                    LoadingState()
                }
                is DetailUiState.Error -> {
                    ErrorContent(
                        message = state.message,
                        onRetry = onNavigateBack
                    )
                }
                is DetailUiState.Success -> {
                    NetworkDetailContent(
                        network = state.network,
                        isConnecting = state.isConnecting,
                        password = password,
                        onPasswordChange = viewModel::updatePassword,
                        onConnect = viewModel::connectToNetwork,
                        onNavigateBack = onNavigateBack
                    )
                }
                is DetailUiState.ConnectionResult -> {
                }
            }
        }
    }
}

@Composable
private fun NetworkDetailContent(
    network: WifiNetwork,
    isConnecting: Boolean,
    password: String,
    onPasswordChange: (String) -> Unit,
    onConnect: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    val percentage = SignalCalculator.rssiToPercentage(network.rssi)
    val signalLevel = SignalCalculator.rssiToSignalLevel(network.rssi)
    val signalColor = getSignalColor(signalLevel)
    val bandName = SignalCalculator.getBandName(network.frequency)
    val channel = SignalCalculator.frequencyToChannel(network.frequency)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
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
                    text = network.ssid,
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
            text = "Información Técnica",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        DetailInfoCard(
            icon = Icons.Default.Wifi,
            label = "BSSID",
            value = network.bssid
        )

        Spacer(modifier = Modifier.height(8.dp))

        DetailInfoCard(
            icon = Icons.Default.SignalCellularAlt,
            label = "Intensidad de Señal",
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
                value = password,
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
            enabled = !isConnecting && (password.isNotEmpty() || !network.securityType.isSecure())
        ) {
            if (isConnecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("Conectar a esta red")
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Volver")
        }
    }
}

@Composable
private fun DetailInfoCard(
    icon: ImageVector,
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
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
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
    onRetry: () -> Unit
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
            text = "Error",
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
        Button(onClick = onRetry) {
            Text("Volver")
        }
    }
}

@Composable
private fun <T> remember(calculation: () -> T): T {
    return androidx.compose.runtime.remember { calculation() }
}
