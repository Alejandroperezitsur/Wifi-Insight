package com.example.wifiinsight.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wifiinsight.data.model.WifiState
import com.example.wifiinsight.data.repository.WifiRepositoryImpl
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel v3.3 - HARDENING FAANG LEVEL
 *
 * FIXES:
 * - Usa WifiState (Redux pattern) en lugar de WifiUiState
 * - Expone StateFlow<WifiState> directo del Repository
 * - Sin lógica de negocio (solo delega)
 */
class UnifiedWifiViewModel(
    private val repository: WifiRepositoryImpl
) : ViewModel() {

    /**
     * SSOT: Estado Redux directo del Repository
     */
    val state: StateFlow<WifiState> = repository.state

    init {
        // Trigger scan inicial
        viewModelScope.launch {
            repository.scanNetworks()
        }
    }

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
