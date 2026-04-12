package com.example.wifiinsight.data.reducer

import com.example.wifiinsight.data.model.BlockingState
import com.example.wifiinsight.data.model.ConnectionQuality
import com.example.wifiinsight.data.model.InternetStatus
import com.example.wifiinsight.data.model.PermissionState
import com.example.wifiinsight.data.model.SystemDegradation
import com.example.wifiinsight.data.model.UiError
import com.example.wifiinsight.data.model.UserAction
import com.example.wifiinsight.data.model.WifiEvent
import com.example.wifiinsight.data.model.WifiState

object WifiStateReducer {

    fun reduce(state: WifiState, event: WifiEvent): WifiState {
        val updatedState = when (event) {
            is WifiEvent.ActionStarted -> state.copy(
                isProcessing = true,
                lastAction = event.action,
                activeActionToken = event.token,
                stateVersion = state.stateVersion + 1
            )

            is WifiEvent.ActionFinished -> {
                if (state.activeActionToken != event.token) {
                    state
                } else {
                    state.copy(
                        isProcessing = false,
                        activeActionToken = null,
                        stateVersion = state.stateVersion + 1
                    )
                }
            }

            is WifiEvent.ActionTimeout -> {
                if (state.activeActionToken != event.token || !state.isProcessing) {
                    state
                } else {
                    state.copy(
                        errorQueue = enqueueError(
                            state.errorQueue,
                            UiError(
                                title = "La acción tardó demasiado",
                                message = "Tardó demasiado. Intenta de nuevo",
                                isRecoverable = true
                            )
                        ),
                        stateVersion = state.stateVersion + 1
                    )
                }
            }

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
                connectionQuality = if (event.enabled) state.connectionQuality else ConnectionQuality.DISCONNECTED,
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
                connectionQuality = if (event.enabled) ConnectionQuality.DISCONNECTED else state.connectionQuality,
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
                        connectionQuality = ConnectionQuality.DISCONNECTED,
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
                        connectionQuality = if (isNewConnection) {
                            ConnectionQuality.CONNECTING
                        } else {
                            state.connectionQuality
                        },
                        stateVersion = state.stateVersion + 1
                    )
                }
            }

            is WifiEvent.SignalUpdated -> {
                if (!state.isConnected) {
                    state
                } else {
                    state.copy(
                        signalStrength = event.rssi,
                        signalHistory = (state.signalHistory + event.rssi).takeLast(50),
                        stateVersion = state.stateVersion + 1
                    )
                }
            }

            is WifiEvent.ScanRequested -> state

            is WifiEvent.ScanStarted -> state.copy(
                isScanning = true,
                errorQueue = emptyList(),
                stateVersion = state.stateVersion + 1
            )

            is WifiEvent.ScanCompleted -> state.copy(
                isScanning = false,
                networks = event.results,
                lastScanTimestamp = event.completedAtElapsedMs,
                stateVersion = state.stateVersion + 1
            )

            is WifiEvent.ScanFailed -> state.copy(
                isScanning = false,
                errorQueue = enqueueError(state.errorQueue, createError(event.message)),
                stateVersion = state.stateVersion + 1
            )

            is WifiEvent.ConnectionRefreshStarted -> state.copy(
                isRefreshingConnection = true,
                errorQueue = emptyList(),
                stateVersion = state.stateVersion + 1
            )

            is WifiEvent.ConnectionRefreshFinished -> state.copy(
                isRefreshingConnection = false,
                stateVersion = state.stateVersion + 1
            )

            is WifiEvent.InternetCheckStarted -> state.copy(
                internetStatus = InternetStatus.CHECKING,
                connectionQuality = if (state.isConnected) {
                    ConnectionQuality.CONNECTING
                } else {
                    ConnectionQuality.DISCONNECTED
                },
                stateVersion = state.stateVersion + 1
            )

            is WifiEvent.InternetChecked -> state.copy(
                internetStatus = event.status,
                connectionQuality = when {
                    !state.isConnected -> ConnectionQuality.DISCONNECTED
                    event.status == InternetStatus.AVAILABLE -> ConnectionQuality.CONNECTED_INTERNET
                    event.status == InternetStatus.UNAVAILABLE -> ConnectionQuality.CONNECTED_NO_INTERNET
                    else -> ConnectionQuality.CONNECTING
                },
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
                remainingThrottleMs = event.remainingMs.coerceAtLeast(0L),
                stateVersion = state.stateVersion + 1
            )

            is WifiEvent.SystemDegraded -> state.copy(
                systemDegradation = event.degradation,
                stateVersion = state.stateVersion + 1
            )

            is WifiEvent.ErrorOccurred -> state.copy(
                errorQueue = enqueueError(state.errorQueue, event.error),
                stateVersion = state.stateVersion + 1
            )

            is WifiEvent.ErrorCleared -> state.copy(
                errorQueue = state.errorQueue.drop(1),
                stateVersion = state.stateVersion + 1
            )
        }

        return normalize(updatedState)
    }

    private fun normalize(state: WifiState): WifiState {
        val normalizedConnected = state.isConnected &&
            state.wifiEnabled &&
            !state.isAirplaneMode &&
            !state.ssid.isNullOrBlank()

        val blockingState = when {
            state.isAirplaneMode -> BlockingState.AirplaneMode
            !state.wifiEnabled -> BlockingState.NoWifi
            state.permissionState != PermissionState.Granted -> BlockingState.NoPermission
            !state.locationEnabled -> BlockingState.LocationOff
            else -> null
        }

        return state.copy(
            isConnected = normalizedConnected,
            internetStatus = if (normalizedConnected) state.internetStatus else InternetStatus.UNKNOWN,
            connectionQuality = when {
                !normalizedConnected -> ConnectionQuality.DISCONNECTED
                state.connectionQuality == ConnectionQuality.DISCONNECTED -> ConnectionQuality.CONNECTING
                else -> state.connectionQuality
            },
            canScan = state.remainingThrottleMs <= 0L,
            blockingState = blockingState
        )
    }

    private fun enqueueError(currentQueue: List<UiError>, error: UiError): List<UiError> {
        val lastError = currentQueue.lastOrNull()
        return if (lastError?.title == error.title && lastError.message == error.message) {
            currentQueue
        } else {
            currentQueue + error
        }
    }

    private fun createError(message: String) = UiError(
        title = "Error",
        message = message,
        isRecoverable = true
    )
}
