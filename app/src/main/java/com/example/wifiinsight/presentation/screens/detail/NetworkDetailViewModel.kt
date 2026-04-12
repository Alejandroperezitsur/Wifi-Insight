package com.example.wifiinsight.presentation.screens.detail

import android.net.Uri
import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wifiinsight.data.model.ConnectionQuality
import com.example.wifiinsight.data.model.InternetStatus
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
    val errorMessage: String? = null,
    val isStale: Boolean = false,
    val lastScanTimestamp: Long = 0L,
    val isCurrentConnection: Boolean = false,
    val connectionQuality: ConnectionQuality = ConnectionQuality.DISCONNECTED,
    val internetStatus: InternetStatus = InternetStatus.UNKNOWN
)

private data class DetailLocalState(
    val password: String = "",
    val connectionResult: ConnectionResultState = ConnectionResultState.Idle,
    val lastKnownNetwork: WifiNetwork? = null
)

private const val NETWORK_STALE_TIMEOUT = 10_000L

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
            .onEach { repositoryState ->
                val network = repositoryState.networks.firstOrNull { it.bssid == bssid }
                if (network != null) {
                    localState.update { it.copy(lastKnownNetwork = network) }
                }
            }
            .map { repositoryState ->
                repositoryState to repositoryState.networks.firstOrNull { it.bssid == bssid }
            },
        localState
    ) { repositorySnapshot, local ->
        val repositoryState = repositorySnapshot.first
        val liveNetwork = repositorySnapshot.second
        val resolvedNetwork = liveNetwork ?: local.lastKnownNetwork
        val scanAge = if (repositoryState.lastScanTimestamp == 0L) {
            Long.MAX_VALUE
        } else {
            SystemClock.elapsedRealtime() - repositoryState.lastScanTimestamp
        }
        val isStale = resolvedNetwork != null &&
            (liveNetwork == null || scanAge > NETWORK_STALE_TIMEOUT)
        val isCurrentConnection = resolvedNetwork?.bssid == repositoryState.bssid && repositoryState.isConnected
        NetworkDetailUiState(
            isLoading = false,
            network = resolvedNetwork,
            password = local.password,
            connectionResult = local.connectionResult,
            isStale = isStale,
            lastScanTimestamp = repositoryState.lastScanTimestamp,
            isCurrentConnection = isCurrentConnection,
            connectionQuality = if (isCurrentConnection) {
                repositoryState.connectionQuality
            } else {
                ConnectionQuality.DISCONNECTED
            },
            internetStatus = if (isCurrentConnection) {
                repositoryState.internetStatus
            } else {
                InternetStatus.UNKNOWN
            },
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
            isStale = false,
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
        val network = uiState.value.network ?: localState.value.lastKnownNetwork ?: run {
            localState.update {
                it.copy(connectionResult = ConnectionResultState.Error("No se encontró información de la red"))
            }
            return
        }
        val password = uiState.value.password.ifBlank { null }

        viewModelScope.launch {
            localState.update {
                it.copy(connectionResult = ConnectionResultState.Loading)
            }

            try {
                val result = repository.connectToNetwork(network, password)
                localState.update { current ->
                    current.copy(
                        connectionResult = if (result.isSuccess) {
                            ConnectionResultState.Success(
                                result.getOrNull() ?: "Conectado exitosamente a ${network.safeSsid}"
                            )
                        } else {
                            ConnectionResultState.Error(
                                result.exceptionOrNull()?.message ?: "No se pudo conectar"
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                localState.update { current ->
                    current.copy(
                        connectionResult = ConnectionResultState.Error(
                            e.message ?: "Error inesperado al intentar conectar"
                        )
                    )
                }
            }
        }
    }
}
