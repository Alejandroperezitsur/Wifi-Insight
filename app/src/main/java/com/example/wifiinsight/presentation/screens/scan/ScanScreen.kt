package com.example.wifiinsight.presentation.screens.scan

import android.Manifest
import android.app.Activity
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
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
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.example.wifiinsight.data.model.SystemDegradation
import com.example.wifiinsight.data.model.WifiNetwork
import com.example.wifiinsight.data.model.WifiState
import com.example.wifiinsight.presentation.common.components.FeedbackTone
import com.example.wifiinsight.presentation.common.components.StateFeedbackCard
import com.example.wifiinsight.presentation.screens.scan.components.NetworkCard
import com.example.wifiinsight.presentation.screens.scan.components.RadarAnimation
import com.example.wifiinsight.presentation.viewmodel.UnifiedWifiViewModel
import kotlinx.coroutines.flow.distinctUntilChanged

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
    val haptic = LocalHapticFeedback.current
    var initialAutoScanDone by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    var visibleNetworks by remember { mutableStateOf<List<WifiNetwork>>(emptyList()) }
    val contentState = remember(
        uiState.permissionState,
        uiState.blockingState,
        uiState.isScanning,
        uiState.networks.isEmpty()
    ) {
        when {
            uiState.permissionState == PermissionState.PermanentlyDenied -> ScanContentState.PermissionLocked
            uiState.blockingState != null -> ScanContentState.Blocked
            uiState.isScanning -> ScanContentState.Scanning
            uiState.networks.isEmpty() -> ScanContentState.Empty
            else -> ScanContentState.Results
        }
    }
    val requestScan: () -> Unit = {
        if (uiState.canScan && uiState.blockingState == null && !uiState.isScanning) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            Toast.makeText(context, "Buscando redes...", Toast.LENGTH_SHORT).show()
            viewModel.scanNetworks()
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { uiState.networks }
            .distinctUntilChanged()
            .collect { networks ->
                visibleNetworks = networks.take(20)
            }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        viewModel.refreshPermissions(activity)
        if (result.values.all { it }) {
            requestScan()
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
            uiState.canScan &&
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
                        onClick = requestScan,
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
                    onClick = requestScan,
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Escanear redes"
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (uiState.isProcessing) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            ScanStateAlerts(
                state = uiState
            )

            Crossfade(
                targetState = contentState,
                label = "scan-content"
            ) { screenState ->
                when (screenState) {
                    ScanContentState.PermissionLocked -> {
                        PermissionRequiredState(
                            message = "Los permisos están bloqueados. Actívalos desde Ajustes para volver a escanear.",
                            actionLabel = "Abrir ajustes",
                            onAction = { viewModel.openAppSettings() }
                        )
                    }

                    ScanContentState.Blocked -> {
                        when (uiState.blockingState) {
                            BlockingState.NoPermission -> {
                                val isFirstTime = !uiState.permissionState.toString().contains("Requested")
                                val isPermanentlyDenied = uiState.permissionState == PermissionState.PermanentlyDenied
                                
                                PermissionRequiredState(
                                    message = if (isPermanentlyDenied) {
                                        "Los permisos están bloqueados. Actívalos desde Ajustes del sistema."
                                    } else {
                                        "Necesitamos permisos para mostrar redes cercanas."
                                    },
                                    actionLabel = if (isPermanentlyDenied) "Abrir ajustes" else "Permitir acceso",
                                    onAction = {
                                        if (isPermanentlyDenied) {
                                            viewModel.openAppSettings()
                                        } else {
                                            viewModel.markPermissionRequested()
                                            permissionLauncher.launch(requiredPermissions())
                                        }
                                    }
                                )
                            }

                            BlockingState.AirplaneMode -> {
                                BlockingStateContent(
                                    title = "Modo avión activado",
                                    message = "Desactiva el modo avión para recuperar el WiFi.",
                                    actionLabel = "Desactivar modo avión",
                                    onAction = { viewModel.openWifiSettings() }
                                )
                            }

                            BlockingState.LocationOff -> {
                                BlockingStateContent(
                                    title = "Ubicación desactivada",
                                    message = "Activa ubicación para que Android permita escanear redes WiFi.",
                                    actionLabel = "Activa ubicación",
                                    onAction = { viewModel.openLocationSettings() }
                                )
                            }

                            BlockingState.NoWifi -> {
                                BlockingStateContent(
                                    title = "WiFi desactivado",
                                    message = "Activa WiFi para comenzar.",
                                    actionLabel = "Activa WiFi",
                                    onAction = { viewModel.openWifiSettings() }
                                )
                            }

                            null -> Unit
                        }
                    }

                    ScanContentState.Scanning -> {
                        ScanningState()
                    }

                    ScanContentState.Empty -> {
                        EmptyScanState(
                            onRetry = requestScan,
                            canScan = uiState.canScan
                        )
                    }

                    ScanContentState.Results -> {
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
                            connectedBssid = uiState.bssid.orEmpty(),
                            onNetworkClick = { network ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onNavigateToDetail(network.bssid)
                            }
                        )
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
    if (state.systemDegradation == SystemDegradation.ScanBlockedBySystem) {
        StateFeedbackCard(
            title = "El sistema está limitando el escaneo",
            message = "Tu dispositivo limita esta función. No es un error de la app y puede devolver listas vacías aunque el WiFi siga activo.",
            tone = FeedbackTone.Warning
        )
        Spacer(modifier = Modifier.height(12.dp))
    }

    if (!state.canScan) {
        StateFeedbackCard(
            title = "Escaneo en espera",
            message = "Puedes escanear en ${(state.remainingThrottleMs / 1000L).coerceAtLeast(1L)}s. Android limita escaneos para ahorrar batería.",
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
    connectedBssid: String,
    onNetworkClick: (WifiNetwork) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp),
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
                isConnected = connectedBssid == network.bssid,
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
        message = "No encontramos redes cercanas. Intenta moverte o esperar unos segundos.",
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

private enum class ScanContentState {
    PermissionLocked,
    Blocked,
    Scanning,
    Empty,
    Results
}
