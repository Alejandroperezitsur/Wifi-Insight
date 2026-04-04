package com.example.wifiinsight.data.repository

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.wifiinsight.data.model.ConnectionState
import com.example.wifiinsight.data.model.WifiNetwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean

class WifiRepositoryImpl(private val context: Context) : WifiRepository {

    private val wifiManager: WifiManager by lazy {
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    private val connectivityManager: ConnectivityManager by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    private val scanHistory = Collections.synchronizedList(mutableListOf<WifiNetwork>())
    private val isScanning = AtomicBoolean(false)

    override fun scanNetworks(): Flow<Result<List<WifiNetwork>>> = flow {
        if (!hasRequiredPermissions()) {
            emit(Result.failure(SecurityException("Permisos de ubicación requeridos para escanear redes WiFi")))
            return@flow
        }

        if (!wifiManager.isWifiEnabled) {
            emit(Result.failure(IllegalStateException("WiFi está desactivado")))
            return@flow
        }

        if (isScanning.get()) {
            emit(Result.success(scanHistory.toList()))
            return@flow
        }

        isScanning.set(true)
        emit(Result.success(scanHistory.toList()))

        try {
            val success = wifiManager.startScan()
            if (!success) {
                emit(Result.failure(IllegalStateException("No se pudo iniciar el escaneo. Throttling de Android puede estar activo.")))
                isScanning.set(false)
                return@flow
            }

            delay(2000)

            val scanResults = wifiManager.scanResults
                ?.filter { !it.SSID.isNullOrEmpty() }
                ?.map { WifiNetwork.fromScanResult(it) }
                ?.distinctBy { it.ssid }
                ?.sortedByDescending { it.rssi }
                ?: emptyList()

            synchronized(scanHistory) {
                scanHistory.clear()
                scanHistory.addAll(scanResults)
            }

            emit(Result.success(scanResults))
        } catch (e: SecurityException) {
            emit(Result.failure(SecurityException("Permisos insuficientes: ${e.message}")))
        } catch (e: Exception) {
            emit(Result.failure(e))
        } finally {
            isScanning.set(false)
        }
    }.flowOn(Dispatchers.IO)

    override fun getCurrentConnection(): Flow<ConnectionState> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                trySend(getCurrentConnectionState())
            }

            override fun onLost(network: android.net.Network) {
                trySend(ConnectionState.Disconnected)
            }

            override fun onCapabilitiesChanged(
                network: android.net.Network,
                networkCapabilities: NetworkCapabilities
            ) {
                trySend(getCurrentConnectionState())
            }
        }

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        trySend(getCurrentConnectionState())

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.flowOn(Dispatchers.IO)

    private fun getCurrentConnectionState(): ConnectionState {
        return try {
            val connectionInfo = wifiManager.connectionInfo
            if (connectionInfo?.ssid == null || connectionInfo.ssid == "<unknown ssid>") {
                ConnectionState.Disconnected
            } else {
                val ssid = connectionInfo.ssid.replace("\"", "")
                val ipAddress = getWifiIpAddress()
                val linkSpeed = connectionInfo.linkSpeed
                val rssi = connectionInfo.rssi.takeIf { it != -127 }

                ConnectionState.Connected(
                    ssid = ssid,
                    ipAddress = ipAddress,
                    linkSpeed = linkSpeed,
                    rssi = rssi
                )
            }
        } catch (e: SecurityException) {
            ConnectionState.Error("Permisos insuficientes para obtener información de conexión")
        } catch (e: Exception) {
            ConnectionState.Error("Error al obtener información: ${e.message}")
        }
    }

    override fun monitorSignalStrength(): Flow<Int> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == WifiManager.RSSI_CHANGED_ACTION) {
                    val newRssi = intent.getIntExtra(WifiManager.EXTRA_NEW_RSSI, -100)
                    trySend(newRssi)
                }
            }
        }

        val filter = IntentFilter(WifiManager.RSSI_CHANGED_ACTION)
        context.registerReceiver(receiver, filter)

        wifiManager.connectionInfo?.rssi?.let { trySend(it) }

        awaitClose {
            context.unregisterReceiver(receiver)
        }
    }.flowOn(Dispatchers.IO)

    override fun connectToNetwork(network: WifiNetwork, password: String?): Flow<Result<Boolean>> = flow {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val specifier = WifiNetworkSpecifier.Builder()
                .setSsid(network.ssid)
                .apply {
                    if (network.securityType.isSecure() && !password.isNullOrEmpty()) {
                        setWpa2Passphrase(password)
                    }
                }
                .build()

            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .setNetworkSpecifier(specifier)
                .build()

            emit(Result.success(true))
        } else {
            emit(Result.failure(UnsupportedOperationException("Conexión directa no soportada en Android 9 y menor")))
        }
    }.flowOn(Dispatchers.IO)

    override fun isWifiEnabled(): Boolean {
        return try {
            wifiManager.isWifiEnabled
        } catch (e: SecurityException) {
            false
        }
    }

    override fun setWifiEnabled(enabled: Boolean) {
        try {
            @Suppress("DEPRECATION")
            wifiManager.isWifiEnabled = enabled
        } catch (e: SecurityException) {
        } catch (e: Exception) {
        }
    }

    override fun getScanHistory(): List<WifiNetwork> {
        return synchronized(scanHistory) {
            scanHistory.toList()
        }
    }

    override fun clearScanHistory() {
        synchronized(scanHistory) {
            scanHistory.clear()
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.NEARBY_WIFI_DEVICES
                ) == PackageManager.PERMISSION_GRANTED
            }
            else -> {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
    }

    private fun getWifiIpAddress(): String? {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            interfaces.toList()
                .filter { it.name.startsWith("wlan") }
                .flatMap { it.inetAddresses.toList() }
                .filterIsInstance<Inet4Address>()
                .firstOrNull()
                ?.hostAddress
        } catch (e: Exception) {
            null
        }
    }
}
