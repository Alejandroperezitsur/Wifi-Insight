package com.example.wifiinsight.presentation.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material3.ripple
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wifiinsight.data.model.ConnectionState
import com.example.wifiinsight.data.repository.WifiRepositoryImpl
import com.example.wifiinsight.domain.usecase.GetCurrentConnectionUseCase
import com.example.wifiinsight.domain.usecase.MonitorSignalUseCase
import com.example.wifiinsight.domain.util.SystemSettingsHelper
import com.example.wifiinsight.presentation.common.components.DemoModeBadge
import com.example.wifiinsight.presentation.common.components.GlobalStatus
import com.example.wifiinsight.presentation.common.components.GlobalStatusBar
import com.example.wifiinsight.presentation.common.components.LoadingButton
import com.example.wifiinsight.presentation.common.components.ShimmerLoading
import com.example.wifiinsight.presentation.common.components.SignalChart
import com.example.wifiinsight.presentation.screens.home.components.CurrentNetworkCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val application = context.applicationContext as android.app.Application
    val repository = remember { WifiRepositoryImpl(context) }
    val getConnectionUseCase = remember { GetCurrentConnectionUseCase(repository) }
    val monitorSignalUseCase = remember { MonitorSignalUseCase(repository) }
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.provideFactory(
            application,
            repository,
            getConnectionUseCase,
            monitorSignalUseCase
        )
    )

    val uiState by viewModel.uiState.collectAsState()
    val isWifiEnabled by viewModel.isWifiEnabled.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isDemoMode by viewModel.isDemoMode.collectAsState()
    
    // Determinar estado global para la barra de estado
    val globalStatus = when (val state = uiState) {
        is HomeUiState.Loading -> GlobalStatus.Loading
        is HomeUiState.Timeout -> GlobalStatus.Timeout
        is HomeUiState.Error -> GlobalStatus.Error
        is HomeUiState.Success -> {
            when (state.connectionState) {
                is ConnectionState.Connected -> GlobalStatus.Connected
                is ConnectionState.Disconnected -> GlobalStatus.Disconnected
                else -> GlobalStatus.Disconnected
            }
        }
        else -> GlobalStatus.Loading
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    
    // FIX CRÍTICO: Observar lifecycle para detectar onResume y refrescar datos
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Refrescar conexión al volver de settings u otra pantalla
                viewModel.refreshConnection()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "WiFi Insight",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            if (isWifiEnabled) {
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
            
            if (!isWifiEnabled) {
                WifiDisabledState(
                    onEnableWifi = { viewModel.openWifiSettings() },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Barra de estado global siempre visible
                GlobalStatusBar(
                    status = globalStatus,
                    onRetry = { viewModel.retryConnection() },
                    modifier = Modifier.fillMaxWidth()
                )
                
                when (val state = uiState) {
                    is HomeUiState.Loading -> {
                        ExtendedLoadingState()
                    }
                    is HomeUiState.Timeout -> {
                        TimeoutState(onRetry = { viewModel.retryConnection() })
                    }
                    is HomeUiState.Error -> {
                        ErrorState(
                            message = state.message,
                            onRetry = if (state.canRetry) {{ viewModel.retryConnection() }} else null
                        )
                    }
                    is HomeUiState.Success -> {
                        DashboardContent(
                            connectionState = state.connectionState,
                            signalHistory = state.signalHistory,
                            isRefreshing = isRefreshing,
                            onNavigateToScan = onNavigateToScan,
                            onRefresh = { viewModel.refreshConnection() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardContent(
    connectionState: ConnectionState,
    signalHistory: List<Int>,
    isRefreshing: Boolean,
    onNavigateToScan: () -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Hero Card - Red Actual
        CurrentNetworkCard(
            connectionState = connectionState,
            signalHistory = signalHistory,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Gráfica de Señal
        if (signalHistory.isNotEmpty()) {
            SignalSection(signalHistory = signalHistory)
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Acciones Rápidas
        ActionsSection(
            onNavigateToScan = onNavigateToScan,
            onRefresh = onRefresh,
            isRefreshing = isRefreshing,
            context = androidx.compose.ui.platform.LocalContext.current
        )
    }
}

@Composable
private fun SignalSection(signalHistory: List<Int>) {
    Column {
        Text(
            text = "Señal en Tiempo Real",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        SignalChart(
            signalHistory = signalHistory,
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(16.dp))
        )
    }
}

@Composable
private fun ActionsSection(
    onNavigateToScan: () -> Unit,
    onRefresh: () -> Unit,
    isRefreshing: Boolean,
    context: android.content.Context
) {
    Column {
        Text(
            text = "Acciones",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Botón Principal: Escanear con loading state
        LoadingButton(
            onClick = onNavigateToScan,
            text = "Buscar Redes WiFi",
            modifier = Modifier.fillMaxWidth(),
            isLoading = false,
            icon = Icons.Default.Search,
            shape = RoundedCornerShape(16.dp)
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Botón Secundario: Refresh con loading state
        LoadingButton(
            onClick = onRefresh,
            text = "Actualizar Conexión",
            modifier = Modifier.fillMaxWidth(),
            isLoading = isRefreshing,
            loadingText = "Actualizando...",
            icon = Icons.Default.Refresh,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Botón Terciario: Ajustes
        Button(
            onClick = { SystemSettingsHelper.openWifiSettings(context) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Icon(
                imageVector = Icons.Default.Wifi,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "Abrir Ajustes WiFi",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Estado de carga extendido con shimmer effect
 */
@Composable
private fun ExtendedLoadingState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Spinner
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 4.dp
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Cargando datos de conexión...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Obteniendo información de la red",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Shimmer loading
        ShimmerLoading()
    }
}

/**
 * Estado de timeout con opción de retry
 */
@Composable
private fun TimeoutState(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
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
                text = "La conexión está tomando más tiempo de lo esperado. Puedes reintentar o verificar tu conexión WiFi.",
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
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Reintentar",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            TextButton(
                onClick = onRetry
            ) {
                Text("Intentar de nuevo")
            }
        }
    }
}

/**
 * Estado de error mejorado con retry
 */
@Composable
private fun ErrorState(
    message: String,
    onRetry: (() -> Unit)? = null
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
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WifiOff,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Error",
                style = MaterialTheme.typography.titleLarge,
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
            
            if (onRetry != null) {
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = onRetry,
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
                        "Reintentar",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun WifiDisabledState(
    onEnableWifi: () -> Unit,
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
                    modifier = Modifier.size(40.dp),
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
                text = "Activa el WiFi para analizar redes cercanas",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // FIX UX #6: Texto explicativo adicional
            Text(
                text = "Por seguridad, Android requiere que actives el WiFi manualmente",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onEnableWifi,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    "Abrir ajustes WiFi",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
