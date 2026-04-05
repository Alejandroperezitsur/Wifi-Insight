package com.example.wifiinsight.presentation.screens.home

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.wifiinsight.data.model.ConnectionState
import com.example.wifiinsight.data.model.InternetStatus
import com.example.wifiinsight.data.model.PermissionState
import com.example.wifiinsight.data.model.WifiState
import com.example.wifiinsight.domain.util.SignalCalculator
import com.example.wifiinsight.presentation.common.components.FeedbackTone
import com.example.wifiinsight.presentation.common.components.SignalChart
import com.example.wifiinsight.presentation.common.components.StateFeedbackCard
import com.example.wifiinsight.presentation.viewmodel.UnifiedWifiViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToScan: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UnifiedWifiViewModel = hiltViewModel()
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    val connectionState by remember(uiState) {
        derivedStateOf {
            ConnectionState.Connected(
                ssid = uiState.ssid ?: "Red desconocida",
                bssid = uiState.bssid.orEmpty(),
                ipAddress = uiState.ipAddress,
                linkSpeed = uiState.linkSpeed,
                rssi = uiState.signalStrength,
                hasInternet = uiState.internetStatus == InternetStatus.AVAILABLE,
                isValidated = uiState.internetStatus == InternetStatus.AVAILABLE,
                internetStatus = uiState.internetStatus
            )
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

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "WiFi Insight",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            if (uiState.wifiEnabled) {
                FloatingActionButton(
                    onClick = onNavigateToScan,
                    containerColor = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Escanear redes",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            HomeStateAlerts(
                state = uiState,
                onOpenAppSettings = viewModel::openAppSettings,
                onOpenLocationSettings = viewModel::openLocationSettings
            )

            when {
                !uiState.wifiEnabled -> {
                    WifiDisabledState(
                        onEnableWifi = viewModel::openWifiSettings,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                uiState.isConnected -> {
                    ConnectedContent(
                        connection = connectionState,
                        signalHistory = uiState.signalHistory,
                        isRefreshingConnection = uiState.isRefreshingConnection,
                        onNavigateToScan = onNavigateToScan,
                        onReEvaluate = viewModel::reEvaluateConnection,
                        onOpenWifiSettings = viewModel::openWifiSettings
                    )
                }

                else -> {
                    DisconnectedContent(
                        isRefreshingConnection = uiState.isRefreshingConnection,
                        onReEvaluate = viewModel::reEvaluateConnection,
                        onOpenSettings = viewModel::openWifiSettings,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeStateAlerts(
    state: WifiState,
    onOpenAppSettings: () -> Boolean,
    onOpenLocationSettings: () -> Boolean
) {
    if (state.isAirplaneMode) {
        StateFeedbackCard(
            title = "Modo avión activado",
            message = "Desactiva el modo avión para recuperar el WiFi.",
            tone = FeedbackTone.Warning
        )
        Spacer(modifier = Modifier.height(12.dp))
    }

    if (!state.locationEnabled) {
        StateFeedbackCard(
            title = "Ubicación desactivada",
            message = "Activa ubicación para poder escanear redes cercanas.",
            tone = FeedbackTone.Warning,
            actionLabel = "Abrir ubicación",
            onAction = { onOpenLocationSettings() }
        )
        Spacer(modifier = Modifier.height(12.dp))
    }

    when (state.permissionState) {
        PermissionState.Denied -> {
            StateFeedbackCard(
                title = "Permiso pendiente",
                message = "Permite ubicación para escanear redes WiFi.",
                tone = FeedbackTone.Warning
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        PermissionState.PermanentlyDenied -> {
            StateFeedbackCard(
                title = "Permiso bloqueado",
                message = "Activa los permisos desde Ajustes para volver a escanear redes.",
                tone = FeedbackTone.Error,
                actionLabel = "Abrir ajustes",
                onAction = { onOpenAppSettings() }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        PermissionState.Granted -> Unit
    }

    if (!state.canScan) {
        StateFeedbackCard(
            title = "Escaneo en espera",
            message = "Puedes escanear en ${(state.remainingThrottleMs / 1000L).coerceAtLeast(1L)}s.",
            tone = FeedbackTone.Info
        )
        Spacer(modifier = Modifier.height(12.dp))
    }

    if (state.isRefreshingConnection) {
        StateFeedbackCard(
            title = "Re-evaluando conexión",
            message = "Actualizando el estado real de la red y del acceso a internet.",
            tone = FeedbackTone.Info
        )
        Spacer(modifier = Modifier.height(12.dp))
    }

}

@Composable
private fun ConnectedContent(
    connection: ConnectionState.Connected,
    signalHistory: List<Int>,
    isRefreshingConnection: Boolean,
    onNavigateToScan: () -> Unit,
    onReEvaluate: () -> Unit,
    onOpenWifiSettings: () -> Boolean
) {
    com.example.wifiinsight.presentation.screens.home.components.CurrentNetworkCard(
        connectionState = connection,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(24.dp))

    SignalSection(signalHistory = signalHistory)

    Spacer(modifier = Modifier.height(24.dp))

    ActionsSection(
        onNavigateToScan = onNavigateToScan,
        onReEvaluate = onReEvaluate,
        onOpenWifiSettings = onOpenWifiSettings,
        isRefreshingConnection = isRefreshingConnection
    )
}

@Composable
private fun SignalSection(signalHistory: List<Int>) {
    val validHistory = remember(signalHistory) {
        signalHistory.filter { it != -127 }
    }
    val lastRssi = validHistory.lastOrNull()
    val signalStatus = remember(validHistory) {
        when {
            validHistory.isEmpty() -> "Sin datos de señal"
            validHistory.last() <= -80 -> "Señal débil"
            SignalCalculator.analyzeStability(validHistory).isStable -> "Señal estable"
            else -> "Señal inestable"
        }
    }

    Column {
        Text(
            text = "Señal en Tiempo Real",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = signalStatus,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Señal: ${SignalCalculator.rssiToQuality(lastRssi)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        SignalChart(
            signalHistory = validHistory,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(16.dp))
        )

        Spacer(modifier = Modifier.height(12.dp))

        SignalReferenceRow()
    }
}

@Composable
private fun SignalReferenceRow() {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        SignalReferenceItem("-30 dBm", "Excelente")
        SignalReferenceItem("-70 dBm", "Aceptable")
        SignalReferenceItem("-90 dBm", "Mala")
    }
}

@Composable
private fun SignalReferenceItem(dbm: String, label: String) {
    Text(
        text = "$dbm · $label",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
private fun ActionsSection(
    onNavigateToScan: () -> Unit,
    onReEvaluate: () -> Unit,
    onOpenWifiSettings: () -> Boolean,
    isRefreshingConnection: Boolean
) {
    Column {
        Text(
            text = "Acciones",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onNavigateToScan,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text("Escanear redes")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onReEvaluate,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isRefreshingConnection,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            if (isRefreshingConnection) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(if (isRefreshingConnection) "Re-evaluando..." else "Re-evaluar conexión")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { onOpenWifiSettings() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Text("Abrir ajustes WiFi")
        }
    }
}

@Composable
private fun WifiDisabledState(
    onEnableWifi: () -> Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WifiOff,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "WiFi desactivado",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Activa WiFi para comenzar.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { onEnableWifi() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Abrir ajustes WiFi")
            }
        }
    }
}

@Composable
private fun DisconnectedContent(
    isRefreshingConnection: Boolean,
    onReEvaluate: () -> Unit,
    onOpenSettings: () -> Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Sin conexión WiFi",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "No hay una red WiFi activa. Re-evalúa la conexión o escanea redes disponibles.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onReEvaluate,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isRefreshingConnection,
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isRefreshingConnection) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(if (isRefreshingConnection) "Re-evaluando..." else "Re-evaluar conexión")
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = { onOpenSettings() }
            ) {
                Text("Abrir ajustes WiFi")
            }
        }
    }
}
