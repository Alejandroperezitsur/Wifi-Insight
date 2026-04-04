package com.example.wifiinsight.presentation.screens.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.wifiinsight.data.model.WifiNetwork
import com.example.wifiinsight.data.repository.WifiRepository
import com.example.wifiinsight.domain.usecase.ScanWifiNetworksUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class ScanUiState {
    data object Initial : ScanUiState()
    data object Loading : ScanUiState()
    data class Success(
        val networks: List<WifiNetwork>,
        val isScanning: Boolean = false
    ) : ScanUiState()
    data class Error(val message: String) : ScanUiState()
}

class ScanViewModel(
    private val repository: WifiRepository,
    private val scanWifiNetworksUseCase: ScanWifiNetworksUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScanUiState>(ScanUiState.Initial)
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    private val _isWifiEnabled = MutableStateFlow(false)
    val isWifiEnabled: StateFlow<Boolean> = _isWifiEnabled.asStateFlow()

    private var currentNetworks = listOf<WifiNetwork>()

    init {
        checkWifiState()
    }

    fun checkWifiState() {
        _isWifiEnabled.value = repository.isWifiEnabled()
    }

    fun enableWifi() {
        repository.setWifiEnabled(true)
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            checkWifiState()
            if (repository.isWifiEnabled()) {
                scanNetworks()
            }
        }
    }

    fun scanNetworks() {
        if (_uiState.value is ScanUiState.Loading) return

        _uiState.value = ScanUiState.Loading

        scanWifiNetworksUseCase()
            .onEach { result ->
                result.fold(
                    onSuccess = { networks ->
                        currentNetworks = networks
                        _uiState.value = ScanUiState.Success(
                            networks = networks,
                            isScanning = false
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = ScanUiState.Error(
                            error.message ?: "Error desconocido al escanear"
                        )
                    }
                )
            }
            .catch { error ->
                _uiState.value = ScanUiState.Error(
                    error.message ?: "Error inesperado"
                )
            }
            .launchIn(viewModelScope)
    }

    fun refreshScan() {
        scanNetworks()
    }

    companion object {
        fun provideFactory(
            repository: WifiRepository,
            scanWifiNetworksUseCase: ScanWifiNetworksUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ScanViewModel(repository, scanWifiNetworksUseCase) as T
            }
        }
    }
}
