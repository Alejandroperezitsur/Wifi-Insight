package com.example.wifiinsight.presentation.screens.scan

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.wifiinsight.data.model.BlockingState
import com.example.wifiinsight.data.model.PermissionState
import com.example.wifiinsight.data.model.WifiNetwork
import com.example.wifiinsight.data.model.WifiState
import com.example.wifiinsight.presentation.common.components.FeedbackTone
import com.example.wifiinsight.presentation.common.components.StateFeedbackCard
import com.example.wifiinsight.presentation.screens.scan.components.NetworkCard
import com.example.wifiinsight.presentation.screens.scan.components.RadarAnimation
import com.example.wifiinsight.presentation.viewmodel.UnifiedWifiViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    onNavigateToDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UnifiedWifiViewModel = hiltViewModel()
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    var initialAutoScanDone by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val visibleNetworks = remember(uiState.networks) { uiState.networks.take(20) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        viewModel.refreshPermissions(activity)
        if (result.values.all { it }) {
            viewModel.scanNetworks()
        }
    }

    DisposableEffect(lifecycleOwner, activity) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshSystemState(activity)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(uiState.errorQueue.firstOrNull()?.message) {
        val error = uiState.errorQueue.firstOrNull() ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(error.message)
        viewModel.clearError()
    }

    LaunchedEffect(uiState.permissionState, uiState.wifiEnabled, uiState.locationEnabled) {
        if (uiState.permissionState == PermissionState.Granted &&
            uiState.wifiEnabled &&
            uiState.locationEnabled &&
            uiState.networks.isEmpty() &&
            !uiState.isScanning &&
            !initialAutoScanDone
        ) {
            initialAutoScanDone = true
            viewModel.scanNetworks()
        }

        if (uiState.permissionState != PermissionState.Granted ||
            !uiState.wifiEnabled ||
            !uiState.locationEnabled
        ) {
            initialAutoScanDone = false
        }
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Escanear redes WiFi",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.scanNetworks() },
                        enabled = uiState.canScan &&
                            uiState.blockingState == null &&
                            !uiState.isScanning
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Escanear redes"
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            if (uiState.blockingState == null && !uiState.isScanning && uiState.canScan) {
                FloatingActionButton(
                    onClick = { viewModel.scanNetworks() },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Escanear redes"
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            ScanStateAlerts(
                state = uiState
            )

            when {
                uiState.permissionState == PermissionState.PermanentlyDenied -> {
                    PermissionRequiredState(
                        message = "Los permisos están bloqueados. Actívalos desde Ajustes para volver a escanear.",
                        actionLabel = "Abrir ajustes",
                        onAction = { viewModel.openAppSettings() }
                    )
                }

                uiState.blockingState == BlockingState.NoPermission -> {
                    PermissionRequiredState(
                        message = "Permite ubicación para escanear redes.",
                        actionLabel = "Conceder permisos",
                        onAction = {
                            viewModel.markPermissionRequested()
                            permissionLauncher.launch(requiredPermissions())
                        }
                    )
                }

                uiState.blockingState == BlockingState.AirplaneMode -> {
                    BlockingStateContent(
                        title = "Modo avión activado",
                        message = "Desactiva el modo avión para recuperar el WiFi.",
                        actionLabel = "Desactivar modo avión",
                        onAction = { viewModel.openWifiSettings() }
                    )
                }

                uiState.blockingState == BlockingState.LocationOff -> {
                    BlockingStateContent(
                        title = "Ubicación desactivada",
                        message = "Activa ubicación para escanear redes WiFi.",
                        actionLabel = "Activa ubicación",
                        onAction = { viewModel.openLocationSettings() }
                    )
                }

                uiState.blockingState == BlockingState.NoWifi -> {
                    BlockingStateContent(
                        title = "WiFi desactivado",
                        message = "Activa WiFi para comenzar.",
                        actionLabel = "Activa WiFi",
                        onAction = { viewModel.openWifiSettings() }
                    )
                }

                else -> {
                    when {
                        uiState.isScanning -> {
                            ScanningState()
                        }

                        uiState.networks.isEmpty() -> {
                            EmptyScanState(
                                onRetry = { viewModel.scanNetworks() },
                                canScan = uiState.canScan
                            )
                        }

                        else -> {
                            if (uiState.networks.size > visibleNetworks.size) {
                                Text(
                                    text = "Mostrando las 20 redes con mayor prioridad.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                            NetworksList(
                                networks = visibleNetworks,
                                onNetworkClick = { network ->
                                    onNavigateToDetail(network.bssid)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanStateAlerts(
    state: WifiState
) {
    if (!state.canScan) {
        StateFeedbackCard(
            title = "Escaneo en espera",
            message = "Puedes escanear en ${(state.remainingThrottleMs / 1000L).coerceAtLeast(1L)}s.",
            tone = FeedbackTone.Info
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun ScanningState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            RadarAnimation(isScanning = true)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Escaneando redes...",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Buscando puntos de acceso cercanos.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NetworksList(
    networks: List<WifiNetwork>,
    onNetworkClick: (WifiNetwork) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = networks,
            key = { network ->
                network.bssid.ifBlank { "${network.ssid}-${network.frequency}" }
            }
        ) { network ->
            NetworkCard(
                network = network,
                onClick = { onNetworkClick(network) }
            )
        }
    }
}

@Composable
private fun EmptyScanState(
    onRetry: () -> Unit,
    canScan: Boolean
) {
    BlockingStateContent(
        title = "No se encontraron redes",
        message = "No se encontraron redes. Intenta acercarte al router.",
        actionLabel = "Escanear redes",
        onAction = onRetry,
        actionEnabled = canScan
    )
}

@Composable
private fun PermissionRequiredState(
    message: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    BlockingStateContent(
        title = "Permisos requeridos",
        message = message,
        actionLabel = actionLabel,
        onAction = onAction
    )
}

@Composable
private fun BlockingStateContent(
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    actionEnabled: Boolean = true
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = if (title.contains("WiFi")) Icons.Default.WifiOff else Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = actionEnabled,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(actionLabel)
            }
        }
    }
}

private fun requiredPermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.NEARBY_WIFI_DEVICES)
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }
}
