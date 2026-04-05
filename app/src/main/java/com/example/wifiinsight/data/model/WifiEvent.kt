package com.example.wifiinsight.data.model

/**
 * Eventos v3.3 - HARDENING FAANG LEVEL
 * Event-driven architecture: TODOS los cambios pasan por eventos
 */
sealed class WifiEvent {
    data class WifiToggled(val enabled: Boolean) : WifiEvent()
    data class AirplaneModeChanged(val enabled: Boolean) : WifiEvent()
    data class ConnectionChanged(
        val ssid: String?,
        val bssid: String?,
        val ipAddress: String?,
        val linkSpeed: Int?,
        val rssi: Int?,
        val hasInternetCapability: Boolean
    ) : WifiEvent()
    data class SignalUpdated(val rssi: Int) : WifiEvent()
    data object ScanRequested : WifiEvent()
    data object ScanStarted : WifiEvent()
    data class ScanCompleted(
        val results: List<WifiNetwork>,
        val completedAtElapsedMs: Long
    ) : WifiEvent()
    data class ScanFailed(val message: String) : WifiEvent()
    data object ConnectionRefreshStarted : WifiEvent()
    data object ConnectionRefreshFinished : WifiEvent()
    data object InternetCheckStarted : WifiEvent()
    data class InternetChecked(val status: InternetStatus) : WifiEvent()
    data class PermissionUpdated(val state: PermissionState) : WifiEvent()
    data class LocationStateChanged(val enabled: Boolean) : WifiEvent()
    data class ThrottleUpdated(val remainingMs: Long) : WifiEvent()
    data class ErrorOccurred(val error: UiError) : WifiEvent()
    data object ErrorCleared : WifiEvent()
}
