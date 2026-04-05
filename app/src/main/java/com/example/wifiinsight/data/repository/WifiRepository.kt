package com.example.wifiinsight.data.repository

import com.example.wifiinsight.data.model.WifiNetwork
import com.example.wifiinsight.data.model.WifiUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Interfaz Repository v3.2 - PRODUCTION READY
 *
 * ARQUITECTURA:
 * - SSOT: Un único StateFlow<WifiUiState>
 * - Sin flows separados (eliminado anti-patrón de múltiples fuentes)
 * - UI observa solo uiState
 */
interface WifiRepository {

    /**
     * SINGLE SOURCE OF TRUTH
     * Estado unificado que incluye toda la información UI.
     */
    val uiState: StateFlow<WifiUiState>

    /**
     * Inicia escaneo de redes.
     * Resultado via uiState.
     */
    suspend fun scanNetworks()

    /**
     * Reintenta operación.
     */
    suspend fun retry()

    /**
     * Actualiza estado de permisos.
     */
    fun updatePermissionState(granted: Boolean, shouldShowRationale: Boolean = false)

    /**
     * Abre settings de WiFi.
     */
    fun openWifiSettings(): Boolean

    /**
     * Activa/desactiva modo demo.
     */
    fun setDemoMode(enabled: Boolean)

    /**
     * Limpia recursos.
     */
    fun cleanup()

    /**
     * Obtiene historial de escaneo (para compatibilidad)
     */
    fun getScanHistory(): List<WifiNetwork>

    /**
     * Conecta a una red WiFi
     */
    fun connectToNetwork(network: WifiNetwork, password: String?): Flow<Result<Boolean>>

    // ===== MÉTODOS LEGACY (para compatibilidad) =====
    fun observeConnectionState(): StateFlow<WifiUiState>
    fun observeScanState(): StateFlow<WifiUiState>
    fun observeSystemState(): StateFlow<WifiUiState>
    fun observeErrorState(): StateFlow<WifiUiState>
}
