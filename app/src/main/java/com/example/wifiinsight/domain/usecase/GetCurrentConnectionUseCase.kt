package com.example.wifiinsight.domain.usecase

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import com.example.wifiinsight.data.model.ConnectionInfo
import com.example.wifiinsight.domain.util.InternetChecker
import com.example.wifiinsight.domain.util.SignalCalculator
import javax.inject.Inject

/**
 * Caso de uso para obtener información de la conexión WiFi actual.
 * Combina datos del WifiManager y ConnectivityManager.
 */
class GetCurrentConnectionUseCase @Inject constructor(
    private val context: Context,
    private val internetChecker: InternetChecker
) {
    companion object {
        private const val TAG = "GetCurrentConnectionUseCase"
    }

    /**
     * Información completa de la conexión actual.
     */
    data class ConnectionInfo(
        val ssid: String?,
        val bssid: String?,
        val ipAddress: String?,
        val linkSpeed: Int?, // Mbps
        val rssi: Int?, // dBm
        val signalPercentage: Int,
        val signalLevel: String,
        val frequency: Int?, // MHz
        val band: String,
        val hasInternetCapability: Boolean,
        val hasRealInternet: Boolean,
        val isValidated: Boolean,
        val channel: Int
    )

    /**
     * Obtiene información detallada de la conexión WiFi actual.
     * Retorna null si no hay conexión WiFi activa.
     */
    suspend fun execute(): ConnectionInfo? {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

        if (wifiManager == null || connectivityManager == null) {
            return null
        }

        // Verificar si hay red WiFi activa
        val activeNetwork = connectivityManager.activeNetwork ?: return null
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return null

        if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            return null
        }

        // Obtener información de conexión WiFi
        val connectionInfo = wifiManager.connectionInfo
        
        val rawSsid = connectionInfo?.ssid?.replace("\"", "")
        val ssid = rawSsid?.takeIf { it.isNotBlank() && it != "<unknown ssid>" }
        
        val bssid = connectionInfo?.bssid?.takeIf { it.isNotBlank() }
        
        val rssi = connectionInfo?.rssi?.takeIf { it != -127 }
        
        val linkSpeed = connectionInfo?.linkSpeed?.takeIf { it > 0 }
        
        val frequency = connectionInfo?.frequency?.takeIf { it > 0 }

        // Obtener IP
        val linkProperties = connectivityManager.getLinkProperties(activeNetwork)
        val ipAddress = linkProperties?.linkAddresses
            ?.firstOrNull { it.address is java.net.Inet4Address }
            ?.address?.hostAddress

        // Calcular métricas derivadas
        val signalPercentage = rssi?.let { SignalCalculator.rssiToPercentage(it) } ?: 0
        val signalLevel = rssi?.let { SignalCalculator.rssiToSignalLevel(it).label } ?: "Desconocido"
        val band = frequency?.let { SignalCalculator.getBandName(it) } ?: "Unknown"
        val channel = frequency?.let { SignalCalculator.frequencyToChannel(it) } ?: -1

        // Verificar capacidades de internet
        val hasInternetCapability = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        
        // Verificar conectividad real
        val hasRealInternet = if (hasInternetCapability) {
            internetChecker.hasRealInternet()
        } else {
            false
        }

        return ConnectionInfo(
            ssid = ssid,
            bssid = bssid,
            ipAddress = ipAddress,
            linkSpeed = linkSpeed,
            rssi = rssi,
            signalPercentage = signalPercentage,
            signalLevel = signalLevel,
            frequency = frequency,
            band = band,
            hasInternetCapability = hasInternetCapability,
            hasRealInternet = hasRealInternet,
            isValidated = isValidated,
            channel = channel
        )
    }

    /**
     * Verifica rápidamente si hay conexión WiFi activa sin detalles.
     */
    fun isWifiConnected(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = connectivityManager?.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
}
