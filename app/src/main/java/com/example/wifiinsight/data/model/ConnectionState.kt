package com.example.wifiinsight.data.model

import com.example.wifiinsight.domain.util.SignalCalculator

/**
 * Estados de conexión WiFi v3.3
 * Usa InternetStatus de WifiEvent.kt (SSOT)
 */
sealed class ConnectionState {
    abstract val message: String

    data object Disconnected : ConnectionState() {
        override val message: String = "Desconectado"
    }

    data object Scanning : ConnectionState() {
        override val message: String = "Buscando redes..."
    }

    data object Connecting : ConnectionState() {
        override val message: String = "Conectando..."
    }

    data class Connected(
        val ssid: String,
        val bssid: String = "",
        val ipAddress: String? = null,
        val linkSpeed: Int? = null,
        val rssi: Int? = null,
        val hasInternet: Boolean = false,
        val isValidated: Boolean = false,
        val internetStatus: InternetStatus = InternetStatus.UNKNOWN
    ) : ConnectionState() {
        override val message: String = when (internetStatus) {
            InternetStatus.AVAILABLE -> "Conectado a $ssid (Internet OK)"
            InternetStatus.CHECKING -> "Conectado a $ssid (Validando...)"
            InternetStatus.UNAVAILABLE -> "Conectado a $ssid (Sin internet)"
            InternetStatus.UNKNOWN -> "Conectado a $ssid"
        }

        val safeSsid: String
            get() = ssid.takeIf { it.isNotBlank() && it != "<unknown ssid>" } ?: "Red Desconocida"

        val safeBssid: String
            get() = bssid.takeIf { it.isNotBlank() } ?: "No disponible"

        fun getSafeIpAddress(): String = ipAddress ?: "No disponible"
        fun getSafeLinkSpeed(): String = linkSpeed?.let { "$it Mbps" } ?: "—"
        fun getSafeSignalPercentage(): Int = rssi?.let { SignalCalculator.rssiToPercentage(it) } ?: 0
    }

    data class Error(val error: String) : ConnectionState() {
        override val message: String = "Error: $error"
    }
}

data class NetworkDiagnostics(
    val isWifi: Boolean = false,
    val hasInternet: Boolean = false,
    val isValidated: Boolean = false,
    val isCaptivePortal: Boolean = false,
    val signalStrength: Int? = null,
    val linkSpeed: Int? = null,
    val frequency: Int? = null,
    val ipAddress: String? = null,
    val dnsServers: List<String> = emptyList(),
    val gateway: String? = null,
    val leaseDuration: Int? = null
)
