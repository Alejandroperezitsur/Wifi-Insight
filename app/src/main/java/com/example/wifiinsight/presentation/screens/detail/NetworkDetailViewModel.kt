package com.example.wifiinsight.presentation.screens.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.wifiinsight.data.model.NetworkQuality
import com.example.wifiinsight.data.model.WifiNetwork
import com.example.wifiinsight.data.repository.WifiRepository
import com.example.wifiinsight.domain.usecase.ConnectToNetworkUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

sealed class DetailUiState {
    data object Loading : DetailUiState()
    data class Success(
        val network: WifiNetwork,
        val networkQuality: NetworkQuality,
        val isConnecting: Boolean = false
    ) : DetailUiState()
    data class Error(val message: String) : DetailUiState()
    data class ConnectionResult(val success: Boolean, val message: String) : DetailUiState()
}

class NetworkDetailViewModel(
    private val repository: WifiRepository,
    private val connectToNetworkUseCase: ConnectToNetworkUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    init {
        loadNetworkDetails()
    }

    private fun loadNetworkDetails() {
        viewModelScope.launch {
            val networkId = savedStateHandle.get<String>("networkId") ?: return@launch

            val networks = repository.getScanHistory()
            val network = networks.find { it.id == networkId }

            if (network != null) {
                val quality = NetworkQuality.fromRssi(network.rssi)
                _uiState.value = DetailUiState.Success(
                    network = network,
                    networkQuality = quality
                )
            } else {
                _uiState.value = DetailUiState.Error("Red no encontrada")
            }
        }
    }

    fun updatePassword(password: String) {
        _password.value = password
    }

    fun connectToNetwork() {
        val currentState = _uiState.value
        if (currentState !is DetailUiState.Success) return

        val network = currentState.network
        val password = _password.value

        _uiState.value = currentState.copy(isConnecting = true)

        connectToNetworkUseCase(network, password.takeIf { it.isNotEmpty() })
            .onEach { result ->
                result.fold(
                    onSuccess = {
                        _uiState.value = DetailUiState.ConnectionResult(
                            success = true,
                            message = "Solicitud de conexión enviada. Revisa las notificaciones del sistema."
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = DetailUiState.ConnectionResult(
                            success = false,
                            message = error.message ?: "Error al conectar"
                        )
                    }
                )
            }
            .catch { error ->
                _uiState.value = DetailUiState.ConnectionResult(
                    success = false,
                    message = error.message ?: "Error inesperado"
                )
            }
            .launchIn(viewModelScope)
    }

    companion object {
        fun provideFactory(
            repository: WifiRepository,
            connectToNetworkUseCase: ConnectToNetworkUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return NetworkDetailViewModel(
                    repository,
                    connectToNetworkUseCase,
                    SavedStateHandle()
                ) as T
            }
        }
    }
}
