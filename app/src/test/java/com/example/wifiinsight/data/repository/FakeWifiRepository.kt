package com.example.wifiinsight.data.repository

import android.app.Activity
import com.example.wifiinsight.data.model.PermissionState
import com.example.wifiinsight.data.model.UiError
import com.example.wifiinsight.data.model.WifiNetwork
import com.example.wifiinsight.data.model.WifiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FakeWifiRepository : WifiRepository {

    private val _uiState = MutableStateFlow(
        WifiState(
            wifiEnabled = true,
            locationEnabled = true,
            permissionState = PermissionState.Granted
        )
    )

    override val uiState: StateFlow<WifiState> = _uiState.asStateFlow()

    private var nextPermissionState: PermissionState = PermissionState.Granted

    override suspend fun scanNetworks() {
        _uiState.update { it.copy(isScanning = true, error = null) }
        _uiState.update {
            it.copy(
                isScanning = false,
                scanResults = generateFakeNetworks()
            )
        }
    }

    override suspend fun reEvaluateConnection() {
        _uiState.update { it.copy(isRefreshingConnection = true, error = null) }
        _uiState.update {
            it.copy(
                wifiEnabled = true,
                isConnected = true,
                ssid = "TestNetwork",
                bssid = "00:11:22:33:44:55",
                linkSpeed = 433,
                signalStrength = -48,
                isRefreshingConnection = false
            )
        }
    }

    override suspend fun connectToNetwork(network: WifiNetwork, password: String?): Result<Unit> {
        return Result.failure(NotImplementedError("WiFi connection not supported"))
    }

    override fun refreshSystemState(activity: Activity?) = Unit

    override fun refreshPermissions(activity: Activity?) {
        _uiState.update { it.copy(permissionState = nextPermissionState) }
    }

    override fun markPermissionRequested() = Unit

    override fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun openWifiSettings(): Boolean = true

    override fun openAppSettings(): Boolean = true

    override fun openLocationSettings(): Boolean = true

    override fun getNetworkByBssid(bssid: String): WifiNetwork? {
        return _uiState.value.scanResults.firstOrNull { it.bssid == bssid }
    }

    fun setPermissionState(permissionState: PermissionState) {
        nextPermissionState = permissionState
    }

    fun setLocationEnabled(enabled: Boolean) {
        _uiState.update { it.copy(locationEnabled = enabled) }
    }

    fun setError(message: String) {
        _uiState.update {
            it.copy(
                error = UiError(
                    title = "Error",
                    message = message
                )
            )
        }
    }

    private fun generateFakeNetworks(): List<WifiNetwork> {
        return listOf(
            WifiNetwork(
                bssid = "00:11:22:33:44:55",
                ssid = "TestNetwork_5G",
                rssi = -45,
                frequency = 5200,
                securityType = com.example.wifiinsight.data.model.SecurityType.WPA3
            ),
            WifiNetwork(
                bssid = "00:11:22:33:44:66",
                ssid = "TestNetwork_2.4G",
                rssi = -55,
                frequency = 2400,
                securityType = com.example.wifiinsight.data.model.SecurityType.WPA2
            ),
            WifiNetwork(
                bssid = "00:11:22:33:44:77",
                ssid = "<Red oculta>",
                rssi = -65,
                frequency = 2400,
                securityType = com.example.wifiinsight.data.model.SecurityType.OPEN
            )
        )
    }
}
