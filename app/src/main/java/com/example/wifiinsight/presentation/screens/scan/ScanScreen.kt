package com.example.wifiinsight.presentation.screens.scan

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wifiinsight.presentation.common.components.DemoModeBadge
import com.example.wifiinsight.presentation.screens.scan.components.NetworkCard
import com.example.wifiinsight.presentation.screens.scan.components.RadarAnimation
import com.example.wifiinsight.presentation.viewmodel.UnifiedWifiViewModel
import com.example.wifiinsight.data.repository.WifiRepositoryImpl
import com.example.wifiinsight.domain.util.InternetChecker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    onNavigateToDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: UnifiedWifiViewModel = viewModel(
        factory = remember {
            object : androidx.lifecycle.ViewModelProvider.Factory {
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    val internetChecker = InternetChecker()
                    val repository = WifiRepositoryImpl(context, internetChecker)
                    @Suppress("UNCHECKED_CAST")
                    return UnifiedWifiViewModel(repository) as T
                }
            }
        }
    )
    val uiState by viewModel.state.collectAsState()

    // Estado de permisos
    var permissionState by remember { mutableStateOf<PermissionScreenState>(PermissionScreenState.Checking) }

    // Launcher para solicitar permisos
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val allGranted = result.values.all { it }
        permissionState = if (allGranted) {
            PermissionScreenState.Granted
        } else {
            val permanentlyDenied = result.entries.any { (permission, granted) ->
                !granted && !ActivityCompat.shouldShowRequestPermissionRationale(
                    context as android.app.Activity,
                    permission
                )
            }
            if (permanentlyDenied) PermissionScreenState.PermanentlyDenied
            else PermissionScreenState.Denied
        }
    }

    // Verificar permisos al inicio
    LaunchedEffect(Unit) {
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.NEARBY_WIFI_DEVICES)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val hasPermissions = requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        permissionState = if (hasPermissions) PermissionScreenState.Granted
        else PermissionScreenState.NotRequested

        if (hasPermissions && uiState.wifiEnabled) {
            viewModel.scanNetworks()
        }
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    LaunchedEffect(uiState.wifiEnabled) {
        if (uiState.wifiEnabled && permissionState == PermissionScreenState.Granted) {
            viewModel.scanNetworks()
        }
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Analizador WiFi",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.scanNetworks() },
                        enabled = !uiState.isScanning && uiState.wifiEnabled
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Actualizar"
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            if (uiState.wifiEnabled && !uiState.isScanning) {
                FloatingActionButton(
                    onClick = { viewModel.scanNetworks() },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Escanear"
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Badge de modo demo
            DemoModeBadge(
                isDemoMode = uiState.isDemoMode,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            when {
                permissionState != PermissionScreenState.Granted -> {
                    PermissionRequiredState(
                        state = permissionState,
                        onRequestPermission = {
                            val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                arrayOf(Manifest.permission.NEARBY_WIFI_DEVICES)
                            } else {
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                            permissionLauncher.launch(requiredPermissions)
                        },
                        onOpenSettings = {
                            val intent = android.content.Intent(
                                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            ).apply {
                                data = android.net.Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        }
                    )
                }
                !uiState.wifiEnabled -> {
                    WifiDisabledScanState(
                        onEnableWifi = { viewModel.openWifiSettings() }
                    )
                }
                uiState.isScanning -> {
                    ScanningState()
                }
                uiState.scanResults.isEmpty() -> {
                    EmptyScanState(
                        onRetry = { viewModel.scanNetworks() }
                    )
                }
                else -> {
                    NetworksList(
                        networks = uiState.scanResults,
                        onNetworkClick = { network ->
                            onNavigateToDetail(network.id)
                        }
                    )
                }
            }
        }
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
            text = "Analizando frecuencias...",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Buscando redes WiFi disponibles",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NetworksList(
    networks: List<com.example.wifiinsight.data.model.WifiNetwork>,
    onNetworkClick: (com.example.wifiinsight.data.model.WifiNetwork) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            count = networks.size,
            key = { index -> networks[index].id }
        ) { index ->
            val network = networks[index]
            NetworkCard(
                network = network,
                onClick = { onNetworkClick(network) }
            )
        }
    }
}

@Composable
private fun WifiDisabledScanState(
    onEnableWifi: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .height(120.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WifiOff,
                    contentDescription = null,
                    modifier = Modifier.height(80.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "WiFi Desactivado",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Activa el WiFi para buscar redes",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onEnableWifi
            ) {
                Text("Activar WiFi")
            }
        }
    }
}

@Composable
private fun EmptyScanState(
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .height(120.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WifiOff,
                    contentDescription = null,
                    modifier = Modifier.height(80.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "No se encontraron redes",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Intenta moverte a otra ubicación o verifica tu conexión",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onRetry
            ) {
                Text("Buscar de nuevo")
            }
        }
    }
}

private enum class PermissionScreenState {
    Checking,
    NotRequested,
    Denied,
    PermanentlyDenied,
    Granted
}

@Composable
private fun PermissionRequiredState(
    state: PermissionScreenState,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
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
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = when (state) {
                    PermissionScreenState.PermanentlyDenied -> "Permiso denegado permanentemente"
                    else -> "Permisos necesarios"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = when (state) {
                    PermissionScreenState.PermanentlyDenied ->
                        "Necesitamos tu permiso para buscar redes WiFi. Abre Configuración para activarlo."
                    else ->
                        "Para buscar redes WiFi cercanas, necesitamos acceso a tu ubicación (Android lo requiere para escanear redes)."
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (state == PermissionScreenState.PermanentlyDenied) {
                Button(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Abrir Configuración",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        "Permitir",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
