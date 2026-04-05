package com.example.wifiinsight.data.repository

import com.example.wifiinsight.data.model.ConnectionUiState
import com.example.wifiinsight.data.model.ErrorUiState
import com.example.wifiinsight.data.model.ScanUiState
import com.example.wifiinsight.data.model.SystemUiState
import kotlinx.coroutines.flow.Flow

/**
 * Interfaz Repository v3.1 - Estados Granulares
 * 
 * ARQUITECTURA OPTIMIZADA:
 * - 4 flows separados en lugar de 1 monolítico
 * - UI observa solo lo que necesita
 * - Sin recomposiciones innecesarias
 */
interface WifiRepository {
    
    /**
     * Estado de conexión - cambia con WiFi/internet/señal.
     * Con debounce(300ms) + conflate() para reducir emisiones.
     */
    fun observeConnectionState(): Flow<ConnectionUiState>
    
    /**
     * Estado de escaneo - cambia solo durante scans.
     * Throttle countdown incluido como flow reactivo.
     */
    fun observeScanState(): Flow<ScanUiState>
    
    /**
     * Estado del sistema - cambia raramente.
     * WiFi on/off, permisos, modo avión.
     */
    fun observeSystemState(): Flow<SystemUiState>
    
    /**
     * Estado de error - aislado para manejo específico.
     */
    fun observeErrorState(): Flow<ErrorUiState>
    
    /**
     * Inicia escaneo de redes.
     * Resultado via observeScanState().
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
}
