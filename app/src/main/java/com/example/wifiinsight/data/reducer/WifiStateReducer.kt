package com.example.wifiinsight.data.reducer

import com.example.wifiinsight.data.model.InternetStatus
import com.example.wifiinsight.data.model.PermissionState
import com.example.wifiinsight.data.model.WifiEvent
import com.example.wifiinsight.data.model.WifiState

/**
 * State Reducer v3.3 - HARDENING FAANG LEVEL
 * Pure function: (State, Event) -> State
 * TODA la lógica de estado está aquí. NO hay lógica dispersa.
 */
object WifiStateReducer {

    fun reduce(state: WifiState, event: WifiEvent): WifiState {
        return when (event) {
            is WifiEvent.WifiToggled -> state.copy(
                wifiEnabled = event.enabled,
                isConnected = if (event.enabled) state.isConnected else false,
                ssid = if (event.enabled) state.ssid else null,
                bssid = if (event.enabled) state.bssid else null,
                ipAddress = if (event.enabled) state.ipAddress else null,
                linkSpeed = if (event.enabled) state.linkSpeed else null,
                signalHistory = if (event.enabled) state.signalHistory else emptyList(),
                signalStrength = if (event.enabled) state.signalStrength else null,
                internetStatus = if (event.enabled) state.internetStatus else InternetStatus.UNKNOWN,
                isRefreshingConnection = if (event.enabled) state.isRefreshingConnection else false,
                stateVersion = state.stateVersion + 1
            )

            is WifiEvent.AirplaneModeChanged -> state.copy(
                isAirplaneMode = event.enabled,
                wifiEnabled = if (event.enabled) false else state.wifiEnabled,
                isConnected = if (event.enabled) false else state.isConnected,
                ssid = if (event.enabled) null else state.ssid,
                bssid = if (event.enabled) null else state.bssid,
                ipAddress = if (event.enabled) null else state.ipAddress,
                linkSpeed = if (event.enabled) null else state.linkSpeed,
                signalStrength = if (event.enabled) null else state.signalStrength,
                signalHistory = if (event.enabled) emptyList() else state.signalHistory,
                internetStatus = if (event.enabled) InternetStatus.UNKNOWN else state.internetStatus,
                isRefreshingConnection = if (event.enabled) false else state.isRefreshingConnection,
                stateVersion = state.stateVersion + 1
            )

            is WifiEvent.ConnectionChanged -> {
                if (event.ssid == null) {
                    state.copy(
                        isConnected = false,
                        ssid = null,
                        bssid = null,
                        ipAddress = null,
                        linkSpeed = null,
                        signalStrength = null,
                        signalHistory = emptyList(),
                        internetStatus = InternetStatus.UNKNOWN,
                        stateVersion = state.stateVersion + 1
                    )
                } else {
                    val isNewConnection = event.ssid != state.ssid || event.bssid != state.bssid
                    state.copy(
                        isConnected = true,
                        ssid = event.ssid,
                        bssid = event.bssid,
                        ipAddress = event.ipAddress,
                        linkSpeed = event.linkSpeed,
                        signalStrength = event.rssi,
                        signalHistory = if (isNewConnection) emptyList() else state.signalHistory,
                        internetStatus = if (isNewConnection) InternetStatus.UNKNOWN else state.internetStatus,
                        stateVersion = state.stateVersion + 1
                    )
                }
            }

            is WifiEvent.SignalUpdated -> {
                if (!state.isConnected) return state
                val newHistory = (state.signalHistory + event.rssi).takeLast(50)
                state.copy(
                    signalStrength = event.rssi,
                    signalHistory = newHistory,
                    stateVersion = state.stateVersion + 1
                )
            }

            is WifiEvent.ScanRequested -> state

            is WifiEvent.ScanStarted -> state.copy(
                isScanning = true,
                error = null,
                stateVersion = state.stateVersion + 1
            )

            is WifiEvent.ScanCompleted -> state.copy(
                isScanning = false,
                scanResults = event.results,
                stateVersion = state.stateVersion + 1
            )

            is WifiEvent.ScanFailed -> state.copy(
                isScanning = false,
                error = createError(event.message),
                stateVersion = state.stateVersion + 1
            )

            is WifiEvent.ConnectionRefreshStarted -> state.copy(
                isRefreshingConnection = true,
                error = null,
                stateVersion = state.stateVersion + 1
            )

            is WifiEvent.ConnectionRefreshFinished -> state.copy(
                isRefreshingConnection = false,
                stateVersion = state.stateVersion + 1
            )

            is WifiEvent.InternetCheckStarted -> state.copy(
                internetStatus = InternetStatus.CHECKING,
                stateVersion = state.stateVersion + 1
            )

            is WifiEvent.InternetChecked -> state.copy(
                internetStatus = event.status,
                stateVersion = state.stateVersion + 1
            )

            is WifiEvent.PermissionUpdated -> state.copy(
                permissionState = event.state,
                stateVersion = state.stateVersion + 1
            )

            is WifiEvent.LocationStateChanged -> state.copy(
                locationEnabled = event.enabled,
                stateVersion = state.stateVersion + 1
            )

            is WifiEvent.ThrottleUpdated -> state.copy(
                scanThrottleRemaining = event.remainingSeconds,
                stateVersion = state.stateVersion + 1
            )

            is WifiEvent.ErrorOccurred -> state.copy(
                error = event.error,
                stateVersion = state.stateVersion + 1
            )

            is WifiEvent.ErrorCleared -> state.copy(
                error = null,
                stateVersion = state.stateVersion + 1
            )
        }
    }

    private fun createError(message: String) = com.example.wifiinsight.data.model.UiError(
        title = "Error",
        message = message,
        isRecoverable = true
    )
}
