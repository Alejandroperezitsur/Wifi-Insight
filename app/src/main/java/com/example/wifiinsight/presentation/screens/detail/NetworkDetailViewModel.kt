package com.example.wifiinsight.presentation.screens.detail

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wifiinsight.data.model.WifiNetwork
import com.example.wifiinsight.data.repository.WifiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class ConnectionResultState {
    data object Idle : ConnectionResultState()
    data object Loading : ConnectionResultState()
    data class Success(val message: String) : ConnectionResultState()
    data class Error(val message: String) : ConnectionResultState()
}

data class NetworkDetailUiState(
    val isLoading: Boolean = true,
    val network: WifiNetwork? = null,
    val password: String = "",
    val connectionResult: ConnectionResultState = ConnectionResultState.Idle,
    val errorMessage: String? = null
)

private data class DetailLocalState(
    val password: String = "",
    val connectionResult: ConnectionResultState = ConnectionResultState.Idle,
    val lastKnownNetwork: WifiNetwork? = null
)

@HiltViewModel
class NetworkDetailViewModel @Inject constructor(
    private val repository: WifiRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val bssid = Uri.decode(savedStateHandle.get<String>("bssid").orEmpty())
    private val initialNetwork = repository.getNetworkByBssid(bssid)

    private val localState = MutableStateFlow(
        DetailLocalState(lastKnownNetwork = initialNetwork)
    )

    val uiState: StateFlow<NetworkDetailUiState> = combine(
        repository.uiState
            .map { repository.getNetworkByBssid(bssid) }
            .onEach { network ->
                if (network != null) {
                    localState.update { it.copy(lastKnownNetwork = network) }
                }
            },
        localState
    ) { liveNetwork, local ->
        val resolvedNetwork = liveNetwork ?: local.lastKnownNetwork
        NetworkDetailUiState(
            isLoading = false,
            network = resolvedNetwork,
            password = local.password,
            connectionResult = local.connectionResult,
            errorMessage = if (resolvedNetwork == null) {
                "Red no disponible. Escanea de nuevo para cargar sus detalles."
            } else {
                null
            }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NetworkDetailUiState(
            isLoading = false,
            network = initialNetwork,
            errorMessage = if (initialNetwork == null) {
                "Red no disponible. Escanea de nuevo para cargar sus detalles."
            } else {
                null
            }
        )
    )

    fun updatePassword(password: String) {
        localState.update { it.copy(password = password) }
    }

    fun dismissConnectionResult() {
        localState.update { it.copy(connectionResult = ConnectionResultState.Idle) }
    }

    fun connectToNetwork() {
        val network = uiState.value.network ?: localState.value.lastKnownNetwork ?: return
        val password = uiState.value.password.ifBlank { null }

        viewModelScope.launch {
            localState.update {
                it.copy(connectionResult = ConnectionResultState.Loading)
            }

            val result = repository.connectToNetwork(network, password)
            localState.update { current ->
                current.copy(
                    connectionResult = if (result.isSuccess) {
                        ConnectionResultState.Success("Conexión iniciada.")
                    } else {
                        ConnectionResultState.Error(
                            result.exceptionOrNull()?.message ?: "No se pudo conectar"
                        )
                    }
                )
            }
        }
    }
}
