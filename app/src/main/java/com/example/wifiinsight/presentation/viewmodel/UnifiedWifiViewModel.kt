package com.example.wifiinsight.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wifiinsight.data.model.WifiUiState
import com.example.wifiinsight.data.repository.WifiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel v3 - ARQUITECTURA LIMPIA
 * 
 * Principios:
 * - Sin lógica de negocio (toda la lógica está en Repository)
 * - Solo expone el estado UI del repository
 * - Métodos públicos son acciones simples que delegan al repository
 * - Testeable (repository es interfaz inyectable)
 */
@HiltViewModel
class UnifiedWifiViewModel @Inject constructor(
    private val repository: WifiRepository
) : ViewModel() {

    /**
     * Single Source of Truth desde el Repository.
     * 
     * ARQUITECTURA:
     * - stateIn: Convierte cold flow a hot StateFlow
         * - SharingStarted.WhileSubscribed: Solo activo cuando hay collectors
     * - Stops immediately cuando no hay UI observando (ahorro batería)
     */
    val uiState: StateFlow<WifiUiState> = repository.observeWifiState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
            initialValue = WifiUiState()
        )

    init {
        // Trigger scan inicial cuando el ViewModel se crea
        viewModelScope.launch {
            repository.scanNetworks()
        }
    }

    /**
     * Acción: Solicitar escaneo de redes.
     * El resultado se refleja automáticamente en uiState.
     */
    fun scanNetworks() {
        viewModelScope.launch {
            repository.scanNetworks()
        }
    }

    /**
     * Acción: Reintentar operación fallida.
     */
    fun retry() {
        viewModelScope.launch {
            repository.retry()
        }
    }

    /**
     * Acción: Actualizar estado de permisos.
     */
    fun updatePermissions(granted: Boolean, shouldShowRationale: Boolean = false) {
        repository.updatePermissionState(granted, shouldShowRationale)
    }

    /**
     * Acción: Abrir settings de WiFi.
     */
    fun openWifiSettings(): Boolean {
        return repository.openWifiSettings()
    }

    /**
     * Acción: Activar/desactivar modo demo.
     */
    fun setDemoMode(enabled: Boolean) {
        repository.setDemoMode(enabled)
    }

    override fun onCleared() {
        super.onCleared()
        repository.cleanup()
    }
}
