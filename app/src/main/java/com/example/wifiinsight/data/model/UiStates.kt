package com.example.wifiinsight.data.model

/**
 * ESTADO DE CONEXIÓN - Granular y aislado.
 * Solo cambia cuando la conexión WiFi real cambia.
 */
data class ConnectionUiState(
    val isConnected: Boolean = false,
    val ssid: String? = null,
    val bssid: String? = null,
    val ipAddress: String? = null,
    val linkSpeed: Int? = null,
    val signalStrength: Int? = null,
    val signalHistory: List<Int> = emptyList(),
    val internetStatus: InternetStatus = InternetStatus.NONE,
    val hasInternetCapability: Boolean = false,
    val isInternetValidated: Boolean = false
) {
    val displaySsid: String
        get() = ssid?.takeIf { it.isNotBlank() && it != "<unknown ssid>" } ?: "Red Desconocida"
    
    val displayIp: String
        get() = ipAddress ?: "No disponible"
    
    val displayLinkSpeed: String
        get() = linkSpeed?.let { "$it Mbps" } ?: "—"
    
    val signalPercentage: Int
        get() = signalStrength?.let { calculateSignalPercentage(it) } ?: 0
    
    private fun calculateSignalPercentage(rssi: Int): Int = when {
        rssi >= -50 -> 100
        rssi >= -60 -> 80
        rssi >= -70 -> 60
        rssi >= -80 -> 40
        rssi >= -90 -> 20
        else -> 10
    }
}

/**
 * ESTADO DE ESCANEO - Aislado del resto.
 * Solo cambia durante operaciones de scan.
 */
data class ScanUiState(
    val isScanning: Boolean = false,
    val scanResults: List<WifiNetwork> = emptyList(),
    val throttleRemainingSeconds: Int = 0,
    val lastScanTimestamp: Long = 0L
)

/**
 * ESTADO DEL SISTEMA - Hardware y permisos.
 * Cambia raramente (WiFi on/off, permisos, modo avión).
 */
data class SystemUiState(
    val wifiEnabled: Boolean = false,
    val isAirplaneMode: Boolean = false,
    val permissionState: PermissionState = PermissionState.Unknown,
    val isDemoMode: Boolean = false
)

/**
 * ESTADO DE ERROR - Aislado para manejo de errores.
 */
data class ErrorUiState(
    val error: UiError? = null,
    val hasError: Boolean = false
)

// ===== CLASES SOPORTE =====

sealed class PermissionState {
    data object Unknown : PermissionState()
    data object Granted : PermissionState()
    data class Denied(val shouldShowRationale: Boolean) : PermissionState()
    data object PermanentlyDenied : PermissionState()
}

data class UiError(
    val title: String,
    val message: String,
    val actionLabel: String? = null,
    val isRecoverable: Boolean = true
)

enum class InternetStatus {
    NONE,
    UNVALIDATED,
    VALIDATED
}
