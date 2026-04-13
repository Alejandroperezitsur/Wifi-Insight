package com.example.wifiinsight.data.repository

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.wifiinsight.data.model.InternetStatus
import com.example.wifiinsight.data.model.PermissionState
import com.example.wifiinsight.data.model.SystemDegradation
import com.example.wifiinsight.data.model.UiError
import com.example.wifiinsight.data.model.UserAction
import com.example.wifiinsight.data.model.WifiEvent
import com.example.wifiinsight.data.model.WifiNetwork
import com.example.wifiinsight.data.model.WifiState
import com.example.wifiinsight.data.reducer.WifiStateReducer
import com.example.wifiinsight.domain.util.ConnectionResult
import com.example.wifiinsight.domain.util.InternetChecker
import com.example.wifiinsight.domain.util.PermissionHandler
import com.example.wifiinsight.domain.util.SettingsHelper
import com.example.wifiinsight.domain.util.SystemSettingsHelper
import com.example.wifiinsight.domain.util.WifiConnector
import com.example.wifiinsight.domain.util.WifiDebugLogger
import com.example.wifiinsight.domain.util.WifiStateMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class WifiRepositoryImpl(
    private val context: Context,
    private val internetChecker: InternetChecker,
    private val wifiConnector: WifiConnector
) : WifiRepository {

    companion object {
        private const val TAG = "WifiRepository"
        private const val SCAN_THROTTLE_MS = 30_000L
        private const val SCAN_TIMEOUT_MS = 10_000L
        private const val INTERNET_TIMEOUT_MS = 5_000L
        private const val STOP_MONITORING_DELAY_MS = 5_000L
        private const val ACTION_TIMEOUT_MS = 3_000L
        private const val AUTO_RECOVERY_DELAY_MS = 1_500L
    }

    private data class SystemSnapshot(
        val wifiEnabled: Boolean,
        val isAirplaneMode: Boolean,
        val locationEnabled: Boolean,
        val permissionState: PermissionState,
        val serviceError: UiError? = null
    )

    private val appContext = context.applicationContext
    private val repositoryDispatcher = Dispatchers.IO.limitedParallelism(1)
    private val repositoryScope = CoroutineScope(SupervisorJob() + repositoryDispatcher)
    private val permissionHandler = PermissionHandler(appContext)
    private val preferences = appContext.getSharedPreferences("wifi_repository", Context.MODE_PRIVATE)
    private val scanMutex = Mutex()

    private val wifiManager: WifiManager? by lazy {
        appContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    }
    private val connectivityManager: ConnectivityManager? by lazy {
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    }

    private val _uiState = MutableStateFlow(WifiState())
    override val uiState: StateFlow<WifiState> = _uiState.asStateFlow()

    private var monitoringJobs: List<Job> = emptyList()
    private var monitoringStopJob: Job? = null
    private var lastScanElapsedTime = 0L
    private var emptyScanCount = 0
    private var hasAutoRetriedEmptyScan = false
    private var throttleJob: Job? = null
    private var internetCheckJob: Job? = null
    private var actionTimeoutJob: Job? = null
    private var autoRecoveryJob: Job? = null
    private var currentInternetTarget: String? = null
    private var nextActionToken = 0L

    init {
        observeUiSubscriptions()
        refreshSystemState()
    }

    override suspend fun scanNetworks() {
        withContext(repositoryDispatcher) {
            scanMutex.withLock {
                val actionToken = beginAction(UserAction.Scan)
                val snapshot = readSystemSnapshot()
                applySystemSnapshot(snapshot)

                when {
                    snapshot.serviceError != null -> {
                        dispatch(WifiEvent.ScanFailed(snapshot.serviceError.message))
                        finishAction(UserAction.Scan, actionToken)
                        return@withContext
                    }

                    snapshot.isAirplaneMode -> {
                        dispatchError(
                            title = "Modo avión activado",
                            message = "Desactiva el modo avión para escanear redes WiFi."
                        )
                        finishAction(UserAction.Scan, actionToken)
                        return@withContext
                    }

                    !snapshot.wifiEnabled -> {
                        dispatchError(
                            title = "WiFi desactivado",
                            message = "Abre ajustes WiFi para activar la red inalámbrica."
                        )
                        finishAction(UserAction.Scan, actionToken)
                        return@withContext
                    }

                    !snapshot.locationEnabled -> {
                        dispatchError(
                            title = "Ubicación desactivada",
                            message = "Activa la ubicación para escanear redes WiFi."
                        )
                        finishAction(UserAction.Scan, actionToken)
                        return@withContext
                    }

                    snapshot.permissionState != PermissionState.Granted -> {
                        dispatchError(
                            title = "Permisos requeridos",
                            message = "Permite los permisos necesarios para escanear redes WiFi."
                        )
                        finishAction(UserAction.Scan, actionToken)
                        return@withContext
                    }
                }

                val elapsed = SystemClock.elapsedRealtime() - lastScanElapsedTime
                if (elapsed < SCAN_THROTTLE_MS) {
                    val remaining = (SCAN_THROTTLE_MS - elapsed).coerceAtLeast(1L)
                    startThrottleCountdown(remaining)
                    dispatchError(
                        title = "Escaneo limitado",
                        message = "Puedes escanear en ${remaining / 1000L}s."
                    )
                    finishAction(UserAction.Scan, actionToken)
                    return@withContext
                }

                dispatch(WifiEvent.ScanStarted)
                stopThrottleCountdown()

                try {
                    val results = performRealScan()
                    lastScanElapsedTime = SystemClock.elapsedRealtime()
                    if (results.isEmpty()) {
                        emptyScanCount++
                        scheduleAutoRecoveryIfNeeded()
                    } else {
                        emptyScanCount = 0
                        hasAutoRetriedEmptyScan = false
                        autoRecoveryJob?.cancel()
                    }
                    dispatch(
                        WifiEvent.ScanCompleted(
                            results = results,
                            completedAtElapsedMs = lastScanElapsedTime
                        )
                    )
                    dispatch(
                        WifiEvent.SystemDegraded(
                            if (emptyScanCount >= 3) {
                                SystemDegradation.ScanBlockedBySystem
                            } else {
                                SystemDegradation.None
                            }
                        )
                    )
                    if (emptyScanCount >= 5) {
                        repositoryScope.launch {
                            softResetInternal(triggeredBySystem = true)
                        }
                    }
                    dispatch(WifiEvent.ThrottleUpdated(0L))
                } catch (error: Exception) {
                    Log.e(TAG, "scanNetworks failed", error)
                    dispatch(
                        WifiEvent.ScanFailed(
                            error.message ?: "No se pudo completar el escaneo de redes."
                        )
                    )
                } finally {
                    finishAction(UserAction.Scan, actionToken)
                }
            }
        }
    }

    override suspend fun reEvaluateConnection() {
        withContext(repositoryDispatcher) {
            val actionToken = beginAction(UserAction.RefreshConnection)
            dispatch(WifiEvent.ConnectionRefreshStarted)
            try {
                internetChecker.invalidateCache()
                refreshSystemStateInternal(forceInternetCheck = true)
            } finally {
                dispatch(WifiEvent.ConnectionRefreshFinished)
                finishAction(UserAction.RefreshConnection, actionToken)
            }
        }
    }

    override suspend fun connectToNetwork(network: WifiNetwork, password: String?): Result<String> {
        Log.i(TAG, "Connecting to network: ${network.ssid} (${network.securityType})")

        return try {
            when (val result = wifiConnector.connectToNetwork(network, password)) {
                is ConnectionResult.Success -> {
                    Log.i(TAG, "✓ Connected to ${network.ssid}: ${result.message}")
                    Result.success(result.message)
                }
                is ConnectionResult.Error -> {
                    Log.w(TAG, "✗ Failed to connect to ${network.ssid}: ${result.message}")
                    Result.failure(Exception(result.message))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to network", e)
            Result.failure(e)
        }
    }

    override fun refreshSystemState(activity: Activity?) {
        repositoryScope.launch {
            refreshSystemStateInternal(
                activity = activity,
                forceInternetCheck = true
            )
        }
    }

    override fun refreshPermissions(activity: Activity?) {
        applySystemSnapshot(readSystemSnapshot(activity))
    }

    override fun softReset() {
        repositoryScope.launch {
            softResetInternal(triggeredBySystem = false)
        }
    }

    override fun markPermissionRequested() {
        preferences.edit().putBoolean("permission_requested", true).apply()
    }

    override fun clearError() {
        dispatch(WifiEvent.ErrorCleared)
    }

    override fun openWifiSettings(): Boolean = SystemSettingsHelper.openWifiSettings(appContext)

    override fun openAppSettings(): Boolean = SystemSettingsHelper.openAppSettings(appContext)

    override fun openLocationSettings(): Boolean = SystemSettingsHelper.openLocationSettings(appContext)

    override fun getNetworkByBssid(bssid: String): WifiNetwork? {
        return _uiState.value.networks.firstOrNull { it.bssid == bssid }
    }

    private fun observeUiSubscriptions() {
        repositoryScope.launch {
            _uiState.subscriptionCount
                .map { it > 0 }
                .distinctUntilChanged()
                .collectLatest { hasSubscribers ->
                    if (hasSubscribers) {
                        monitoringStopJob?.cancel()
                        startMonitoringIfNeeded()
                        refreshSystemStateInternal(forceInternetCheck = true)
                    } else {
                        scheduleMonitoringStop()
                    }
                }
        }
    }

    private fun scheduleMonitoringStop() {
        monitoringStopJob?.cancel()
        monitoringStopJob = repositoryScope.launch {
            delay(STOP_MONITORING_DELAY_MS)
            stopMonitoring()
        }
    }

    private fun startMonitoringIfNeeded() {
        if (monitoringJobs.isNotEmpty()) {
            return
        }
        monitoringJobs = startMonitoring()
    }

    private fun stopMonitoring() {
        monitoringJobs.forEach(Job::cancel)
        monitoringJobs = emptyList()
        internetCheckJob?.cancel()
    }

    private fun readSystemSnapshot(activity: Activity? = null): SystemSnapshot {
        return try {
            val wifiService = wifiManager
            val permissionWasRequested = preferences.getBoolean("permission_requested", false)
            val permissionState = when {
                permissionHandler.hasAllPermissions() -> PermissionState.Granted
                activity != null && permissionWasRequested && permissionHandler.isPermanentlyDenied(activity) -> {
                    PermissionState.PermanentlyDenied
                }
                else -> PermissionState.Denied
            }

            SystemSnapshot(
                wifiEnabled = wifiService?.isWifiEnabled == true,
                isAirplaneMode = SettingsHelper.isAirplaneModeOn(appContext),
                locationEnabled = SettingsHelper.isLocationEnabled(appContext),
                permissionState = permissionState,
                serviceError = if (wifiService == null) {
                    UiError(
                        title = "Servicio WiFi no disponible",
                        message = "El sistema no permitió acceder al servicio WiFi."
                    )
                } else {
                    null
                }
            )
        } catch (error: SecurityException) {
            SystemSnapshot(
                wifiEnabled = false,
                isAirplaneMode = SettingsHelper.isAirplaneModeOn(appContext),
                locationEnabled = SettingsHelper.isLocationEnabled(appContext),
                permissionState = PermissionState.Denied,
                serviceError = UiError(
                    title = "Permisos requeridos",
                    message = "No fue posible leer el estado WiFi por falta de permisos."
                )
            )
        } catch (error: Exception) {
            SystemSnapshot(
                wifiEnabled = false,
                isAirplaneMode = false,
                locationEnabled = SettingsHelper.isLocationEnabled(appContext),
                permissionState = PermissionState.Denied,
                serviceError = UiError(
                    title = "Error del sistema",
                    message = "No fue posible leer el estado actual del dispositivo."
                )
            )
        }
    }

    private fun applySystemSnapshot(snapshot: SystemSnapshot) {
        dispatch(WifiEvent.WifiToggled(snapshot.wifiEnabled))
        dispatch(WifiEvent.AirplaneModeChanged(snapshot.isAirplaneMode))
        dispatch(WifiEvent.LocationStateChanged(snapshot.locationEnabled))
        dispatch(WifiEvent.PermissionUpdated(snapshot.permissionState))
        snapshot.serviceError?.let { error ->
            dispatch(WifiEvent.ErrorOccurred(error))
        }
    }

    private fun refreshSystemStateInternal(
        activity: Activity? = null,
        forceInternetCheck: Boolean
    ) {
        val snapshot = readSystemSnapshot(activity)
        applySystemSnapshot(snapshot)

        if (snapshot.serviceError != null) {
            dispatch(WifiEvent.ConnectionChanged(null, null, null, null, null, false))
            dispatch(WifiEvent.InternetChecked(InternetStatus.UNKNOWN))
            return
        }

        updateConnectionState(forceInternetCheck = forceInternetCheck)
    }

    private fun startMonitoring(): List<Job> {
        val jobs = mutableListOf<Job>()

        jobs += repositoryScope.launch {
            try {
                WifiStateMonitor(appContext).wifiStateFlow
                    .distinctUntilChanged()
                    .collectLatest { wifiState ->
                        when (wifiState) {
                            is com.example.wifiinsight.domain.util.WifiState.Connected -> {
                                dispatch(WifiEvent.AirplaneModeChanged(false))
                                dispatch(WifiEvent.WifiToggled(true))
                                updateConnectionState(forceInternetCheck = false)
                            }

                            is com.example.wifiinsight.domain.util.WifiState.EnabledDisconnected -> {
                                dispatch(WifiEvent.AirplaneModeChanged(false))
                                dispatch(WifiEvent.WifiToggled(true))
                                dispatch(WifiEvent.ConnectionChanged(null, null, null, null, null, false))
                                dispatch(WifiEvent.InternetChecked(InternetStatus.UNKNOWN))
                            }

                            is com.example.wifiinsight.domain.util.WifiState.Disabled -> {
                                dispatch(WifiEvent.AirplaneModeChanged(false))
                                dispatch(WifiEvent.WifiToggled(false))
                                dispatch(WifiEvent.ConnectionChanged(null, null, null, null, null, false))
                                dispatch(WifiEvent.InternetChecked(InternetStatus.UNKNOWN))
                            }

                            is com.example.wifiinsight.domain.util.WifiState.AirplaneMode -> {
                                dispatch(WifiEvent.AirplaneModeChanged(true))
                                dispatch(WifiEvent.ConnectionChanged(null, null, null, null, null, false))
                                dispatch(WifiEvent.InternetChecked(InternetStatus.UNKNOWN))
                            }

                            is com.example.wifiinsight.domain.util.WifiState.Error -> {
                                dispatchError("Estado WiFi inválido", wifiState.message)
                            }
                        }
                    }
            } catch (error: SecurityException) {
                dispatchError("Permisos insuficientes", "No fue posible seguir monitoreando el WiFi.")
            } catch (error: Exception) {
                Log.e(TAG, "WifiStateMonitor failed", error)
            }
        }

        jobs += repositoryScope.launch {
            try {
                observeNetworkChanges()
                    .conflate()
                    .collectLatest {
                        updateConnectionState(forceInternetCheck = false)
                    }
            } catch (error: SecurityException) {
                dispatchError("Permisos insuficientes", "No fue posible seguir observando cambios de red.")
            } catch (error: Exception) {
                Log.e(TAG, "observeNetworkChanges failed", error)
            }
        }

        jobs += repositoryScope.launch {
            try {
                observeSignalChanges()
                    .conflate()
                    .collectLatest { rssi ->
                        if (_uiState.value.isConnected) {
                            dispatch(WifiEvent.SignalUpdated(rssi))
                        }
                    }
            } catch (error: SecurityException) {
                dispatchError("Permisos insuficientes", "No fue posible seguir observando la señal WiFi.")
            } catch (error: Exception) {
                Log.e(TAG, "observeSignalChanges failed", error)
            }
        }

        jobs += repositoryScope.launch {
            try {
                observeAirplaneMode()
                    .distinctUntilChanged()
                    .collectLatest { enabled ->
                        dispatch(WifiEvent.AirplaneModeChanged(enabled))
                        if (enabled) {
                            dispatch(WifiEvent.ConnectionChanged(null, null, null, null, null, false))
                            dispatch(WifiEvent.InternetChecked(InternetStatus.UNKNOWN))
                        } else {
                            updateConnectionState(forceInternetCheck = true)
                        }
                    }
            } catch (error: Exception) {
                Log.e(TAG, "observeAirplaneMode failed", error)
            }
        }

        jobs += repositoryScope.launch {
            try {
                observeLocationState()
                    .distinctUntilChanged()
                    .collectLatest { enabled ->
                        dispatch(WifiEvent.LocationStateChanged(enabled))
                    }
            } catch (error: Exception) {
                Log.e(TAG, "observeLocationState failed", error)
            }
        }

        return jobs
    }

    private fun dispatch(event: WifiEvent) {
        val previousState = _uiState.value
        _uiState.update { currentState ->
            WifiStateReducer.reduce(currentState, event)
        }
        logStateEvent(previousState, event, _uiState.value)
    }

    private fun dispatchError(title: String, message: String) {
        dispatch(
            WifiEvent.ErrorOccurred(
                UiError(
                    title = title,
                    message = message,
                    isRecoverable = true
                )
            )
        )
    }

    private fun beginAction(action: UserAction): Long {
        nextActionToken += 1
        val token = nextActionToken
        dispatch(WifiEvent.ActionStarted(action, token))
        startActionTimeout(action, token)
        return token
    }

    private fun finishAction(action: UserAction, token: Long) {
        actionTimeoutJob?.cancel()
        dispatch(WifiEvent.ActionFinished(action, token))
    }

    private fun startActionTimeout(action: UserAction, token: Long) {
        actionTimeoutJob?.cancel()
        actionTimeoutJob = repositoryScope.launch {
            delay(ACTION_TIMEOUT_MS)
            dispatch(WifiEvent.ActionTimeout(action, token))
        }
    }

    private suspend fun softResetInternal(triggeredBySystem: Boolean) {
        withContext(repositoryDispatcher) {
            val currentState = _uiState.value
            emptyScanCount = 0
            hasAutoRetriedEmptyScan = false
            autoRecoveryJob?.cancel()
            actionTimeoutJob?.cancel()
            throttleJob?.cancel()
            internetCheckJob?.cancel()
            currentInternetTarget = null
            if (triggeredBySystem) {
                WifiDebugLogger.log("FORCED_RESET")
            } else {
                WifiDebugLogger.log("SOFT_RESET")
            }
            _uiState.update {
                WifiState(
                    wifiEnabled = currentState.wifiEnabled,
                    isAirplaneMode = currentState.isAirplaneMode,
                    permissionState = currentState.permissionState,
                    locationEnabled = currentState.locationEnabled
                )
            }
            refreshSystemStateInternal(forceInternetCheck = true)
            val refreshedState = _uiState.value
            if (refreshedState.canScan &&
                refreshedState.blockingState == null &&
                !refreshedState.isScanning
            ) {
                scanNetworks()
            }
        }
    }

    private fun scheduleAutoRecoveryIfNeeded() {
        if (hasAutoRetriedEmptyScan || emptyScanCount <= 0) {
            return
        }
        hasAutoRetriedEmptyScan = true
        autoRecoveryJob?.cancel()
        autoRecoveryJob = repositoryScope.launch {
            delay(AUTO_RECOVERY_DELAY_MS)
            val current = _uiState.value
            if (current.networks.isEmpty() &&
                current.canScan &&
                !current.isScanning &&
                current.blockingState == null
            ) {
                WifiDebugLogger.log("AUTO_RECOVERY_SCAN")
                scanNetworks()
            }
        }
    }

    private fun logStateEvent(
        previousState: WifiState,
        event: WifiEvent,
        currentState: WifiState
    ) {
        when (event) {
            is WifiEvent.ScanStarted -> WifiDebugLogger.log("SCAN_STARTED")
            is WifiEvent.ScanCompleted -> if (event.results.isEmpty()) {
                WifiDebugLogger.log("SCAN_EMPTY")
            }
            is WifiEvent.SystemDegraded -> if (
                event.degradation == SystemDegradation.ScanBlockedBySystem &&
                previousState.systemDegradation != currentState.systemDegradation
            ) {
                WifiDebugLogger.log("OEM_BLOCK_DETECTED")
            }
            is WifiEvent.PermissionUpdated -> if (
                event.state != PermissionState.Granted &&
                previousState.permissionState != event.state
            ) {
                WifiDebugLogger.log("PERMISSION_DENIED")
            }
            is WifiEvent.ActionTimeout -> if (
                previousState.activeActionToken == event.token &&
                currentState.errorQueue.firstOrNull()?.message == "Tardó demasiado. Intenta de nuevo"
            ) {
                WifiDebugLogger.log("ACTION_TIMEOUT_${event.action.name}")
            }
            is WifiEvent.ScanFailed -> WifiDebugLogger.log("SCAN_FAILED")
            else -> Unit
        }
    }

    private fun updateConnectionState(forceInternetCheck: Boolean) {
        try {
            val connectivityService = connectivityManager ?: run {
                dispatchError(
                    title = "Servicio de conectividad no disponible",
                    message = "No fue posible consultar el estado actual de la red."
                )
                dispatch(WifiEvent.ConnectionChanged(null, null, null, null, null, false))
                dispatch(WifiEvent.InternetChecked(InternetStatus.UNKNOWN))
                return
            }
            val wifiService = wifiManager ?: run {
                dispatchError(
                    title = "Servicio WiFi no disponible",
                    message = "No fue posible consultar la conexión WiFi actual."
                )
                dispatch(WifiEvent.ConnectionChanged(null, null, null, null, null, false))
                dispatch(WifiEvent.InternetChecked(InternetStatus.UNKNOWN))
                return
            }

            val activeNetwork = connectivityService.activeNetwork ?: run {
                currentInternetTarget = null
                internetCheckJob?.cancel()
                dispatch(WifiEvent.ConnectionChanged(null, null, null, null, null, false))
                dispatch(WifiEvent.InternetChecked(InternetStatus.UNKNOWN))
                return
            }

            val capabilities = connectivityService.getNetworkCapabilities(activeNetwork) ?: run {
                currentInternetTarget = null
                internetCheckJob?.cancel()
                dispatch(WifiEvent.ConnectionChanged(null, null, null, null, null, false))
                dispatch(WifiEvent.InternetChecked(InternetStatus.UNKNOWN))
                return
            }

            if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                currentInternetTarget = null
                internetCheckJob?.cancel()
                dispatch(WifiEvent.ConnectionChanged(null, null, null, null, null, false))
                dispatch(WifiEvent.InternetChecked(InternetStatus.UNKNOWN))
                return
            }

            val connectionInfo = wifiService.connectionInfo
            val rawSsid = connectionInfo?.ssid?.replace("\"", "").orEmpty()
            val displaySsid = rawSsid.takeUnless { it.isBlank() || it == "<unknown ssid>" }
                ?: "<Red desconocida>"
            val bssid = connectionInfo?.bssid.orEmpty()
            val rssi = connectionInfo?.rssi?.takeIf { it != -127 }
            val linkSpeed = connectionInfo?.linkSpeed?.takeIf { it > 0 }

            val linkProperties = connectivityService.getLinkProperties(activeNetwork)
            val ipAddress = linkProperties?.linkAddresses
                ?.firstOrNull { it.address is java.net.Inet4Address }
                ?.address
                ?.hostAddress

            val hasInternetCapability =
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

            dispatch(
                WifiEvent.ConnectionChanged(
                    ssid = displaySsid,
                    bssid = bssid,
                    ipAddress = ipAddress,
                    linkSpeed = linkSpeed,
                    rssi = rssi,
                    hasInternetCapability = hasInternetCapability
                )
            )

            when {
                !hasInternetCapability -> {
                    currentInternetTarget = bssid.ifBlank { displaySsid }
                    internetCheckJob?.cancel()
                    dispatch(WifiEvent.InternetChecked(InternetStatus.UNAVAILABLE))
                }

                else -> startInternetCheck(
                    target = bssid.ifBlank { displaySsid },
                    forceInternetCheck = forceInternetCheck,
                    validatedHint = capabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_VALIDATED
                    )
                )
            }
        } catch (error: SecurityException) {
            Log.e(TAG, "updateConnectionState security error", error)
            dispatchError(
                title = "Permisos insuficientes",
                message = "No fue posible leer el estado actual del WiFi."
            )
        } catch (error: Exception) {
            Log.e(TAG, "updateConnectionState error", error)
            dispatchError(
                title = "Error de conexión",
                message = "No fue posible re-evaluar la conexión actual."
            )
        }
    }

    private fun startInternetCheck(
        target: String,
        forceInternetCheck: Boolean,
        validatedHint: Boolean
    ) {
        if (!forceInternetCheck && currentInternetTarget == target &&
            _uiState.value.internetStatus == InternetStatus.CHECKING
        ) {
            return
        }

        currentInternetTarget = target
        internetCheckJob?.cancel()
        dispatch(WifiEvent.InternetCheckStarted)

        internetCheckJob = repositoryScope.launch {
            val hasInternet = try {
                withTimeout(INTERNET_TIMEOUT_MS) {
                    if (forceInternetCheck) {
                        internetChecker.forceCheck()
                    } else {
                        internetChecker.hasRealInternet()
                    }
                }
            } catch (error: Exception) {
                false
            }

            val activeTarget = _uiState.value.bssid.orEmpty().ifBlank {
                _uiState.value.ssid.orEmpty()
            }
            if (activeTarget == target) {
                if (!hasInternet && validatedHint) {
                    WifiDebugLogger.log("INTERNET_FALSE_POSITIVE")
                }
                dispatch(
                    WifiEvent.InternetChecked(
                        if (hasInternet) InternetStatus.AVAILABLE else InternetStatus.UNAVAILABLE
                    )
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun performRealScan(): List<WifiNetwork> {
        return suspendCancellableCoroutine { continuation ->
            val wifiService = wifiManager
            if (wifiService == null) {
                continuation.resumeWithException(
                    IllegalStateException("El servicio WiFi no está disponible en este momento.")
                )
                return@suspendCancellableCoroutine
            }

            var receiver: BroadcastReceiver? = null
            var timeoutJob: Job? = null

            fun cleanup() {
                receiver?.let { safeUnregister(it) }
                timeoutJob?.cancel()
            }

            try {
                receiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        try {
                            if (intent?.action != WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                                return
                            }

                            val results = wifiService.scanResults
                                .orEmpty()
                                .map { WifiNetwork.fromScanResult(it) }
                                .distinctBy { network ->
                                    network.bssid.ifBlank { "${network.ssid}-${network.frequency}" }
                                }
                                .let(::sortScanResults)

                            cleanup()
                            if (continuation.isActive) {
                                continuation.resume(results)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error in scan onReceive", e)
                            cleanup()
                            if (continuation.isActive) {
                                continuation.resume(emptyList())
                            }
                        }
                    }
                }

                registerReceiverCompat(
                    receiver,
                    IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
                )

                timeoutJob = repositoryScope.launch {
                    delay(SCAN_TIMEOUT_MS)
                    cleanup()
                    if (continuation.isActive) {
                        continuation.resume(emptyList())
                    }
                }

                val scanStarted = wifiService.startScan()
                if (!scanStarted) {
                    cleanup()
                    continuation.resumeWithException(
                        IllegalStateException(
                            "Android limitó el escaneo. Espera unos segundos antes de intentar otra vez."
                        )
                    )
                }
            } catch (error: SecurityException) {
                cleanup()
                continuation.resumeWithException(
                    SecurityException("Permisos requeridos para escanear redes WiFi.")
                )
            } catch (error: Exception) {
                cleanup()
                continuation.resumeWithException(error)
            }

            continuation.invokeOnCancellation {
                cleanup()
            }
        }
    }

    private fun sortScanResults(results: List<WifiNetwork>): List<WifiNetwork> {
        val previousOrder = _uiState.value.networks
            .mapIndexed { index, network ->
                network.bssid.ifBlank { "${network.ssid}-${network.frequency}" } to index
            }
            .toMap()

        return if (previousOrder.isEmpty()) {
            results.sortedByDescending { it.rssi }
        } else {
            results.sortedWith(
                compareBy<WifiNetwork> {
                    previousOrder[it.bssid.ifBlank { "${it.ssid}-${it.frequency}" }] ?: Int.MAX_VALUE
                }.thenByDescending { it.rssi }
            )
        }
    }

    private fun startThrottleCountdown(remainingMs: Long) {
        throttleJob?.cancel()
        throttleJob = repositoryScope.launch {
            var remaining = remainingMs
            while (remaining > 0L) {
                dispatch(WifiEvent.ThrottleUpdated(remaining))
                delay(1_000L)
                remaining = (remaining - 1_000L).coerceAtLeast(0L)
            }
            dispatch(WifiEvent.ThrottleUpdated(0L))
        }
    }

    private fun stopThrottleCountdown() {
        throttleJob?.cancel()
        throttleJob = null
        dispatch(WifiEvent.ThrottleUpdated(0L))
    }

    private fun observeSignalChanges() = callbackFlow {
        val connectivityService = connectivityManager
        val wifiService = wifiManager
        if (connectivityService == null || wifiService == null) {
            close()
            return@callbackFlow
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                val rssi = wifiService.connectionInfo?.rssi?.takeIf { it != -127 }
                if (rssi != null) {
                    trySend(rssi)
                }
            }
        }

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        try {
            connectivityService.registerNetworkCallback(request, callback)
        } catch (error: SecurityException) {
            close(error)
            return@callbackFlow
        } catch (error: Exception) {
            close(error)
            return@callbackFlow
        }

        awaitClose {
            safeUnregister(callback)
        }
    }

    private fun observeNetworkChanges() = callbackFlow {
        val connectivityService = connectivityManager
        if (connectivityService == null) {
            close()
            return@callbackFlow
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(Unit)
            }

            override fun onLost(network: Network) {
                trySend(Unit)
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                trySend(Unit)
            }
        }

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        try {
            connectivityService.registerNetworkCallback(request, callback)
        } catch (error: SecurityException) {
            close(error)
            return@callbackFlow
        } catch (error: Exception) {
            close(error)
            return@callbackFlow
        }

        awaitClose {
            safeUnregister(callback)
        }
    }

    private fun observeAirplaneMode() = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_AIRPLANE_MODE_CHANGED) {
                    trySend(SettingsHelper.isAirplaneModeOn(appContext))
                }
            }
        }

        registerReceiverCompat(receiver, IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED))
        trySend(SettingsHelper.isAirplaneModeOn(appContext))

        awaitClose {
            safeUnregister(receiver)
        }
    }

    private fun observeLocationState() = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                trySend(SettingsHelper.isLocationEnabled(appContext))
            }
        }

        val filter = IntentFilter().apply {
            addAction(LocationManager.PROVIDERS_CHANGED_ACTION)
            addAction(LocationManager.MODE_CHANGED_ACTION)
        }
        registerReceiverCompat(receiver, filter)
        trySend(SettingsHelper.isLocationEnabled(appContext))

        awaitClose {
            safeUnregister(receiver)
        }
    }

    private fun registerReceiverCompat(receiver: BroadcastReceiver, filter: IntentFilter) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For system broadcasts on API 34 (Android 14) we MUST use RECEIVER_EXPORTED
            // as it was the previous default behavior and system processes need to reach us.
            appContext.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            appContext.registerReceiver(receiver, filter)
        }
    }

    private fun safeUnregister(receiver: BroadcastReceiver) {
        try {
            appContext.unregisterReceiver(receiver)
        } catch (_: Exception) {
        }
    }

    private fun safeUnregister(callback: ConnectivityManager.NetworkCallback) {
        try {
            connectivityManager?.unregisterNetworkCallback(callback)
        } catch (_: Exception) {
        }
    }

}
