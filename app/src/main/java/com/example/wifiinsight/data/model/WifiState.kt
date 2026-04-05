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

sealed class BlockingState {
    data object NoWifi : BlockingState()
    data object NoPermission : BlockingState()
    data object LocationOff : BlockingState()
    data object AirplaneMode : BlockingState()
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
    val networks: List<WifiNetwork> = emptyList(),
    val lastScanTimestamp: Long = 0L,
    val remainingThrottleMs: Long = 0L,
    val canScan: Boolean = true,
    val permissionState: PermissionState = PermissionState.Denied,
    val locationEnabled: Boolean = true,
    val blockingState: BlockingState? = null,
    val errorQueue: List<UiError> = emptyList(),
    val error: UiError? = null,
    val stateVersion: Long = 0L
) {
    val scanResults: List<WifiNetwork>
        get() = networks

    val scanThrottleRemaining: Int
        get() = (remainingThrottleMs / 1000L).toInt()
}
