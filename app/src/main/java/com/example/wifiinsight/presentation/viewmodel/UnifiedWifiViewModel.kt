package com.example.wifiinsight.presentation.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wifiinsight.data.model.WifiState
import com.example.wifiinsight.data.repository.WifiRepository
import com.example.wifiinsight.domain.util.ChannelAnalysis
import com.example.wifiinsight.domain.util.ScanDataExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class UnifiedWifiViewModel @Inject constructor(
    private val repository: WifiRepository,
    private val scanDataExporter: ScanDataExporter
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

    fun softReset() {
        repository.softReset()
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

    // Export functions
    private val _exportResult = MutableStateFlow<ScanDataExporter.ExportResult?>(null)
    val exportResult: StateFlow<ScanDataExporter.ExportResult?> = _exportResult.asStateFlow()

    fun exportScanDataToJson() {
        viewModelScope.launch {
            _exportResult.value = scanDataExporter.exportToJson(state.value.networks)
        }
    }

    fun exportScanDataToCsv() {
        viewModelScope.launch {
            _exportResult.value = scanDataExporter.exportToCsv(state.value.networks)
        }
    }

    fun clearExportResult() {
        _exportResult.value = null
    }

    // Channel analysis
    fun analyzeChannelCongestion(): ChannelAnalysis {
        return scanDataExporter.analyzeChannelCongestion(state.value.networks)
    }
}
