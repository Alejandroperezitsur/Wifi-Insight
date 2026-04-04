package com.example.wifiinsight.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.wifiinsight.data.model.ConnectionState
import com.example.wifiinsight.data.repository.WifiRepository
import com.example.wifiinsight.domain.usecase.GetCurrentConnectionUseCase
import com.example.wifiinsight.domain.usecase.MonitorSignalUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import java.util.LinkedList

sealed class HomeUiState {
    data object Loading : HomeUiState()
    data class Success(
        val connectionState: ConnectionState,
        val signalHistory: List<Int>
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

class HomeViewModel(
    private val repository: WifiRepository,
    private val getCurrentConnectionUseCase: GetCurrentConnectionUseCase,
    private val monitorSignalUseCase: MonitorSignalUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _isWifiEnabled = MutableStateFlow(false)
    val isWifiEnabled: StateFlow<Boolean> = _isWifiEnabled.asStateFlow()

    private val signalHistory = LinkedList<Int>()
    private val maxHistorySize = 50

    init {
        checkWifiState()
        startMonitoring()
    }

    fun checkWifiState() {
        _isWifiEnabled.value = repository.isWifiEnabled()
    }

    fun enableWifi() {
        repository.setWifiEnabled(true)
        checkWifiState()
    }

    private fun startMonitoring() {
        getCurrentConnectionUseCase()
            .onEach { connectionState ->
                _uiState.update { currentState ->
                    val currentHistory = if (currentState is HomeUiState.Success) {
                        currentState.signalHistory
                    } else {
                        emptyList()
                    }
                    HomeUiState.Success(
                        connectionState = connectionState,
                        signalHistory = currentHistory
                    )
                }
            }
            .catch { error ->
                _uiState.value = HomeUiState.Error(error.message ?: "Error desconocido")
            }
            .launchIn(viewModelScope)

        monitorSignalUseCase()
            .onEach { rssi ->
                if (signalHistory.size >= maxHistorySize) {
                    signalHistory.removeFirst()
                }
                signalHistory.addLast(rssi)

                _uiState.update { currentState ->
                    val connection = if (currentState is HomeUiState.Success) {
                        currentState.connectionState
                    } else {
                        ConnectionState.Disconnected
                    }
                    HomeUiState.Success(
                        connectionState = connection,
                        signalHistory = signalHistory.toList()
                    )
                }
            }
            .catch { }
            .launchIn(viewModelScope)
    }

    companion object {
        fun provideFactory(
            repository: WifiRepository,
            getCurrentConnectionUseCase: GetCurrentConnectionUseCase,
            monitorSignalUseCase: MonitorSignalUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(
                    repository,
                    getCurrentConnectionUseCase,
                    monitorSignalUseCase
                ) as T
            }
        }
    }
}
