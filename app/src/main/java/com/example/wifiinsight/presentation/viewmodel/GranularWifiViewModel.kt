package com.example.wifiinsight.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wifiinsight.data.model.ConnectionUiState
import com.example.wifiinsight.data.model.ErrorUiState
import com.example.wifiinsight.data.model.ScanUiState
import com.example.wifiinsight.data.model.SystemUiState
import com.example.wifiinsight.data.repository.WifiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel v3.1 - ESTADOS GRANULARES
 * 
 * OPTIMIZACIONES:
 * - Expone 4 StateFlows separados (NO un solo estado monolítico)
 * - UI observa solo lo que necesita
 * - Sin recomposiciones innecesarias
 * - SharingStarted.WhileSubscribed(5000) para eficiencia
 */
@HiltViewModel
class GranularWifiViewModel @Inject constructor(
    private val repository: WifiRepository
) : ViewModel() {

    /**
     * Estado de CONEXIÓN - Usado por HomeScreen.
     * Señal, SSID, IP, velocidad, estado de internet.
     */
    val connectionState: StateFlow<ConnectionUiState> = repository.observeConnectionState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
            initialValue = ConnectionUiState()
        )

    /**
     * Estado de ESCANEO - Usado por ScanScreen.
     * Lista de redes, estado de scanning, throttle countdown.
     */
    val scanState: StateFlow<ScanUiState> = repository.observeScanState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
            initialValue = ScanUiState()
        )

    /**
     * Estado del SISTEMA - Usado por ambas screens.
     * WiFi enabled, permisos, modo demo.
     * Cambia muy raramente.
     */
    val systemState: StateFlow<SystemUiState> = repository.observeSystemState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
            initialValue = SystemUiState()
        )

    /**
     * Estado de ERROR - Aislado.
     * Solo se observa cuando hay errores que mostrar.
     */
    val errorState: StateFlow<ErrorUiState> = repository.observeErrorState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
            initialValue = ErrorUiState()
        )

    init {
        // Scan inicial al crear ViewModel
        viewModelScope.launch {
            repository.scanNetworks()
        }
    }

    // ===== ACCIONES =====

    fun scanNetworks() {
        viewModelScope.launch {
            repository.scanNetworks()
        }
    }

    fun retry() {
        viewModelScope.launch {
            repository.retry()
        }
    }

    fun updatePermissions(granted: Boolean, shouldShowRationale: Boolean = false) {
        repository.updatePermissionState(granted, shouldShowRationale)
    }

    fun openWifiSettings(): Boolean {
        return repository.openWifiSettings()
    }

    fun setDemoMode(enabled: Boolean) {
        repository.setDemoMode(enabled)
    }

    override fun onCleared() {
        super.onCleared()
        repository.cleanup()
    }
}
