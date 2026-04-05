package com.example.wifiinsight.domain.util

import android.util.Log
import com.example.wifiinsight.data.model.ConnectionState
import com.example.wifiinsight.data.model.WifiNetwork
import com.example.wifiinsight.data.model.SecurityType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Modo Demo - Simula escenarios de red para presentaciones.
 * Permite mostrar la app sin depender del entorno WiFi real.
 * 
 * SEGURIDAD: Cuando demoMode = true, TODOS los datos reales son bloqueados
 * y reemplazados por datos simulados consistentes.
 */
class DemoModeManager {
    private val _isDemoMode = MutableStateFlow(false)
    val isDemoMode: StateFlow<Boolean> = _isDemoMode.asStateFlow()

    // Datos demo consistentes - nunca null
    private val _demoConnectionState = MutableStateFlow<ConnectionState>(
        ConnectionState.Connected(
            ssid = "MiRed Demo",
            ipAddress = "192.168.1.100",
            linkSpeed = 144,
            rssi = -45,
            hasInternet = true,
            isValidated = true
        )
    )
    val demoConnectionState: StateFlow<ConnectionState> = _demoConnectionState.asStateFlow()

    private val _demoNetworks = MutableStateFlow<List<WifiNetwork>>(generateDemoNetworks())
    val demoNetworks: StateFlow<List<WifiNetwork>> = _demoNetworks.asStateFlow()

    private val _demoSignalHistory = MutableStateFlow<List<Int>>(List(20) { -50 + (it % 10) })
    val demoSignalHistory: StateFlow<List<Int>> = _demoSignalHistory.asStateFlow()

    private var demoScenarioJob: Job? = null

    /**
     * Activa/desactiva el modo demo con lifecycle controlado.
     * REQUIERE CoroutineScope externo - nunca crear scope propio.
     * 
     * @param enabled true para activar demo, false para desactivar
     * @param scope CoroutineScope externo (ej: viewModelScope) - obligatorio
     */
    fun setDemoMode(enabled: Boolean, scope: CoroutineScope) {
        Log.d(TAG, "setDemoMode: $enabled")
        
        if (enabled) {
            resetDemoState()
            _isDemoMode.value = true
            Log.i(TAG, "✓ Modo demo ACTIVADO - datos reales bloqueados")
            startDemoScenario(scope)
        } else {
            _isDemoMode.value = false
            stopDemoScenario()
            resetDemoState()
            Log.i(TAG, "✓ Modo demo DESACTIVADO - limpiando estado")
        }
    }

    /**
     * Resetea el estado demo a valores iniciales consistentes.
     * Garantiza que no haya datos null o inconsistentes.
     */
    fun resetDemoState() {
        Log.d(TAG, "Reseteando estado demo...")
        
        _demoConnectionState.value = ConnectionState.Connected(
            ssid = "MiRed Demo",
            ipAddress = "192.168.1.100",
            linkSpeed = 144,
            rssi = -45,
            hasInternet = true,
            isValidated = true
        )
        
        _demoNetworks.value = generateDemoNetworks()
        _demoSignalHistory.value = List(20) { -50 + (it % 10) }
        
        Log.d(TAG, "✓ Estado demo reseteado")
    }

    /**
     * Obtiene datos de conexión seguros (nunca null).
     * Si demoMode=true → retorna datos simulados.
     * Si demoMode=false → retorna null (usar datos reales).
     */
    fun getSafeConnectionState(realState: ConnectionState?): ConnectionState {
        return if (_isDemoMode.value) {
            _demoConnectionState.value
        } else {
            realState ?: ConnectionState.Disconnected
        }
    }

    /**
     * Obtiene lista de redes segura (nunca null, nunca vacía en demo).
     * Si demoMode=true → retorna redes simuladas.
     * Si demoMode=false → retorna datos reales o emptyList.
     */
    fun getSafeNetworks(realNetworks: List<WifiNetwork>?): List<WifiNetwork> {
        return if (_isDemoMode.value) {
            _demoNetworks.value.takeIf { it.isNotEmpty() } ?: generateDemoNetworks()
        } else {
            realNetworks ?: emptyList()
        }
    }

    /**
     * Obtiene historial de señal seguro (nunca null).
     * Si demoMode=true → retorna datos simulados.
     * Si demoMode=false → retorna datos reales o emptyList.
     */
    fun getSafeSignalHistory(realHistory: List<Int>?): List<Int> {
        return if (_isDemoMode.value) {
            _demoSignalHistory.value.takeIf { it.isNotEmpty() } ?: List(20) { -50 }
        } else {
            realHistory ?: emptyList()
        }
    }

    /**
     * Inicia el escenario demo con cambios automáticos
     * @param scope CoroutineScope controlado externamente para lifecycle management
     */
    private fun startDemoScenario(scope: CoroutineScope) {
        demoScenarioJob?.cancel() // Cancelar job anterior si existe
        demoScenarioJob = scope.launch {
            var scenarioIndex = 0
            val scenarios = listOf(
                DemoScenario.EXCELLENT_CONNECTION,
                DemoScenario.GOOD_CONNECTION,
                DemoScenario.UNSTABLE_SIGNAL,
                DemoScenario.NO_INTERNET,
                DemoScenario.WEAK_SIGNAL
            )

            while (isActive) {
                val scenario = scenarios[scenarioIndex % scenarios.size]
                applyScenario(scenario)
                scenarioIndex++
                delay(8000)
            }
        }
    }

    /**
     * Detiene el escenario demo
     */
    private fun stopDemoScenario() {
        demoScenarioJob?.cancel()
        demoScenarioJob = null
    }
    
    /**
     * FIX #15: Limpia todos los recursos para evitar memory leaks
     * Llamar cuando el ViewModel se destruye
     */
    fun cleanup() {
        Log.d(TAG, "Limpiando DemoModeManager...")
        stopDemoScenario()
        _isDemoMode.value = false
        // Cancelar cualquier CoroutineScope pendiente
        demoScenarioJob?.cancel()
        demoScenarioJob = null
        Log.d(TAG, "✓ DemoModeManager limpiado")
    }

    /**
     * Aplica un escenario específico de forma segura (sin nulls)
     */
    fun applyScenario(scenario: DemoScenario) {
        Log.d(TAG, "Aplicando escenario: $scenario")
        
        when (scenario) {
            DemoScenario.EXCELLENT_CONNECTION -> {
                _demoConnectionState.value = ConnectionState.Connected(
                    ssid = "Red UltraRápida",
                    ipAddress = "192.168.1.100",
                    linkSpeed = 300,
                    rssi = -35,
                    hasInternet = true,
                    isValidated = true
                )
                updateSignalHistory(-35)
            }
            DemoScenario.GOOD_CONNECTION -> {
                _demoConnectionState.value = ConnectionState.Connected(
                    ssid = "MiRed Casa",
                    ipAddress = "192.168.1.105",
                    linkSpeed = 144,
                    rssi = -55,
                    hasInternet = true,
                    isValidated = true
                )
                updateSignalHistory(-55)
            }
            DemoScenario.UNSTABLE_SIGNAL -> {
                _demoConnectionState.value = ConnectionState.Connected(
                    ssid = "Red Inestable",
                    ipAddress = "192.168.1.110",
                    linkSpeed = 72,
                    rssi = -65,
                    hasInternet = true,
                    isValidated = true
                )
                // FIX: Usar scope externo, no crear scope propio
                demoScenarioJob?.takeIf { it.isActive }?.let { activeScope ->
                    kotlinx.coroutines.GlobalScope.launch {
                        repeat(5) {
                            kotlinx.coroutines.delay(1000)
                            val fluctuation = (-70..-60).random()
                            val current = _demoConnectionState.value
                            if (current is ConnectionState.Connected) {
                                _demoConnectionState.value = current.copy(rssi = fluctuation)
                            }
                            updateSignalHistory(fluctuation)
                        }
                    }
                }
            }
            DemoScenario.NO_INTERNET -> {
                _demoConnectionState.value = ConnectionState.Connected(
                    ssid = "Red Sin Internet",
                    ipAddress = "192.168.1.120",
                    linkSpeed = 144,
                    rssi = -50,
                    hasInternet = false,
                    isValidated = false
                )
                updateSignalHistory(-50)
            }
            DemoScenario.WEAK_SIGNAL -> {
                _demoConnectionState.value = ConnectionState.Connected(
                    ssid = "Red Lejana",
                    ipAddress = "192.168.1.130",
                    linkSpeed = 11,
                    rssi = -85,
                    hasInternet = true,
                    isValidated = true
                )
                updateSignalHistory(-85)
            }
            DemoScenario.DISCONNECTED -> {
                _demoConnectionState.value = ConnectionState.Disconnected
            }
        }
        
        Log.d(TAG, "✓ Escenario aplicado: $scenario")
    }

    /**
     * Simula un escaneo de redes de forma segura
     */
    fun simulateScan(): Flow<List<WifiNetwork>> = flow {
        emit(emptyList())
        delay(1500)
        emit(generateDemoNetworks())
    }

    /**
     * Actualiza el historial de señal de forma segura (sin nulls)
     */
    private fun updateSignalHistory(newRssi: Int) {
        _demoSignalHistory.update { current ->
            (current + newRssi).takeLast(20)
        }
    }

    companion object {
        private const val TAG = "DemoModeManager"
        
        fun generateDemoNetworks(): List<WifiNetwork> {
            return listOf(
                WifiNetwork(
                    ssid = "MiRed Casa",
                    bssid = "00:11:22:33:44:55",
                    rssi = -45,
                    frequency = 5200,
                    securityType = SecurityType.WPA3
                ),
                WifiNetwork(
                    ssid = "Vecino_5G",
                    bssid = "00:11:22:33:44:66",
                    rssi = -62,
                    frequency = 5200,
                    securityType = SecurityType.WPA2
                ),
                WifiNetwork(
                    ssid = "Starbucks_WiFi",
                    bssid = "00:11:22:33:44:77",
                    rssi = -58,
                    frequency = 2400,
                    securityType = SecurityType.OPEN
                ),
                WifiNetwork(
                    ssid = "Oficina_Ejecutiva",
                    bssid = "00:11:22:33:44:88",
                    rssi = -55,
                    frequency = 5200,
                    securityType = SecurityType.WPA2
                ),
                WifiNetwork(
                    ssid = "Red_Inestable",
                    bssid = "00:11:22:33:44:99",
                    rssi = -78,
                    frequency = 2400,
                    securityType = SecurityType.WPA
                ),
                WifiNetwork(
                    ssid = "Café_Gratis",
                    bssid = "00:11:22:33:44:AA",
                    rssi = -72,
                    frequency = 2400,
                    securityType = SecurityType.OPEN
                ),
                WifiNetwork(
                    ssid = "Red5G_Invitados",
                    bssid = "00:11:22:33:44:BB",
                    rssi = -65,
                    frequency = 5200,
                    securityType = SecurityType.WPA2
                )
            )
        }
    }
}

/**
 * Escenarios disponibles en modo demo
 */
enum class DemoScenario {
    EXCELLENT_CONNECTION,
    GOOD_CONNECTION,
    UNSTABLE_SIGNAL,
    NO_INTERNET,
    WEAK_SIGNAL,
    DISCONNECTED
}
