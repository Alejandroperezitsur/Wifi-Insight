package com.example.wifiinsight.presentation.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wifiinsight.data.model.WifiState
import com.example.wifiinsight.data.repository.WifiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class UnifiedWifiViewModel @Inject constructor(
    private val repository: WifiRepository
) : ViewModel() {

    val state: StateFlow<WifiState> = repository.uiState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = repository.uiState.value
    )

    fun scanNetworks() {
        viewModelScope.launch {
            repository.scanNetworks()
        }
    }

    fun reEvaluateConnection() {
        viewModelScope.launch {
            repository.reEvaluateConnection()
        }
    }

    fun refreshSystemState(activity: Activity? = null) {
        repository.refreshSystemState(activity)
    }

    fun refreshPermissions(activity: Activity? = null) {
        repository.refreshPermissions(activity)
    }

    fun markPermissionRequested() {
        repository.markPermissionRequested()
    }

    fun clearError() {
        repository.clearError()
    }

    fun openWifiSettings(): Boolean = repository.openWifiSettings()

    fun openAppSettings(): Boolean = repository.openAppSettings()

    fun openLocationSettings(): Boolean = repository.openLocationSettings()
}
