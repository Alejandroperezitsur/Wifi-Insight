package com.example.wifiinsight.data.model

import android.net.wifi.ScanResult
import androidx.compose.runtime.Stable
import com.example.wifiinsight.domain.util.SignalCalculator

@Stable
data class WifiNetwork(
    val bssid: String,
    val ssid: String,
    val rssi: Int,
    val frequency: Int,
    val securityType: SecurityType,
    val timestamp: Long = System.currentTimeMillis(),
    val capabilities: String = ""
) {
    /**
     * SSID seguro - nunca null ni vacío
     */
    val safeSsid: String
        get() = ssid.takeIf { it.isNotBlank() } ?: "<Red Oculta>"

    /**
     * BSSID seguro - nunca null
     */
    val safeBssid: String
        get() = bssid.takeIf { it.isNotBlank() } ?: "No disponible"

    /**
     * Frecuencia formateada en GHz
     */
    val frequencyGHz: String
        get() = "${frequency / 1000.0} GHz"

    /**
     * Señal como porcentaje (0-100)
     */
    val signalPercentage: Int
        get() = SignalCalculator.rssiToPercentage(rssi)

    companion object {
        fun fromScanResult(scanResult: ScanResult): WifiNetwork {
            val safeBssid = scanResult.BSSID.orEmpty()
            val safeSsid = scanResult.SSID.orEmpty().ifBlank { "<Red oculta>" }
            return WifiNetwork(
                bssid = safeBssid,
                ssid = safeSsid,
                rssi = scanResult.level,
                frequency = scanResult.frequency,
                securityType = parseSecurityType(scanResult.capabilities),
                capabilities = scanResult.capabilities ?: ""
            )
        }

        private fun parseSecurityType(capabilities: String?): SecurityType {
            if (capabilities.isNullOrEmpty()) return SecurityType.OPEN

            val caps = capabilities.uppercase()
            return when {
                caps.contains("WPA3") -> SecurityType.WPA3
                caps.contains("WPA2") -> SecurityType.WPA2
                caps.contains("WPA") && !caps.contains("WPA2") -> SecurityType.WPA
                caps.contains("WEP") -> SecurityType.WEP
                else -> SecurityType.OPEN
            }
        }
    }
}

enum class SecurityType {
    OPEN,
    WEP,
    WPA,
    WPA2,
    WPA3;

    fun displayName(): String = when (this) {
        OPEN -> "Abierta"
        WEP -> "WEP"
        WPA -> "WPA"
        WPA2 -> "WPA2"
        WPA3 -> "WPA3"
    }

    fun isSecure(): Boolean = this != OPEN
}
