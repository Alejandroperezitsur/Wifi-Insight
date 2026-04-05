package com.example.wifiinsight.domain.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Monitoreo REAL del estado WiFi usando BroadcastReceiver.
 * Detecta cambios instantáneos (<500ms) cuando el usuario activa/desactiva WiFi.
 */
class WifiStateMonitor(private val context: Context) {

    companion object {
        private const val TAG = "WifiStateMonitor"
    }

    /**
     * Flow que emite el estado WiFi en tiempo real.
     * Emite inmediatamente cuando cambia el estado.
     */
    val wifiStateFlow: Flow<WifiState> = callbackFlow {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Estado inicial
        trySend(getCurrentWifiState(wifiManager, connectivityManager))

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    WifiManager.WIFI_STATE_CHANGED_ACTION,
                    ConnectivityManager.CONNECTIVITY_ACTION -> {
                        val newState = getCurrentWifiState(wifiManager, connectivityManager)
                        Log.d(TAG, "WiFi state changed: $newState")
                        trySend(newState)
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction(ConnectivityManager.CONNECTIVITY_ACTION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(receiver, filter)
            }
            Log.i(TAG, "WiFiStateMonitor registered")
        } catch (e: Exception) {
            Log.e(TAG, "Error registering receiver", e)
            trySend(WifiState.Error("No se pudo monitorear WiFi"))
        }

        awaitClose {
            try {
                context.unregisterReceiver(receiver)
                Log.d(TAG, "WiFiStateMonitor unregistered")
            } catch (e: Exception) {
                Log.w(TAG, "Error unregistering receiver", e)
            }
        }
    }.distinctUntilChanged()

    private fun getCurrentWifiState(
        wifiManager: WifiManager,
        connectivityManager: ConnectivityManager
    ): WifiState {
        return try {
            val isWifiEnabled = wifiManager.isWifiEnabled
            val isAirplaneMode = SettingsHelper.isAirplaneModeOn(context)

            if (isAirplaneMode) {
                return WifiState.AirplaneMode
            }

            if (!isWifiEnabled) {
                return WifiState.Disabled
            }

            // Verificar conexión activa
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            val isConnected = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

            if (isConnected) {
                WifiState.Connected
            } else {
                WifiState.EnabledDisconnected
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException checking WiFi state", e)
            WifiState.Error("Permisos insuficientes")
        } catch (e: Exception) {
            Log.e(TAG, "Error checking WiFi state", e)
            WifiState.Error("Error al verificar WiFi")
        }
    }
}

/**
 * Estados posibles del WiFi
 */
sealed class WifiState {
    data object Connected : WifiState()
    data object EnabledDisconnected : WifiState() // WiFi on pero sin conexión
    data object Disabled : WifiState()
    data object AirplaneMode : WifiState()
    data class Error(val message: String) : WifiState()
}
