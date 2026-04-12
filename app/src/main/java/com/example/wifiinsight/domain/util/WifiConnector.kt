package com.example.wifiinsight.domain.util

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.util.Log
import com.example.wifiinsight.data.model.SecurityType
import com.example.wifiinsight.data.model.WifiNetwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * WifiConnector - Maneja conexiones WiFi reales usando WifiNetworkSpecifier (Android 10+)
 * y fallback para versiones anteriores.
 */
class WifiConnector(
    private val connectivityManager: ConnectivityManager?
) {
    companion object {
        private const val TAG = "WifiConnector"
        private const val CONNECTION_TIMEOUT_MS = 30_000L
    }

    /**
     * Intenta conectar a una red WiFi específica.
     *
     * @param network La red a la que conectar
     * @param password Contraseña para redes protegidas (null para redes abiertas)
     * @return Resultado de la conexión
     */
    suspend fun connectToNetwork(
        network: WifiNetwork,
        password: String?
    ): ConnectionResult = withContext(Dispatchers.IO) {
        if (connectivityManager == null) {
            return@withContext ConnectionResult.Error(
                "Servicio de conectividad no disponible"
            )
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return@withContext ConnectionResult.Error(
                "Conexión programática no soportada en Android ${Build.VERSION.SDK_INT}. " +
                "Usa la configuración de WiFi del sistema."
            )
        }

        try {
            withTimeout(CONNECTION_TIMEOUT_MS) {
                performConnection(network, password)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Connection timeout or error", e)
            ConnectionResult.Error(
                e.message ?: "Tiempo de espera agotado al intentar conectar"
            )
        }
    }

    private suspend fun performConnection(
        network: WifiNetwork,
        password: String?
    ): ConnectionResult = suspendCancellableCoroutine { continuation ->
        try {
            val specifier = createNetworkSpecifier(network, password)
                ?: run {
                    continuation.resume(
                        ConnectionResult.Error("No se pudo crear la configuración de red")
                    )
                    return@suspendCancellableCoroutine
                }

            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .setNetworkSpecifier(specifier)
                .build()

            var callback: ConnectivityManager.NetworkCallback? = null
            var isResumed = false

            callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(net: Network) {
                    if (!isResumed) {
                        isResumed = true
                        Log.i(TAG, "✓ Connected to ${network.ssid}")
                        continuation.resume(
                            ConnectionResult.Success(
                                message = "Conectado a ${network.safeSsid}. " +
                                         "Android gestionará esta red automáticamente.",
                                network = net
                            )
                        )
                        try {
                            connectivityManager?.unregisterNetworkCallback(this)
                        } catch (_: Exception) {}
                    }
                }

                override fun onUnavailable() {
                    if (!isResumed) {
                        isResumed = true
                        Log.w(TAG, "✗ Connection unavailable for ${network.ssid}")
                        continuation.resume(
                            ConnectionResult.Error(
                                "No se pudo conectar. Verifica la contraseña o señal de la red."
                            )
                        )
                        try {
                            connectivityManager?.unregisterNetworkCallback(this)
                        } catch (_: Exception) {}
                    }
                }

                override fun onLost(net: Network) {
                    Log.w(TAG, "Connection lost for ${network.ssid}")
                }
            }

            connectivityManager?.requestNetwork(request, callback)

            continuation.invokeOnCancellation {
                try {
                    callback?.let { connectivityManager?.unregisterNetworkCallback(it) }
                } catch (_: Exception) {}
            }

        } catch (e: SecurityException) {
            continuation.resume(
                ConnectionResult.Error("Permisos insuficientes para conectar a redes WiFi")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to network", e)
            continuation.resume(
                ConnectionResult.Error(e.message ?: "Error desconocido al conectar")
            )
        }
    }

    private fun createNetworkSpecifier(
        network: WifiNetwork,
        password: String?
    ): WifiNetworkSpecifier? {
        return try {
            val builder = WifiNetworkSpecifier.Builder()
                .setSsid(network.ssid)

            when (network.securityType) {
                SecurityType.OPEN -> {
                    // No se requiere contraseña
                }
                SecurityType.WEP -> {
                    // WEP no es soportado por WifiNetworkSpecifier por seguridad
                    Log.w(TAG, "WEP not supported by WifiNetworkSpecifier")
                    return null
                }
                SecurityType.WPA -> {
                    password?.let { builder.setWpa2Passphrase(it) }
                }
                SecurityType.WPA2 -> {
                    password?.let { builder.setWpa2Passphrase(it) }
                }
                SecurityType.WPA3 -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        password?.let { builder.setWpa3Passphrase(it) }
                    } else {
                        password?.let { builder.setWpa2Passphrase(it) }
                    }
                }
            }

            builder.build()
        } catch (e: Exception) {
            Log.e(TAG, "Error creating network specifier", e)
            null
        }
    }

    /**
     * Desconecta de una red específica (libera el request).
     * Nota: En Android 10+, no se puede "desconectar" forzosamente,
     * solo liberar la solicitud y dejar que el sistema decida.
     */
    fun disconnect(network: Network) {
        try {
            connectivityManager?.bindProcessToNetwork(null)
            Log.i(TAG, "Released network binding")
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting", e)
        }
    }
}

/**
 * Resultado de una operación de conexión.
 */
sealed class ConnectionResult {
    data class Success(
        val message: String,
        val network: android.net.Network? = null
    ) : ConnectionResult()

    data class Error(
        val message: String
    ) : ConnectionResult()
}
