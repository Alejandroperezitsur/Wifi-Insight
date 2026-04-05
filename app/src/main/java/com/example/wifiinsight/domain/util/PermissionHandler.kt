package com.example.wifiinsight.domain.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Handler centralizado para gestión de permisos WiFi
 * FIX CRÍTICO: Evita estado zombie cuando permisos son denegados
 */
class PermissionHandler(private val context: Context) {

    companion object {
        val REQUIRED_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.NEARBY_WIFI_DEVICES,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
    }

    /**
     * Verifica si todos los permisos requeridos están concedidos
     */
    fun hasAllPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Obtiene el estado actual de los permisos
     */
    fun getPermissionState(activity: Activity? = null): PermissionState {
        return when {
            hasAllPermissions() -> PermissionState.Granted
            activity == null -> PermissionState.Unknown
            shouldShowRationale(activity) -> PermissionState.Denied
            isPermanentlyDenied(activity) -> PermissionState.PermanentlyDenied
            else -> PermissionState.Denied
        }
    }

    /**
     * Determina si se debe mostrar explicación de por qué se necesita el permiso
     */
    fun shouldShowRationale(activity: Activity): Boolean {
        return REQUIRED_PERMISSIONS.any { permission ->
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        }
    }

    /**
     * Determina si los permisos fueron denegados permanentemente ("Don't ask again")
     */
    fun isPermanentlyDenied(activity: Activity): Boolean {
        val anyDenied = REQUIRED_PERMISSIONS.any { permission ->
            ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED
        }
        val anyRationale = REQUIRED_PERMISSIONS.any { permission ->
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        }
        // Si hay permisos denegados pero no se debe mostrar rationale = denegado permanentemente
        return anyDenied && !anyRationale
    }

    /**
     * Lista de permisos que aún no han sido concedidos
     */
    fun getMissingPermissions(): List<String> {
        return REQUIRED_PERMISSIONS.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }
    }
}
