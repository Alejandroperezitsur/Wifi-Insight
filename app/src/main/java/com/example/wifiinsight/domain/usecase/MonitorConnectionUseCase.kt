package com.example.wifiinsight.domain.usecase

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.example.wifiinsight.data.model.ConnectionQuality
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import javax.inject.Inject

/**
 * Caso de uso para monitorear cambios en la conexión WiFi.
 * Emite eventos cuando la conexión cambia.
 */
class MonitorConnectionUseCase @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "MonitorConnectionUseCase"
    }

    /**
     * Eventos de cambio de conexión.
     */
    sealed class ConnectionEvent {
        data class Connected(
            val ssid: String?,
            val bssid: String?,
            val hasInternet: Boolean
        ) : ConnectionEvent()
        
        data object Disconnected : ConnectionEvent()
        data object WifiDisabled : ConnectionEvent()
        data class QualityChanged(val quality: ConnectionQuality) : ConnectionEvent()
        data class SignalChanged(val rssi: Int) : ConnectionEvent()
    }

    /**
     * Flow que emite eventos de cambio de conexión.
     */
    fun execute(): Flow<ConnectionEvent> = callbackFlow {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) 
            as? ConnectivityManager
            ?: run {
                close()
                return@callbackFlow
            }

        val callback = object : ConnectivityManager.NetworkCallback() {
            private var currentNetwork: Network? = null

            override fun onAvailable(network: Network) {
                currentNetwork = network
                trySend(ConnectionEvent.Connected(
                    ssid = null, // Se obtiene del WifiManager en el repository
                    bssid = null,
                    hasInternet = true
                ))
            }

            override fun onLost(network: Network) {
                if (currentNetwork == network) {
                    trySend(ConnectionEvent.Disconnected)
                    currentNetwork = null
                }
            }

            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities
            ) {
                val hasInternet = capabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_INTERNET
                )
                val isValidated = capabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_VALIDATED
                )

                val quality = when {
                    hasInternet && isValidated -> ConnectionQuality.CONNECTED_INTERNET
                    hasInternet -> ConnectionQuality.CONNECTED_NO_INTERNET
                    else -> ConnectionQuality.CONNECTING
                }

                trySend(ConnectionEvent.QualityChanged(quality))
            }

            override fun onUnavailable() {
                trySend(ConnectionEvent.Disconnected)
            }
        }

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        try {
            connectivityManager.registerNetworkCallback(request, callback)
        } catch (e: SecurityException) {
            close(e)
            return@callbackFlow
        } catch (e: Exception) {
            close(e)
            return@callbackFlow
        }

        awaitClose {
            try {
                connectivityManager.unregisterNetworkCallback(callback)
            } catch (_: Exception) {}
        }
    }.conflate()

    /**
     * Flow que emite cambios de señal RSSI.
     * Nota: Esta es una implementación placeholder - el RSSI real
     * requiere acceso al WifiManager desde el repository.
     */
    fun monitorSignal(): Flow<Int> = kotlinx.coroutines.flow.emptyFlow()
}
