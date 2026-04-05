package com.example.wifiinsight.data.model

/**
 * Estados de permiso v3.3
 */
sealed class PermissionState {
    data object Granted : PermissionState()
    data object Denied : PermissionState()
    data object PermanentlyDenied : PermissionState()
}

/**
 * Error UI
 */
data class UiError(
    val title: String,
    val message: String,
    val actionLabel: String? = null,
    val isRecoverable: Boolean = true
)

/**
 * Estados de internet - CHECKING incluido para no bloquear UI
 */
enum class InternetStatus {
    UNKNOWN,
    CHECKING,
    AVAILABLE,
    UNAVAILABLE
}

/**
 * Estado unificado v3.3 - HARDENING FAANG LEVEL
 * Immutable state para Redux pattern
 */
data class WifiState(
    val wifiEnabled: Boolean = false,
    val isAirplaneMode: Boolean = false,
    val isConnected: Boolean = false,
    val ssid: String? = null,
    val bssid: String? = null,
    val ipAddress: String? = null,
    val linkSpeed: Int? = null,
    val signalStrength: Int? = null,
    val signalHistory: List<Int> = emptyList(),
    val internetStatus: InternetStatus = InternetStatus.UNKNOWN,
    val isRefreshingConnection: Boolean = false,
    val isScanning: Boolean = false,
    val scanResults: List<WifiNetwork> = emptyList(),
    val scanThrottleRemaining: Int = 0,
    val permissionState: PermissionState = PermissionState.Denied,
    val locationEnabled: Boolean = true,
    val error: UiError? = null,
    val stateVersion: Long = 0L
)
