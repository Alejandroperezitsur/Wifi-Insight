package com.example.wifiinsight.data.model

import com.example.wifiinsight.domain.util.SignalCalculator

/**
 * Estados de conexión WiFi con información real de NetworkCapabilities.
 * Distingue entre:
 * - Conexión nominal (tiene IP)
 * - Conexión con internet (tiene ruta)
 * - Conexión con internet validado (realmente funciona)
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

    /**
     * Estado conectado con información REAL de NetworkCapabilities + InternetChecker.
     * 
     * @param hasInternet Si NET_CAPABILITY_INTERNET está presente (tiene ruta)
     * @param isValidated Si pasó validación HTTP real con InternetChecker
     * @param internetStatus Estado detallado de conectividad (NONE/UNVALIDATED/VALIDATED)
     */
    data class Connected(
        val ssid: String,
        val bssid: String = "",
        val ipAddress: String? = null,
        val linkSpeed: Int? = null,
        val rssi: Int? = null,
        val hasInternet: Boolean = false,
        val isValidated: Boolean = false,
        val internetStatus: InternetStatus = InternetStatus.NONE
    ) : ConnectionState() {
        override val message: String = when (internetStatus) {
            InternetStatus.VALIDATED -> "Conectado a $ssid (Internet OK)"
            InternetStatus.UNVALIDATED -> "Conectado a $ssid (Sin validación)"
            InternetStatus.NONE -> "Conectado a $ssid (Sin internet)"
        }

        /**
         * SSID seguro nunca null
         */
        val safeSsid: String
            get() = ssid.takeIf { it.isNotBlank() && it != "<unknown ssid>" } ?: "Red Desconocida"

        /**
         * BSSID seguro nunca null
         */
        val safeBssid: String
            get() = bssid.takeIf { it.isNotBlank() } ?: "No disponible"

        /**
         * IP segura con fallback
         */
        fun getSafeIpAddress(): String = ipAddress ?: "No disponible"

        /**
         * Velocidad segura con formato
         */
        fun getSafeLinkSpeed(): String = linkSpeed?.let { "$it Mbps" } ?: "—"

        /**
         * Señal segura con porcentaje
         */
        fun getSafeSignalPercentage(): Int {
            return rssi?.let { SignalCalculator.rssiToPercentage(it) } ?: 0
        }
    }

    data class Error(val error: String) : ConnectionState() {
        override val message: String = "Error: $error"
    }
}

enum class InternetStatus {
    NONE,           // No tiene ruta a internet
    UNVALIDATED,    // Tiene ruta pero no validado (posible captive portal)
    VALIDATED       // Internet realmente funciona
}

/**
 * Información detallada del estado de red para diagnóstico
 */
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
