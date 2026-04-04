package com.example.wifiinsight.presentation.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wifiinsight.data.model.ConnectionState
import com.example.wifiinsight.data.repository.WifiRepositoryImpl
import com.example.wifiinsight.domain.usecase.GetCurrentConnectionUseCase
import com.example.wifiinsight.domain.usecase.MonitorSignalUseCase
import com.example.wifiinsight.presentation.common.components.LoadingState
import com.example.wifiinsight.presentation.common.components.SignalChart
import com.example.wifiinsight.presentation.common.components.WifiDisabledState
import com.example.wifiinsight.presentation.screens.home.components.ConnectionStatus
import com.example.wifiinsight.presentation.screens.home.components.CurrentNetworkCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = androidx.compose.runtime.remember { WifiRepositoryImpl(context) }
    val getConnectionUseCase = androidx.compose.runtime.remember { GetCurrentConnectionUseCase(repository) }
    val monitorSignalUseCase = androidx.compose.runtime.remember { MonitorSignalUseCase(repository) }
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.provideFactory(
            repository,
            getConnectionUseCase,
            monitorSignalUseCase
        )
    )

    val uiState by viewModel.uiState.collectAsState()
    val isWifiEnabled by viewModel.isWifiEnabled.collectAsState()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("WiFi Insight")
                        uiState.let { state ->
                            if (state is HomeUiState.Success) {
                                ConnectionStatus(
                                    connectionState = state.connectionState,
                                )
                            }
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            if (isWifiEnabled) {
                FloatingActionButton(
                    onClick = onNavigateToScan,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Escanear redes"
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
            if (!isWifiEnabled) {
                WifiDisabledState(
                    onEnableWifi = { viewModel.enableWifi() },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                when (val state = uiState) {
                    is HomeUiState.Loading -> {
                        LoadingState()
                    }
                    is HomeUiState.Error -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Error",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    is HomeUiState.Success -> {
                        HomeContent(
                            connectionState = state.connectionState,
                            signalHistory = state.signalHistory,
                            onNavigateToScan = onNavigateToScan
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeContent(
    connectionState: ConnectionState,
    signalHistory: List<Int>,
    onNavigateToScan: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        CurrentNetworkCard(
            connectionState = connectionState,
            signalHistory = signalHistory
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (signalHistory.isNotEmpty()) {
            Text(
                text = "Historial de Señal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            SignalChart(
                signalHistory = signalHistory,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Acciones Rápidas",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onNavigateToScan,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Buscar Redes WiFi")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { /* TODO: Open WiFi Settings */ },
            modifier = Modifier.fillMaxWidth(),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Icon(
                imageVector = Icons.Default.Wifi,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Abrir Ajustes WiFi")
        }
    }
}
