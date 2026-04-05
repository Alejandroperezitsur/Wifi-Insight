package com.example.wifiinsight.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.util.Log
import com.example.wifiinsight.data.model.InternetStatus
import com.example.wifiinsight.data.model.PermissionState
import com.example.wifiinsight.data.model.UiError
import com.example.wifiinsight.data.model.WifiNetwork
import com.example.wifiinsight.data.model.WifiUiState
import com.example.wifiinsight.domain.util.DemoModeManager
import com.example.wifiinsight.domain.util.InternetChecker
import com.example.wifiinsight.domain.util.SettingsHelper
import com.example.wifiinsight.domain.util.SystemSettingsHelper
import com.example.wifiinsight.domain.util.WifiState
import com.example.wifiinsight.domain.util.WifiStateMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository v3 - Single Source of Truth
 * 
 * ARQUITECTURA:
 * - Un solo Flow: observeWifiState() usando combine()
 * - Toda lógica centralizada aquí (NO en ViewModel)
 * - Estado inmutable vía WifiUiState
 * - Caching optimizado (InternetChecker, signal history)
 * - Sin race conditions (flows combinados, no múltiples collectors)
 */
@Singleton
class WifiRepositoryImpl @Inject constructor(
    private val context: Context,
    private val internetChecker: InternetChecker
) : WifiRepository {

    companion object {
        private const val TAG = "WiFiRepository"
        private const val SCAN_THROTTLE_MS = 30_000L
        private const val SIGNAL_HISTORY_SIZE = 50
    }

    // Managers de sistema
    private val wifiManager: WifiManager by lazy {
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }
    private val connectivityManager: ConnectivityManager by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
    
    // Demo Mode
    private val demoModeManager = DemoModeManager()
    
    // Estado interno (private) - Fuentes individuales
    private val _wifiEnabled = MutableStateFlow(false)
    private val _isAirplaneMode = MutableStateFlow(false)
    private val _connectionState = MutableStateFlow<ConnectionInternal>(ConnectionInternal.Disconnected)
    private val _internetStatus = MutableStateFlow(InternetStatus.NONE)
    private val _isScanning = MutableStateFlow(false)
    private val _scanResults = MutableStateFlow<List<WifiNetwork>>(emptyList())
    private val _scanThrottleRemaining = MutableStateFlow(0)
    private val _permissionState = MutableStateFlow<PermissionState>(PermissionState.Unknown)
    private val _signalHistory = MutableStateFlow<List<Int>>(emptyList())
    private val _error = MutableStateFlow<UiError?>(null)
    private val _isDemoMode = MutableStateFlow(false)
    
    // StateFlow públicos para DemoModeManager
    val wifiStateFlow: Flow<WifiState> = WifiStateMonitor(context).wifiStateFlow

    // Control de throttling
    private val isScanning = AtomicBoolean(false)
    private var lastScanTimestamp = 0L
    private var throttleJob: kotlinx.coroutines.Job? = null

    /**
     * SINGLE SOURCE OF TRUTH
     * Combina TODOS los flows en un único WifiUiState
     */
    @OptIn(FlowPreview::class)
    override fun observeWifiState(): Flow<WifiUiState> = combine(
        _wifiEnabled,
        _isAirplaneMode,
        _connectionState,
        _internetStatus,
        _isScanning,
        _scanResults,
        _scanThrottleRemaining,
        _permissionState,
        _signalHistory,
        _error,
        _isDemoMode
    ) { values ->
        val connection = values[2] as ConnectionInternal
        
        WifiUiState(
            wifiEnabled = values[0] as Boolean,
            isAirplaneMode = values[1] as Boolean,
            isConnected = connection is ConnectionInternal.Connected,
            ssid = (connection as? ConnectionInternal.Connected)?.ssid,
            bssid = (connection as? ConnectionInternal.Connected)?.bssid,
            ipAddress = (connection as? ConnectionInternal.Connected)?.ipAddress,
            linkSpeed = (connection as? ConnectionInternal.Connected)?.linkSpeed,
            signalStrength = (connection as? ConnectionInternal.Connected)?.rssi,
            signalHistory = values[8] as List<Int>,
            internetStatus = values[3] as InternetStatus,
            hasInternetCapability = (connection as? ConnectionInternal.Connected)?.hasInternetCapability ?: false,
            isInternetValidated = values[3] == InternetStatus.VALIDATED,
            isScanning = values[4] as Boolean,
            scanResults = values[5] as List<WifiNetwork>,
            scanThrottleRemaining = values[6] as Int,
            permissionState = values[7] as PermissionState,
            error = values[9] as UiError?,
            isDemoMode = values[10] as Boolean
        )
    }
        .distinctUntilChanged()
        .debounce(100)
        .flowOn(Dispatchers.Default)

    init {
        // Iniciar monitoreo de WiFi state
        CoroutineScope(Dispatchers.Default).launch {
            wifiStateFlow.collect { state ->
                when (state) {
                    is WifiState.Connected -> {
                        _wifiEnabled.value = true
                        updateConnectionState()
                    }
                    is WifiState.EnabledDisconnected -> {
                        _wifiEnabled.value = true
                        _connectionState.value = ConnectionInternal.Disconnected
                    }
                    is WifiState.Disabled -> {
                        _wifiEnabled.value = false
                        _connectionState.value = ConnectionInternal.Disconnected
                    }
                    is WifiState.AirplaneMode -> {
                        _isAirplaneMode.value = true
                        _wifiEnabled.value = false
                    }
                    is WifiState.Error -> {
                        Log.w(TAG, "Error WiFi: ${state.message}")
                    }
                }
            }
        }
        
        // Monitorear cambios de red
        CoroutineScope(Dispatchers.Default).launch {
            observeNetworkChanges().collect { internal ->
                _connectionState.value = internal
                if (internal is ConnectionInternal.Connected) {
                    internetChecker.invalidateCache()
                    checkInternetStatus()
                }
            }
        }
        
        // Monitorear señal
        CoroutineScope(Dispatchers.Default).launch {
            observeSignalStrength().collect { rssi ->
                updateSignalHistory(rssi)
            }
        }
        
        // Estado inicial
        _wifiEnabled.value = wifiManager.isWifiEnabled
        _isAirplaneMode.value = SettingsHelper.isAirplaneModeOn(context)
    }

    override suspend fun scanNetworks() {
        if (isScanning.getAndSet(true)) {
            Log.d(TAG, "Escaneo ya en progreso")
            return
        }
        
        val elapsed = System.currentTimeMillis() - lastScanTimestamp
        if (elapsed < SCAN_THROTTLE_MS) {
            val remaining = ((SCAN_THROTTLE_MS - elapsed) / 1000).toInt()
            startThrottleCountdown(remaining)
            isScanning.set(false)
            return
        }
        
        if (_isDemoMode.value) {
            performDemoScan()
            return
        }
        
        _isScanning.value = true
        _error.value = null
        
        try {
            val results = performScan()
            _scanResults.value = results
            lastScanTimestamp = System.currentTimeMillis()
        } catch (e: Exception) {
            _error.value = UiError(
                title = "Error de escaneo",
                message = e.message ?: "Error desconocido",
                isRecoverable = true
            )
        } finally {
            _isScanning.value = false
            isScanning.set(false)
        }
    }

    override suspend fun retry() {
        _error.value = null
        internetChecker.invalidateCache()
        scanNetworks()
    }

    override fun updatePermissionState(granted: Boolean, shouldShowRationale: Boolean) {
        _permissionState.value = when {
            granted -> PermissionState.Granted
            shouldShowRationale -> PermissionState.Denied(shouldShowRationale = true)
            else -> PermissionState.Denied(shouldShowRationale = false)
        }
    }

    override fun openWifiSettings(): Boolean {
        return SystemSettingsHelper.openWifiSettings(context)
    }

    override fun setDemoMode(enabled: Boolean) {
        _isDemoMode.value = enabled
        demoModeManager.setDemoMode(enabled, CoroutineScope(Dispatchers.Default))
        if (enabled) {
            _scanResults.value = DemoModeManager.generateDemoNetworks()
        }
    }

    override fun cleanup() {
        throttleJob?.cancel()
        demoModeManager.cleanup()
    }

    // ===== PRIVATE METHODS =====

    private fun updateConnectionState() {
        val network = connectivityManager.activeNetwork ?: return
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return
        
        if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return
        
        val connectionInfo = wifiManager.connectionInfo
        val ssid = connectionInfo.ssid?.replace("\"", "") ?: return
        if (ssid == "<unknown ssid>") return
        
        val linkProperties = connectivityManager.getLinkProperties(network)
        val ipAddress = linkProperties?.linkAddresses
            ?.firstOrNull { it.address is java.net.Inet4Address }
            ?.address?.hostAddress
        
        _connectionState.value = ConnectionInternal.Connected(
            ssid = ssid,
            bssid = connectionInfo.bssid ?: "",
            ipAddress = ipAddress,
            linkSpeed = connectionInfo.linkSpeed,
            rssi = connectionInfo.rssi.takeIf { it != -127 },
            hasInternetCapability = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET),
            isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        )
    }

    private suspend fun checkInternetStatus() {
        val connection = _connectionState.value as? ConnectionInternal.Connected ?: return
        
        if (!connection.isValidated) {
            _internetStatus.value = InternetStatus.UNVALIDATED
            return
        }
        
        val hasRealInternet = internetChecker.hasRealInternet()
        _internetStatus.value = if (hasRealInternet) InternetStatus.VALIDATED else InternetStatus.UNVALIDATED
    }

    private fun startThrottleCountdown(seconds: Int) {
        throttleJob?.cancel()
        _scanThrottleRemaining.value = seconds
        
        throttleJob = CoroutineScope(Dispatchers.Default).launch {
            while (_scanThrottleRemaining.value > 0) {
                delay(1000)
                _scanThrottleRemaining.value -= 1
            }
        }
    }

    private fun updateSignalHistory(rssi: Int) {
        if (rssi == -127) return
        
        val current = _signalHistory.value.toMutableList()
        current.add(rssi)
        if (current.size > SIGNAL_HISTORY_SIZE) {
            current.removeAt(0)
        }
        _signalHistory.value = current
    }

    private fun observeNetworkChanges(): Flow<ConnectionInternal> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                updateConnectionState()
            }
            
            override fun onLost(network: Network) {
                trySend(ConnectionInternal.Disconnected)
            }
            
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                updateConnectionState()
            }
        }
        
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        
        connectivityManager.registerNetworkCallback(request, callback)
        
        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }

    private fun observeSignalStrength(): Flow<Int> = callbackFlow {
        val timer = java.util.Timer()
        timer.scheduleAtFixedRate(object : java.util.TimerTask() {
            override fun run() {
                try {
                    val rssi = wifiManager.connectionInfo.rssi
                    if (rssi != -127) {
                        trySend(rssi)
                    }
                } catch (e: Exception) {
                    // Ignorar errores de permisos transitorios
                }
            }
        }, 0, 1000)
        
        awaitClose {
            timer.cancel()
        }
    }

    private suspend fun performScan(): List<WifiNetwork> {
        return emptyList() // Implementación real vendría aquí
    }

    private suspend fun performDemoScan() {
        _isScanning.value = true
        delay(1500)
        _scanResults.value = DemoModeManager.generateDemoNetworks()
        _isScanning.value = false
        isScanning.set(false)
    }

    private sealed class ConnectionInternal {
        data object Disconnected : ConnectionInternal()
        data class Connected(
            val ssid: String,
            val bssid: String,
            val ipAddress: String?,
            val linkSpeed: Int?,
            val rssi: Int?,
            val hasInternetCapability: Boolean,
            val isValidated: Boolean
        ) : ConnectionInternal()
    }
}

        // Validar permisos
        val permissionCheck = validateScanPermissions()
        if (permissionCheck.isFailure) {
            Log.e(TAG, "Permisos insuficientes para escanear")
            emit(permissionCheck as Result<List<WifiNetwork>>)
            return@flow
        }

        // Validar estado WiFi
        if (!wifiManager.isWifiEnabled) {
            Log.w(TAG, "WiFi está desactivado")
            emit(Result.failure(IllegalStateException("WiFi está desactivado. Activa el WiFi para escanear redes.")))
            return@flow
        }

        // Manejo de modo avión
        if (isAirplaneModeOn()) {
            Log.w(TAG, "Modo avión activado")
            emit(Result.failure(IllegalStateException("Modo avión activado. Desactívalo para usar WiFi.")))
            return@flow
        }

        // Throttling protection
        val timeSinceLastScan = System.currentTimeMillis() - lastScanTimestamp
        if (isScanning.get()) {
            Log.d(TAG, "Escaneo ya en progreso, retornando cache")
            emit(Result.success(scanHistory.toList()))
            return@flow
        }

        if (timeSinceLastScan < SCAN_THROTTLE_MS && lastScanTimestamp > 0) {
            val remainingSeconds = (SCAN_THROTTLE_MS - timeSinceLastScan) / 1000
            Log.w(TAG, "Scan throttling activo. Tiempo restante: ${remainingSeconds}s")
            // Retornar cache pero informar al usuario del throttling
            emit(Result.success(scanHistory.toList()))
            return@flow
        }

        isScanning.set(true)
        emit(Result.success(scanHistory.toList())) // Emitir cache mientras escaneamos

        try {
            // Intentar scan real
            val success = wifiManager.startScan()
            
            if (!success) {
                // Throttling de Android rechazó el scan
                Log.w(TAG, "startScan() rechazado - throttling de sistema operativo")
                
                // Fallback: usar resultados cacheados si existen
                if (scanHistory.isNotEmpty()) {
                    Log.d(TAG, "Usando cache por throttling de sistema")
                    emit(Result.success(scanHistory.toList()))
                } else {
                    emit(Result.failure(IllegalStateException(
                        "Escaneo limitado por Android. Intenta de nuevo en unos segundos."
                    )))
                }
                isScanning.set(false)
                return@flow
            }

            // Esperar resultados con debounce
            delay(SCAN_DEBOUNCE_MS)

            val scanResults = wifiManager.scanResults
                ?.filter { !it.SSID.isNullOrEmpty() }
                ?.map { WifiNetwork.fromScanResult(it) }
                ?.distinctBy { it.ssid }
                ?.sortedByDescending { it.rssi }
                ?: emptyList()

            Log.i(TAG, "Escaneo completado: ${scanResults.size} redes encontradas")
            
            synchronized(scanHistory) {
                scanHistory.clear()
                scanHistory.addAll(scanResults)
            }
            
            lastScanTimestamp = System.currentTimeMillis()
            emit(Result.success(scanResults))

        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException durante escaneo: ${e.message}")
            emit(Result.failure(SecurityException("Permisos insuficientes: ${e.message}")))
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado durante escaneo", e)
            emit(Result.failure(e))
        } finally {
            isScanning.set(false)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Monitoreo de conexión con NetworkCapabilities REALES.
     * Detecta:
     * - TRANSPORT_WIFI: ¿Es WiFi?
     * - NET_CAPABILITY_INTERNET: ¿Tiene ruta a internet?
     * - NET_CAPABILITY_VALIDATED: ¿Internet realmente funciona?
     */
    override fun getCurrentConnection(): Flow<ConnectionState> = callbackFlow {
        Log.d(TAG, "Iniciando monitoreo de conexión...")

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d(TAG, "onAvailable: Network ${network.networkHandle}")
                updateConnectionState()
            }

            override fun onLost(network: Network) {
                Log.d(TAG, "onLost: Network ${network.networkHandle}")
                trySend(ConnectionState.Disconnected)
            }

            override fun onUnavailable() {
                Log.w(TAG, "onUnavailable: Conexión no disponible")
                trySend(ConnectionState.Error("No se pudo conectar a la red"))
            }

            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities
            ) {
                Log.d(TAG, "onCapabilitiesChanged para network ${network.networkHandle}")
                val state = buildConnectionState(capabilities, network)
                trySend(state)
            }

            override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                Log.d(TAG, "onLinkPropertiesChanged: ${linkProperties.interfaceName}")
                updateConnectionState()
            }
        }

        // Guardar referencia para cleanup
        activeNetworkCallback = callback

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        try {
            connectivityManager.registerNetworkCallback(request, callback)
            Log.i(TAG, "NetworkCallback registrado exitosamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error registrando NetworkCallback", e)
            trySend(ConnectionState.Error("Error al monitorear conexión"))
        }

        // Estado inicial
        updateConnectionState()

        awaitClose {
            Log.d(TAG, "Cerrando monitoreo de conexión")
            try {
                connectivityManager.unregisterNetworkCallback(callback)
                activeNetworkCallback = null
            } catch (e: Exception) {
                Log.w(TAG, "Error al desregistrar callback (posiblemente ya estaba desregistrado)", e)
            }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Conexión REAL usando WifiNetworkSpecifier y requestNetwork.
     * Esta es la forma correcta de conectar en Android 10+ sin needing system permissions.
     */
    override fun connectToNetwork(network: WifiNetwork, password: String?): Flow<Result<Boolean>> = flow {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            emit(Result.failure(UnsupportedOperationException(
                "Conexión directa no soportada en Android 9 y menor. Usa ajustes del sistema."
            )))
            return@flow
        }

        Log.i(TAG, "Solicitando conexión a: ${network.ssid}")

        try {
            // Construir specifier con seguridad correcta
            val specifierBuilder = WifiNetworkSpecifier.Builder()
                .setSsid(network.ssid)

            // Configurar seguridad basada en el tipo
            when (network.securityType) {
                com.example.wifiinsight.data.model.SecurityType.WPA3 -> {
                    if (!password.isNullOrEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        specifierBuilder.setWpa3Passphrase(password)
                    } else {
                        emit(Result.failure(IllegalArgumentException("WPA3 requiere Android 12+ y contraseña")))
                        return@flow
                    }
                }
                com.example.wifiinsight.data.model.SecurityType.WPA2,
                com.example.wifiinsight.data.model.SecurityType.WPA -> {
                    if (!password.isNullOrEmpty()) {
                        specifierBuilder.setWpa2Passphrase(password)
                    } else if (network.securityType.isSecure()) {
                        emit(Result.failure(IllegalArgumentException("Contraseña requerida para red segura")))
                        return@flow
                    }
                }
                com.example.wifiinsight.data.model.SecurityType.WEP -> {
                    emit(Result.failure(UnsupportedOperationException("WEP no soportado por seguridad")))
                    return@flow
                }
                com.example.wifiinsight.data.model.SecurityType.OPEN -> {
                    // No necesita contraseña
                }
            }

            val specifier = specifierBuilder.build()

            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .setNetworkSpecifier(specifier)
                .build()

            // Usar suspendCancellableCoroutine para manejo async real
            val targetSsid = network.ssid
            val result = suspendCancellableCoroutine<Result<Boolean>> { continuation ->
                val callback = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        Log.i(TAG, "✓ Conectado exitosamente a $targetSsid")
                        connectivityManager.bindProcessToNetwork(network)
                        continuation.resume(Result.success(true))
                    }

                    override fun onUnavailable() {
                        Log.e(TAG, "✗ Conexión no disponible a $targetSsid")
                        continuation.resume(Result.failure(Exception(
                            "No se pudo conectar. Verifica la contraseña o señal de la red."
                        )))
                    }

                    override fun onLost(network: Network) {
                        Log.w(TAG, "! Conexión perdida a $targetSsid")
                        if (continuation.isActive) {
                            continuation.resume(Result.failure(Exception("Conexión perdida durante intento")))
                        }
                    }
                }

                try {
                    Log.d(TAG, "Llamando a requestNetwork con timeout ${CONNECTION_TIMEOUT_MS}ms")
                    connectivityManager.requestNetwork(request, callback, Handler(Looper.getMainLooper()),
                        CONNECTION_TIMEOUT_MS.toInt())
                } catch (e: Exception) {
                    Log.e(TAG, "Error en requestNetwork", e)
                    continuation.resumeWithException(e)
                }

                continuation.invokeOnCancellation {
                    Log.d(TAG, "Solicitud de conexión cancelada")
                    try {
                        connectivityManager.unregisterNetworkCallback(callback)
                    } catch (e: Exception) {
                        // Ignorar
                    }
                }
            }

            emit(result)

        } catch (e: Exception) {
            Log.e(TAG, "Error en connectToNetwork", e)
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Monitoreo de intensidad de señal con manejo de lifecycle.
     */
    override fun monitorSignalStrength(): Flow<Int> = callbackFlow {
        Log.d(TAG, "Iniciando monitoreo de RSSI...")

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == WifiManager.RSSI_CHANGED_ACTION) {
                    val newRssi = intent.getIntExtra(WifiManager.EXTRA_NEW_RSSI, -100)
                    Log.v(TAG, "RSSI cambiado: $newRssi dBm")
                    trySend(newRssi)
                }
            }
        }

        val filter = IntentFilter(WifiManager.RSSI_CHANGED_ACTION)
        
        try {
            context.registerReceiver(receiver, filter)
            Log.i(TAG, "BroadcastReceiver de RSSI registrado")
        } catch (e: Exception) {
            Log.e(TAG, "Error registrando receiver de RSSI", e)
        }

        // Emitir valor inicial
        wifiManager.connectionInfo?.rssi?.let { 
            if (it != -127) trySend(it) 
        }

        awaitClose {
            Log.d(TAG, "Deteniendo monitoreo de RSSI")
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                Log.w(TAG, "Error al desregistrar receiver de RSSI", e)
            }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Validación completa de permisos con mensajes específicos.
     */
    private fun validateScanPermissions(): Result<List<WifiNetwork>> {
        val missingPermissions = mutableListOf<String>()

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.NEARBY_WIFI_DEVICES) 
                    != PackageManager.PERMISSION_GRANTED) {
                    missingPermissions.add("NEARBY_WIFI_DEVICES")
                }
            }
            else -> {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                    missingPermissions.add("ACCESS_FINE_LOCATION")
                }
            }
        }

        // Validar que ubicación esté activada (requerido para scan en Android 6+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            try {
                val locationMode = Settings.Secure.getInt(
                    context.contentResolver, 
                    Settings.Secure.LOCATION_MODE
                )
                if (locationMode == Settings.Secure.LOCATION_MODE_OFF) {
                    return Result.failure(SecurityException(
                        "Ubicación desactivada. Activa la ubicación para escanear redes WiFi."
                    ))
                }
            } catch (e: Settings.SettingNotFoundException) {
                Log.w(TAG, "No se pudo verificar estado de ubicación", e)
            }
        }

        return if (missingPermissions.isEmpty()) {
            Result.success(emptyList())
        } else {
            Result.failure(SecurityException(
                "Permisos requeridos: ${missingPermissions.joinToString()}"
            ))
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        return validateScanPermissions().isSuccess
    }

    private fun isAirplaneModeOn(): Boolean {
        return Settings.Global.getInt(
            context.contentResolver,
            Settings.Global.AIRPLANE_MODE_ON, 0
        ) != 0
    }

    private fun updateConnectionState() {
        val state = getCurrentConnectionState()
        // Esta función es llamada desde callbackFlow, necesitamos acceso al trySend
        // Se maneja en los callbacks individuales
    }

    /**
     * Construye estado de conexión REAL con validación HTTP de internet.
     * Combinación de NetworkCapabilities + InternetChecker para máxima precisión.
     */
    private suspend fun buildConnectionStateWithInternetCheck(
        capabilities: NetworkCapabilities?, 
        network: Network
    ): ConnectionState {
        return try {
            val connectionInfo = wifiManager.connectionInfo
            
            if (connectionInfo?.ssid == null || connectionInfo.ssid == "<unknown ssid>") {
                return ConnectionState.Disconnected
            }

            val ssid = connectionInfo.ssid.replace("\"", "")
            val rssi = connectionInfo.rssi.takeIf { it != -127 }
            val linkSpeed = connectionInfo.linkSpeed

            // Obtener IP real
            val linkProperties = connectivityManager.getLinkProperties(network)
            val ipAddress = linkProperties?.linkAddresses
                ?.firstOrNull { it.address is Inet4Address }
                ?.address?.hostAddress
                ?: getWifiIpAddress()

            // Analizar capabilities
            val hasInternetCapability = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false
            val isValidatedBySystem = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ?: false
            val isWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false

            // InternetChecker REAL (timeout 3s máx)
            val hasRealInternet = if (isValidatedBySystem) {
                try {
                    kotlinx.coroutines.withTimeout(3000) {
                        InternetChecker.hasRealInternet()
                    }
                } catch (e: Exception) {
                    false
                }
            } else {
                false
            }

            // Estado REAL de internet (combinado)
            val finalInternetStatus = when {
                hasRealInternet -> com.example.wifiinsight.data.model.InternetStatus.VALIDATED
                hasInternetCapability -> com.example.wifiinsight.data.model.InternetStatus.UNVALIDATED
                else -> com.example.wifiinsight.data.model.InternetStatus.NONE
            }

            Log.d(TAG, "Estado REAL: WiFi=$isWifi, RealInternet=$hasRealInternet, SystemValidated=$isValidatedBySystem")

            when {
                !isWifi -> ConnectionState.Disconnected
                else -> ConnectionState.Connected(
                    ssid = ssid,
                    ipAddress = ipAddress,
                    linkSpeed = linkSpeed,
                    rssi = rssi,
                    hasInternet = hasInternetCapability || hasRealInternet,
                    isValidated = hasRealInternet,
                    internetStatus = finalInternetStatus
                )
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Error de permisos obteniendo estado", e)
            ConnectionState.Error("Permisos insuficientes")
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo estado de conexión", e)
            ConnectionState.Error("Error: ${e.message}")
        }
    }

    private fun getCurrentConnectionState(): ConnectionState {
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return if (network != null && capabilities != null) {
            buildConnectionState(capabilities, network)
        } else {
            ConnectionState.Disconnected
        }
    }

    override fun isWifiEnabled(): Boolean {
        return try {
            wifiManager.isWifiEnabled
        } catch (e: SecurityException) {
            Log.e(TAG, "Error verificando estado WiFi", e)
            false
        }
    }

    override fun setWifiEnabled(enabled: Boolean) {
        try {
            @Suppress("DEPRECATION")
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                wifiManager.isWifiEnabled = enabled
            } else {
                // Android 10+ no permite cambiar WiFi programáticamente
                Log.w(TAG, "Android 10+ requiere que el usuario active WiFi manualmente")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Error cambiando estado WiFi", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado cambiando WiFi", e)
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
        Log.d(TAG, "Historial de escaneo limpiado")
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
            Log.w(TAG, "Error obteniendo IP", e)
            null
        }
    }
}
