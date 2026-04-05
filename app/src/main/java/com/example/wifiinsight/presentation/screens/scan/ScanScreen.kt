package com.example.wifiinsight.presentation.screens.scan

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.os.Build
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wifiinsight.data.repository.WifiRepositoryImpl
import com.example.wifiinsight.domain.usecase.ScanWifiNetworksUseCase
import com.example.wifiinsight.presentation.common.components.GlobalStatus
import com.example.wifiinsight.presentation.common.components.GlobalStatusBar
import com.example.wifiinsight.presentation.common.components.LoadingButton
import com.example.wifiinsight.presentation.common.components.NetworkListShimmer
import com.example.wifiinsight.presentation.screens.scan.components.NetworkCard
import com.example.wifiinsight.presentation.screens.scan.components.RadarAnimation
import com.example.wifiinsight.presentation.common.components.DemoModeBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    onNavigateToDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val application = context.applicationContext as android.app.Application
    val repository = remember { WifiRepositoryImpl(context) }
    val scanUseCase = remember { ScanWifiNetworksUseCase(repository) }
    val viewModel: ScanViewModel = viewModel(
        factory = ScanViewModel.provideFactory(application, repository, scanUseCase)
    )

    val uiState by viewModel.uiState.collectAsState()
    val isWifiEnabled by viewModel.isWifiEnabled.collectAsState()
    val scanAttemptCount by viewModel.scanAttemptCount.collectAsState()
    val isDemoMode by viewModel.isDemoMode.collectAsState()
    val throttleRemaining by viewModel.throttleRemainingSeconds.collectAsState()
    
    // FIX CRÍTICO: Estado de permisos
    var permissionState by remember { androidx.compose.runtime.mutableStateOf<PermissionScreenState>(PermissionScreenState.Checking) }
    
    // FIX: Launcher para solicitar permisos
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val allGranted = result.values.all { it }
        permissionState = if (allGranted) {
            PermissionScreenState.Granted
        } else {
            val permanentlyDenied = result.entries.any { (permission, granted) ->
                !granted && !androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
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
            androidx.core.content.ContextCompat.checkSelfPermission(context, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        
        permissionState = if (hasPermissions) PermissionScreenState.Granted
        else PermissionScreenState.NotRequested
        
        if (hasPermissions && isWifiEnabled) {
            viewModel.scanNetworks()
        }
    }
    
    val uiEvent by viewModel.uiEvent.collectAsState()
    
    // Determinar estado global para la barra de estado
    val globalStatus = when (val state = uiState) {
        is ScanUiState.Initial -> GlobalStatus.Loading
        is ScanUiState.Loading -> GlobalStatus.Scanning
        is ScanUiState.Timeout -> GlobalStatus.Timeout
        is ScanUiState.Error -> GlobalStatus.Error
        is ScanUiState.Success -> {
            if (state.networks.isEmpty()) GlobalStatus.Disconnected
            else GlobalStatus.Connected
        }
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    
    // FIX UX #8: Mostrar Toast cuando hay eventos de UI (doble-tap, throttling)
    LaunchedEffect(uiEvent) {
        uiEvent?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.consumeUiEvent()
        }
    }

    LaunchedEffect(key1 = true) {
        if (isWifiEnabled) {
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
                        onClick = { viewModel.retryScan() },
                        enabled = uiState !is ScanUiState.Loading && isWifiEnabled
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
            if (isWifiEnabled && uiState is ScanUiState.Success) {
                FloatingActionButton(
                    onClick = { viewModel.retryScan() },
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
            // FIX CRÍTICO: DemoModeBadge siempre visible arriba cuando está activo
            DemoModeBadge(
                isDemoMode = isDemoMode,
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
                            val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = android.net.Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        }
                    )
                }
                !isWifiEnabled -> {
                    WifiDisabledScanState(
                        onEnableWifi = { viewModel.openWifiSettings() }
                    )
                }

                uiState is ScanUiState.Initial || uiState is ScanUiState.Loading -> {
                    ScanningState()
                }

                uiState is ScanUiState.Error -> {
                    val errorState = uiState as ScanUiState.Error
                    ErrorScanState(
                        message = errorState.message,
                        canRetry = errorState.canRetry,
                        onRetry = { viewModel.retryScan() }
                    )
                }
                
                uiState is ScanUiState.Timeout -> {
                    TimeoutScanState(
                        onRetry = { viewModel.retryScan() },
                        attemptCount = scanAttemptCount
                    )
                }

                uiState is ScanUiState.Success -> {
                    val successState = uiState as ScanUiState.Success
                    val networks = successState.networks

                    if (networks.isEmpty()) {
                        EmptyScanState(
                            onRetry = { viewModel.retryScan() },
                            attemptCount = scanAttemptCount
                        )
                    } else {
                        NetworksList(
                            networks = networks,
                            isScanning = successState.isScanning,
                            onNetworkClick = { network ->
                                onNavigateToDetail(network.id)
                            }
                        )
                    }
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
    isScanning: Boolean,
    onNetworkClick: (com.example.wifiinsight.data.model.WifiNetwork) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Radar animation when refreshing
        AnimatedVisibility(
            visible = isScanning,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                RadarAnimation(isScanning = true)
            }
        }

        // Networks list
        AnimatedContent(
            targetState = networks,
            label = "networks"
        ) { currentNetworks ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    count = currentNetworks.size,
                    key = { index -> currentNetworks[index].id }
                ) { index ->
                    val network = currentNetworks[index]
                    NetworkCard(
                        network = network,
                        onClick = { onNetworkClick(network) }
                    )
                }
            }
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
            
            androidx.compose.material3.Button(
                onClick = onEnableWifi
            ) {
                Text("Activar WiFi")
            }
        }
    }
}

@Composable
private fun EmptyScanState(
    onRetry: () -> Unit,
    attemptCount: Int = 0
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
            
            androidx.compose.material3.Button(
                onClick = onRetry
            ) {
                Text("Buscar de nuevo")
            }
        }
    }
}

@Composable
private fun ErrorScanState(
    message: String,
    canRetry: Boolean = true,
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
            Text(
                text = "Error",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            androidx.compose.material3.Button(
                onClick = onRetry,
                enabled = canRetry
            ) {
                Text("Intentar de nuevo")
            }
        }
    }
}

@Composable
private fun TimeoutScanState(
    onRetry: () -> Unit,
    attemptCount: Int = 0
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
                    .size(80.dp)
                    .background(
                        MaterialTheme.colorScheme.tertiaryContainer,
                        RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Esto está tardando...",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "El escaneo está tomando más tiempo de lo esperado. ${if (attemptCount > 0) "Intento #$attemptCount" else ""}",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
/**
 * Estados de permisos para la pantalla de escaneo
 */
private enum class PermissionScreenState {
    Checking,
    NotRequested,
    Denied,
    PermanentlyDenied,
    Granted
}

/**
 * UI para estados de permisos requeridos
 */
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
