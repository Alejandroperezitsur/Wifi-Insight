package com.example.wifiinsight.domain.usecase

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.wifiinsight.data.model.BlockingState
import com.example.wifiinsight.data.model.PermissionState
import com.example.wifiinsight.data.model.WifiNetwork
import com.example.wifiinsight.domain.util.SettingsHelper
import javax.inject.Inject

/**
 * Caso de uso para escanear redes WiFi disponibles.
 * Valida precondiciones y ejecuta el escaneo.
 */
class ScanNetworksUseCase @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "ScanNetworksUseCase"
        private const val SCAN_THROTTLE_MS = 30_000L
    }

    data class ScanResult(
        val networks: List<WifiNetwork>,
        val isThrottled: Boolean = false,
        val remainingThrottleMs: Long = 0L,
        val blockingState: BlockingState? = null,
        val errorMessage: String? = null
    )

    private var lastScanTime = 0L

    /**
     * Verifica si se puede escanear y retorna el estado de bloqueo si aplica.
     */
    fun checkCanScan(): Pair<Boolean, BlockingState?> {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        
        // Verificar WiFi activado
        if (wifiManager?.isWifiEnabled != true) {
            return Pair(false, BlockingState.NoWifi)
        }
        
        // Verificar modo avión
        if (SettingsHelper.isAirplaneModeOn(context)) {
            return Pair(false, BlockingState.AirplaneMode)
        }
        
        // Verificar ubicación
        if (!SettingsHelper.isLocationEnabled(context)) {
            return Pair(false, BlockingState.LocationOff)
        }
        
        // Verificar permisos
        val permissionState = checkPermissionState()
        if (permissionState != PermissionState.Granted) {
            return Pair(false, BlockingState.NoPermission)
        }
        
        return Pair(true, null)
    }

    /**
     * Verifica si el escaneo está siendo throttled por Android.
     */
    fun checkThrottleStatus(): Pair<Boolean, Long> {
        val elapsed = SystemClock.elapsedRealtime() - lastScanTime
        return if (elapsed < SCAN_THROTTLE_MS) {
            Pair(true, SCAN_THROTTLE_MS - elapsed)
        } else {
            Pair(false, 0L)
        }
    }

    /**
     * Ejecuta el escaneo de redes si todas las precondiciones se cumplen.
     */
    suspend fun execute(): ScanResult {
        // Validar precondiciones
        val (canScan, blockingState) = checkCanScan()
        if (!canScan) {
            return ScanResult(
                networks = emptyList(),
                blockingState = blockingState,
                errorMessage = getBlockingMessage(blockingState)
            )
        }

        // Verificar throttling
        val (isThrottled, remainingMs) = checkThrottleStatus()
        if (isThrottled) {
            return ScanResult(
                networks = emptyList(),
                isThrottled = true,
                remainingThrottleMs = remainingMs,
                errorMessage = "Escaneo limitado por Android. Espera ${remainingMs / 1000}s."
            )
        }

        // TODO: Implementar escaneo real aquí o delegar al repository
        lastScanTime = SystemClock.elapsedRealtime()
        
        return ScanResult(
            networks = emptyList(), // El repository aún maneja el escaneo real
            isThrottled = false,
            remainingThrottleMs = 0L
        )
    }

    private fun checkPermissionState(): PermissionState {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.NEARBY_WIFI_DEVICES)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val allGranted = permissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }

        return if (allGranted) PermissionState.Granted else PermissionState.Denied
    }

    private fun getBlockingMessage(state: BlockingState?): String {
        return when (state) {
            is BlockingState.NoWifi -> "WiFi desactivado. Activa WiFi para escanear."
            is BlockingState.AirplaneMode -> "Modo avión activado. Desactívalo para usar WiFi."
            is BlockingState.LocationOff -> "Ubicación desactivada. Actívala para escanear redes."
            is BlockingState.NoPermission -> "Permisos requeridos. Concede permisos de ubicación."
            null -> "Error desconocido"
        }
    }
}
