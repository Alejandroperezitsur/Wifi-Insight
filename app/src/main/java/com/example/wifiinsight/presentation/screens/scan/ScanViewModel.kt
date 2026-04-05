package com.example.wifiinsight.presentation.screens.scan

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.wifiinsight.data.model.WifiNetwork
import com.example.wifiinsight.data.repository.WifiRepository
import com.example.wifiinsight.domain.usecase.ScanWifiNetworksUseCase
import com.example.wifiinsight.domain.util.SystemSettingsHelper
import com.example.wifiinsight.domain.util.UserErrorMapper
import com.example.wifiinsight.domain.util.SignalCalculator.ActionDebouncer
import com.example.wifiinsight.data.repository.WifiRepositoryImpl
import com.example.wifiinsight.domain.util.WifiState
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

sealed class ScanUiState {
    data object Initial : ScanUiState()
    data object Loading : ScanUiState()
    data class Success(
        val networks: List<WifiNetwork>,
        val isScanning: Boolean = false
    ) : ScanUiState()
    data class Error(
        val message: String,
        val canRetry: Boolean = true
    ) : ScanUiState()
    data class Timeout(
        val message: String = "El escaneo está tardando más de lo normal"
    ) : ScanUiState()
}

class ScanViewModel(
    application: Application,
    private val repository: WifiRepository,
    private val scanWifiNetworksUseCase: ScanWifiNetworksUseCase,
    private val demoModeManager: DemoModeManager = DemoModeManager()
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<ScanUiState>(ScanUiState.Initial)
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    private val _isWifiEnabled = MutableStateFlow(false)
    val isWifiEnabled: StateFlow<Boolean> = _isWifiEnabled.asStateFlow()
    
    // FIX CRÍTICO: Exponer estado de demo mode
    val isDemoMode: StateFlow<Boolean> = demoModeManager.isDemoMode

    private var currentNetworks = listOf<WifiNetwork>()
    
    // FIX UX #9: Exponer tiempo restante de throttling
    private val _throttleRemainingSeconds = MutableStateFlow(0)
    val throttleRemainingSeconds: StateFlow<Int> = _throttleRemainingSeconds.asStateFlow()
    
    // FIX UX #8: Eventos de UI para feedback (Toast/Snackbar)
    private val _uiEvent = MutableStateFlow<String?>(null)
    val uiEvent: StateFlow<String?> = _uiEvent.asStateFlow()
    
    fun consumeUiEvent() {
        _uiEvent.value = null
    }
    
    // Timeout handling para UX FAIL-SAFE
    private var timeoutJob: Job? = null
    private val _scanAttemptCount = MutableStateFlow(0)
    val scanAttemptCount: StateFlow<Int> = _scanAttemptCount.asStateFlow()
    
    init {
        Log.d(TAG, "ScanViewModel inicializado")
        checkWifiState()
        startWifiStateMonitoring()
    }
    
    /**
     * Monitoreo REAL del estado WiFi usando BroadcastReceiver.
     */
    private fun startWifiStateMonitoring() {
        viewModelScope.launch {
            (repository as? WifiRepositoryImpl)?.wifiStateFlow?.collect { wifiState ->
                Log.d(TAG, "WiFi State actualizado: $wifiState")
                when (wifiState) {
                    is WifiState.Connected -> {
                        _isWifiEnabled.value = true
                    }
                    is WifiState.EnabledDisconnected -> {
                        _isWifiEnabled.value = true
                    }
                    is WifiState.Disabled -> {
                        _isWifiEnabled.value = false
                    }
                    is WifiState.AirplaneMode -> {
                        _isWifiEnabled.value = false
                    }
                    is WifiState.Error -> {
                        Log.w(TAG, "Error monitoreando WiFi: ${wifiState.message}")
                    }
                }
            }
        }
    }
    
    /**
     * Inicia countdown real de throttling
     */
    private fun startThrottleCountdown() {
        throttleJob?.cancel()
        val elapsed = (System.currentTimeMillis() - lastScanTime) / 1000
        val remaining = (SCAN_THROTTLE_SECONDS - elapsed).coerceAtLeast(0).toInt()
        _throttleRemainingSeconds.value = remaining
        
        if (remaining > 0) {
            throttleJob = viewModelScope.launch {
                while (isActive && _throttleRemainingSeconds.value > 0) {
                    delay(1000)
                    _throttleRemainingSeconds.value -= 1
                }
            }
        }
    }

    fun checkWifiState() {
        try {
            _isWifiEnabled.value = repository.isWifiEnabled()
            Log.d(TAG, "Estado WiFi verificado: ${_isWifiEnabled.value}")
        } catch (e: Exception) {
            Log.e(TAG, "Error verificando estado WiFi", e)
            _isWifiEnabled.value = false
        }
    }

    /**
     * Abre los ajustes de WiFi del sistema.
     * En Android 10+ esto es la única forma de "activar" WiFi ya que las apps no pueden cambiarlo programáticamente.
     * @return true si se pudo abrir la configuración
     */
    fun openWifiSettings(): Boolean {
        Log.d(TAG, "Abriendo ajustes de WiFi desde ScanScreen...")
        return try {
            val success = SystemSettingsHelper.openWifiSettings(getApplication())
            if (success) {
                Log.d(TAG, "✓ Ajustes de WiFi abiertos desde ScanScreen")
                // Re-verificar estado después de un delay
                viewModelScope.launch {
                    kotlinx.coroutines.delay(2000)
                    checkWifiState()
                    if (_isWifiEnabled.value) {
                        scanNetworks()
                    }
                }
            } else {
                Log.e(TAG, "✗ No se pudieron abrir los ajustes de WiFi")
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error abriendo ajustes WiFi", e)
            false
        }
    }

    /**
     * Legacy: Intenta activar WiFi directamente (solo funciona en Android 9 y menor)
     */
    fun enableWifiLegacy() {
        Log.w(TAG, "enableWifiLegacy llamado - solo funciona en Android 9-")
        try {
            repository.setWifiEnabled(true)
            viewModelScope.launch {
                kotlinx.coroutines.delay(1000)
                checkWifiState()
                if (_isWifiEnabled.value) {
                    scanNetworks()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en enableWifiLegacy", e)
        }
    }

    fun scanNetworks() {
        // FIX UX #8: Feedback cuando ya hay escaneo en progreso
        if (_uiState.value is ScanUiState.Loading) {
            Log.d(TAG, "Scan ya en progreso, enviando feedback al usuario")
            _uiEvent.value = "Escaneo en progreso... por favor espera"
            return
        }
        
        // FIX UX #9: Verificar y mostrar throttling
        if (_throttleRemainingSeconds.value > 0) {
            _uiEvent.value = "Reintentar en ${_throttleRemainingSeconds.value}s (límite de Android)"
            return
        }

        Log.d(TAG, "Iniciando escaneo de redes...")
        _scanAttemptCount.value += 1
        _uiState.value = ScanUiState.Loading
        
        // Iniciar timeout
        timeoutJob?.cancel()
        timeoutJob = viewModelScope.launch {
            delay(TIMEOUT_DURATION)
            if (_uiState.value is ScanUiState.Loading) {
                Log.w(TAG, "Timeout alcanzado - mostrando estado de timeout")
                _uiState.value = ScanUiState.Timeout()
            }
        }

        scanWifiNetworksUseCase()
            .onEach { result ->
                timeoutJob?.cancel() // Cancelar timeout si se completó
                result.fold(
                    onSuccess = { networks ->
                        Log.d(TAG, "✓ Escaneo completado: ${networks.size} redes encontradas")
                        currentNetworks = networks
                        _uiState.value = ScanUiState.Success(
                            networks = networks,
                            isScanning = false
                        )
                    },
                    onFailure = { error ->
                        Log.e(TAG, "✗ Error en escaneo: ${error.message}")
                        _uiState.value = ScanUiState.Error(
                            message = UserErrorMapper.map(error),
                            canRetry = true
                        )
                    }
                )
            }
            .catch { error ->
                timeoutJob?.cancel()
                Log.e(TAG, "✗ Error inesperado en escaneo", error)
                _uiState.value = ScanUiState.Error(
                    message = UserErrorMapper.map(error),
                    canRetry = true
                )
            }
            .launchIn(viewModelScope)
    }

    fun retryScan() {
        // FIX: Debounce para prevenir spam
        if (!scanDebouncer.canExecute()) {
            Log.d(TAG, "Retry ignorado - debounce activo")
            _uiEvent.value = "Por favor espera un momento antes de reintentar"
            return
        }
        
        Log.d(TAG, "Retry solicitado - intento #${_scanAttemptCount.value + 1}")
        scanNetworks()
    }

    override fun onCleared() {
        super.onCleared()
        timeoutJob?.cancel()
        // FIX #15: Limpiar DemoModeManager para evitar memory leaks
        demoModeManager.cleanup()
        Log.d(TAG, "ScanViewModel destruido - DemoModeManager limpiado")
    }

    companion object {
        private const val TAG = "ScanViewModel"
        private const val TIMEOUT_DURATION = 8000L // 8 segundos para escaneo
        private const val SCAN_THROTTLE_SECONDS = 30 // Throttling de Android
        
        fun provideFactory(
            application: Application,
            repository: WifiRepository,
            scanWifiNetworksUseCase: ScanWifiNetworksUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ScanViewModel(application, repository, scanWifiNetworksUseCase) as T
            }
        }
    }
    
    // FIX CRÍTICO: Throttling countdown real
    private var throttleJob: Job? = null
    private var lastScanTime = 0L
    
    // FIX: Debounce para acciones
    private val scanDebouncer = ActionDebouncer(1000L)
}
