package com.example.wifiinsight.data.repository

import com.example.wifiinsight.data.model.ConnectionUiState
import com.example.wifiinsight.data.model.ErrorUiState
import com.example.wifiinsight.data.model.InternetStatus
import com.example.wifiinsight.data.model.PermissionState
import com.example.wifiinsight.data.model.ScanUiState
import com.example.wifiinsight.data.model.SystemUiState
import com.example.wifiinsight.data.model.UiError
import com.example.wifiinsight.data.model.WifiNetwork
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fake Repository v3.1 - Estados Granulares para Testing
 * 
 * ARQUITECTURA:
 * - 4 StateFlows separados (Connection, Scan, System, Error)
 * - Simula comportamiento real del repository
 * - Control total de estados para tests
 */
class FakeWifiRepository : WifiRepository {

    private val _connectionState = MutableStateFlow(ConnectionUiState())
    private val _scanState = MutableStateFlow(ScanUiState())
    private val _systemState = MutableStateFlow(SystemUiState())
    private val _errorState = MutableStateFlow(ErrorUiState())
    
    override fun observeConnectionState(): Flow<ConnectionUiState> = 
        _connectionState.asStateFlow()
    
    override fun observeScanState(): Flow<ScanUiState> = 
        _scanState.asStateFlow()
    
    override fun observeSystemState(): Flow<SystemUiState> = 
        _systemState.asStateFlow()
    
    override fun observeErrorState(): Flow<ErrorUiState> = 
        _errorState.asStateFlow()

    override suspend fun scanNetworks() {
        _scanState.value = _scanState.value.copy(isScanning = true)
        kotlinx.coroutines.delay(500)
        _scanState.value = _scanState.value.copy(
            isScanning = false,
            scanResults = generateFakeNetworks(),
            lastScanTimestamp = System.currentTimeMillis()
        )
    }

    override suspend fun retry() {
        _errorState.value = ErrorUiState()
        scanNetworks()
    }

    override fun updatePermissionState(granted: Boolean, shouldShowRationale: Boolean) {
        _systemState.value = _systemState.value.copy(
            permissionState = when {
                granted -> PermissionState.Granted
                shouldShowRationale -> PermissionState.Denied(shouldShowRationale = true)
                else -> PermissionState.Denied(shouldShowRationale = false)
            }
        )
    }

    override fun openWifiSettings(): Boolean = true

    override fun setDemoMode(enabled: Boolean) {
        _systemState.value = _systemState.value.copy(isDemoMode = enabled)
        if (enabled) {
            _scanState.value = _scanState.value.copy(
                scanResults = generateFakeNetworks()
            )
        }
    }

    override fun cleanup() {
        // No-op para testing
    }

    // ===== MÉTODOS DE AYUDA PARA TESTS =====

    fun setWifiEnabled(enabled: Boolean) {
        _systemState.value = _systemState.value.copy(wifiEnabled = enabled)
    }

    fun setConnected(ssid: String = "TestNetwork", hasInternet: Boolean = true) {
        _connectionState.value = ConnectionUiState(
            isConnected = true,
            ssid = ssid,
            bssid = "00:11:22:33:44:55",
            ipAddress = "192.168.1.100",
            linkSpeed = 866,
            signalStrength = -50,
            internetStatus = if (hasInternet) InternetStatus.VALIDATED else InternetStatus.NONE,
            hasInternetCapability = true,
            isInternetValidated = hasInternet
        )
        _systemState.value = _systemState.value.copy(wifiEnabled = true)
    }

    fun setDisconnected() {
        _connectionState.value = ConnectionUiState()
    }

    fun setScanning(scanning: Boolean) {
        _scanState.value = _scanState.value.copy(isScanning = scanning)
    }

    fun setError(title: String, message: String) {
        _errorState.value = ErrorUiState(
            error = UiError(
                title = title,
                message = message,
                isRecoverable = true
            ),
            hasError = true
        )
    }

    fun clearError() {
        _errorState.value = ErrorUiState()
    }

    fun addSignalReading(rssi: Int) {
        val current = _connectionState.value.signalHistory.toMutableList()
        current.add(rssi)
        if (current.size > 50) current.removeAt(0)
        _connectionState.value = _connectionState.value.copy(signalHistory = current)
    }

    fun setThrottleRemaining(seconds: Int) {
        _scanState.value = _scanState.value.copy(throttleRemainingSeconds = seconds)
    }

    private fun generateFakeNetworks(): List<WifiNetwork> {
        return listOf(
            WifiNetwork(
                ssid = "TestNetwork_5G",
                bssid = "00:11:22:33:44:55",
                rssi = -45,
                frequency = 5200,
                securityType = com.example.wifiinsight.data.model.SecurityType.WPA3
            ),
            WifiNetwork(
                ssid = "TestNetwork_2.4G",
                bssid = "00:11:22:33:44:66",
                rssi = -55,
                frequency = 2400,
                securityType = com.example.wifiinsight.data.model.SecurityType.WPA2
            ),
            WifiNetwork(
                ssid = "GuestNetwork",
                bssid = "00:11:22:33:44:77",
                rssi = -65,
                frequency = 2400,
                securityType = com.example.wifiinsight.data.model.SecurityType.OPEN
            )
        )
    }
}
