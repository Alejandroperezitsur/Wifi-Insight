package com.example.wifiinsight.presentation.screens.scan

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wifiinsight.data.repository.WifiRepositoryImpl
import com.example.wifiinsight.domain.usecase.ScanWifiNetworksUseCase
import com.example.wifiinsight.presentation.common.components.EmptyNetworksState
import com.example.wifiinsight.presentation.common.components.LoadingState
import com.example.wifiinsight.presentation.common.components.SearchingState
import com.example.wifiinsight.presentation.common.components.WifiDisabledState
import com.example.wifiinsight.presentation.screens.scan.components.NetworkCard
import com.example.wifiinsight.presentation.screens.scan.components.RadarAnimation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    onNavigateToDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = remember { WifiRepositoryImpl(context) }
    val scanUseCase = remember { ScanWifiNetworksUseCase(repository) }
    val viewModel: ScanViewModel = viewModel(
        factory = ScanViewModel.provideFactory(repository, scanUseCase)
    )

    val uiState by viewModel.uiState.collectAsState()
    val isWifiEnabled by viewModel.isWifiEnabled.collectAsState()

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

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
                    Text("Redes WiFi Disponibles")
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshScan() },
                        enabled = uiState !is ScanUiState.Loading && isWifiEnabled
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Actualizar"
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            if (isWifiEnabled && uiState is ScanUiState.Success) {
                FloatingActionButton(
                    onClick = { viewModel.refreshScan() }
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
            when {
                !isWifiEnabled -> {
                    WifiDisabledState(
                        onEnableWifi = { viewModel.enableWifi() },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                uiState is ScanUiState.Initial || uiState is ScanUiState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        RadarAnimation(isScanning = true)
                        Spacer(modifier = Modifier.height(24.dp))
                        SearchingState()
                    }
                }

                uiState is ScanUiState.Error -> {
                    val errorMessage = (uiState as ScanUiState.Error).message
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
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        androidx.compose.material3.Button(onClick = { viewModel.refreshScan() }) {
                            Text("Intentar de nuevo")
                        }
                    }
                }

                uiState is ScanUiState.Success -> {
                    val successState = uiState as ScanUiState.Success
                    val networks = successState.networks

                    if (networks.isEmpty()) {
                        EmptyNetworksState(
                            onRetry = { viewModel.refreshScan() },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            AnimatedVisibility(
                                visible = successState.isScanning,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    RadarAnimation(isScanning = true)
                                }
                            }

                            AnimatedContent(
                                targetState = networks,
                                label = "networks"
                            ) { currentNetworks ->
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(
                                        items = currentNetworks,
                                        key = { it.id }
                                    ) { network ->
                                        NetworkCard(
                                            network = network,
                                            onClick = { onNavigateToDetail(network.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> remember(calculation: () -> T): T {
    return androidx.compose.runtime.remember { calculation() }
}
