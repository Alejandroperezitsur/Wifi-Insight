package com.example.wifiinsight.presentation.screens.home

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.wifiinsight.data.model.ConnectionState
import com.example.wifiinsight.data.repository.WifiRepository
import com.example.wifiinsight.domain.usecase.GetCurrentConnectionUseCase
import com.example.wifiinsight.domain.usecase.MonitorSignalUseCase
import com.example.wifiinsight.domain.util.SystemSettingsHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import java.util.LinkedList

import com.example.wifiinsight.domain.util.UserErrorMapper
import com.example.wifiinsight.domain.util.SignalCalculator.ActionDebouncer
import kotlinx.coroutines.isActive
import com.example.wifiinsight.domain.util.SignalCalculator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed class HomeUiState {
    data object Loading : HomeUiState()
    data class Success(
        val connectionState: ConnectionState,
        val signalHistory: List<Int>,
        val isRefreshing: Boolean = false
    ) : HomeUiState()
    data class Error(
        val message: String,
        val canRetry: Boolean = true
    ) : HomeUiState()
    data class Timeout(
        val message: String = "La operación está tardando más de lo normal"
    ) : HomeUiState()
}

class HomeViewModel(
    application: Application,
    private val repository: WifiRepository,
    private val getCurrentConnectionUseCase: GetCurrentConnectionUseCase,
    private val monitorSignalUseCase: MonitorSignalUseCase,
    private val demoModeManager: DemoModeManager = DemoModeManager()
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _isWifiEnabled = MutableStateFlow(false)
    val isWifiEnabled: StateFlow<Boolean> = _isWifiEnabled.asStateFlow()
    
    // Exponer estado de demo mode
    val isDemoMode: StateFlow<Boolean> = demoModeManager.isDemoMode

    private val signalHistory = LinkedList<Int>()
    private val maxHistorySize = 50
    
    // Timeout handling para UX FAIL-SAFE
    private var timeoutJob: Job? = null
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    
    // FIX CRÍTICO: Trackear SSID para limpiar historial al cambiar de red
    private var lastSsid: String? = null
    
    // FIX: Debounce para prevenir spam de acciones
    private val refreshDebouncer = ActionDebouncer(1000L)
    
    // FIX: Monitoreo REAL del estado WiFi (BroadcastReceiver)
    init {
        Log.d(TAG, "ViewModel inicializado")
        startWifiStateMonitoring()
        startMonitoring()
    }
    
    /**
     * Monitoreo REAL del estado WiFi usando BroadcastReceiver.
     * Se actualiza instantáneamente cuando el usuario activa/desactiva WiFi.
     */
    private fun startWifiStateMonitoring() {
        viewModelScope.launch {
            (repository as? WifiRepositoryImpl)?.wifiStateFlow?.collect { wifiState ->
                Log.d(TAG, "WiFi State actualizado: $wifiState")
                when (wifiState) {
                    is WifiState.Connected -> {
                        _isWifiEnabled.value = true
                        // Trigger refresh when WiFi becomes available
                        if (_uiState.value is HomeUiState.Error || _uiState.value is HomeUiState.Loading) {
                            retryConnection(force = true)
                        }
                    }
                    is WifiState.EnabledDisconnected -> {
                        _isWifiEnabled.value = true
                    }
                    is WifiState.Disabled -> {
                        _isWifiEnabled.value = false
                        _uiState.value = HomeUiState.Error(
                            message = UserErrorMapper.Messages.WIFI_DISABLED,
                            canRetry = true
                        )
                    }
                    is WifiState.AirplaneMode -> {
                        _isWifiEnabled.value = false
                        _uiState.value = HomeUiState.Error(
                            message = UserErrorMapper.Messages.AIRPLANE_MODE,
                            canRetry = true
                        )
                    }
                    is WifiState.Error -> {
                        // Mantener estado anterior pero loggear error
                        Log.w(TAG, "Error monitoreando WiFi: ${wifiState.message}")
                    }
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
        Log.d(TAG, "Abriendo ajustes de WiFi...")
        return try {
            val success = SystemSettingsHelper.openWifiSettings(getApplication())
            if (success) {
                Log.d(TAG, "✓ Ajustes de WiFi abiertos correctamente")
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
     * Retry manual con timeout controlado y debounce
     */
    fun retryConnection(force: Boolean = false) {
        // FIX: Debounce para prevenir spam
        if (!force && !refreshDebouncer.canExecute()) {
            Log.d(TAG, "Retry ignorado - debounce activo")
            return
        }
        
        Log.d(TAG, "Retry solicitado por usuario")
        timeoutJob?.cancel()
        _uiState.value = HomeUiState.Loading
        startMonitoringWithTimeout()
    }
    
    /**
     * Inicia monitoreo con timeout automático
     */
    private fun startMonitoringWithTimeout() {
        startMonitoring()
        
        // Iniciar timeout
        timeoutJob?.cancel()
        timeoutJob = viewModelScope.launch {
            delay(TIMEOUT_DURATION)
            if (_uiState.value is HomeUiState.Loading) {
                Log.w(TAG, "Timeout alcanzado - mostrando estado de timeout")
                _uiState.value = HomeUiState.Timeout()
            }
        }
    }
    
    /**
     * Refresh manual REAL - sin delays fake
     */
    fun refreshConnection() {
        // FIX: Debounce para prevenir spam
        if (!refreshDebouncer.canExecute()) {
            Log.d(TAG, "Refresh ignorado - debounce activo")
            return
        }
        
        if (_isRefreshing.value) return
        
        Log.d(TAG, "Refresh iniciado")
        _isRefreshing.value = true
        
        // Actualizar estado actual si es Success
        _uiState.update { current ->
            if (current is HomeUiState.Success) {
                current.copy(isRefreshing = true)
            } else current
        }
        
        // FIX: Sin delay artificial - actualizar inmediatamente con datos reales
        viewModelScope.launch {
            checkWifiState()
            _isRefreshing.value = false
            
            _uiState.update { current ->
                if (current is HomeUiState.Success) {
                    current.copy(isRefreshing = false)
                } else current
            }
        }
    }

    private fun startMonitoring() {
        Log.d(TAG, "Iniciando monitoreo de conexión...")
        
        getCurrentConnectionUseCase()
            .onEach { connectionState ->
                Log.d(TAG, "Estado de conexión actualizado: $connectionState")
                
                // FIX CRÍTICO #3: Limpiar historial si cambió el SSID
                val newSsid = (connectionState as? ConnectionState.Connected)?.ssid
                if (newSsid != lastSsid) {
                    Log.d(TAG, "Cambio de red detectado: $lastSsid -> $newSsid, limpiando historial")
                    SignalCalculator.clearHistory()
                    signalHistory.clear()
                    lastSsid = newSsid
                }
                
                _uiState.update { currentState ->
                    val currentHistory = if (currentState is HomeUiState.Success) {
                        currentState.signalHistory
                    } else {
                        emptyList()
                    }
                    HomeUiState.Success(
                        connectionState = connectionState,
                        signalHistory = currentHistory
                    )
                }
            }
            .catch { error ->
                Log.e(TAG, "Error en monitoreo de conexión", error)
                _uiState.value = HomeUiState.Error(UserErrorMapper.map(error))
            }
            .launchIn(viewModelScope)

        monitorSignalUseCase()
            .onEach { rssi ->
                if (signalHistory.size >= maxHistorySize) {
                    signalHistory.removeFirst()
                }
                signalHistory.addLast(rssi)

                _uiState.update { currentState ->
                    val connection = if (currentState is HomeUiState.Success) {
                        currentState.connectionState
                    } else {
                        ConnectionState.Disconnected
                    }
                    HomeUiState.Success(
                        connectionState = connection,
                        signalHistory = signalHistory.toList()
                    )
                }
            }
            .catch { error ->
                Log.e(TAG, "Error en monitoreo de señal", error)
            }
            .launchIn(viewModelScope)
    }

    override fun onCleared() {
        super.onCleared()
        timeoutJob?.cancel()
        // FIX #15: Limpiar DemoModeManager para evitar memory leaks
        demoModeManager.cleanup()
        Log.d(TAG, "HomeViewModel destruido - DemoModeManager limpiado")
    }

    companion object {
        private const val TAG = "HomeViewModel"
        private const val TIMEOUT_DURATION = 5000L // 5 segundos - FIX UX: Reducido de 10s
        
        fun provideFactory(
            application: Application,
            repository: WifiRepository,
            getCurrentConnectionUseCase: GetCurrentConnectionUseCase,
            monitorSignalUseCase: MonitorSignalUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(
                    application,
                    repository,
                    getCurrentConnectionUseCase,
                    monitorSignalUseCase
                ) as T
            }
        }
    }
}
