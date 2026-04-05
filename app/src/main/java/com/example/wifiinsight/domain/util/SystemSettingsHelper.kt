package com.example.wifiinsight.domain.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log

/**
 * Utilidades para abrir configuraciones del sistema
 */
object SystemSettingsHelper {
    
    private const val TAG = "SystemSettingsHelper"
    
    /**
     * Abre los ajustes de WiFi del sistema usando Settings Panel (Android 10+) o Settings fallback
     * @return true si se pudo abrir la configuración, false en caso contrario
     */
    fun openWifiSettings(context: Context): Boolean {
        return try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+: Usar el Settings Panel de WiFi (mejor UX)
                Intent(Settings.Panel.ACTION_WIFI).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                // Android 9 y menor: Ajustes WiFi tradicionales
                Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            
            // Verificar que haya una app que pueda manejar el intent
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                Log.d(TAG, "✓ Ajustes WiFi abiertos correctamente")
                true
            } else {
                // Fallback a ajustes generales de red
                Log.w(TAG, "Settings Panel no disponible, usando fallback")
                openNetworkSettings(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error abriendo ajustes WiFi", e)
            // Intentar fallback de última instancia
            openNetworkSettings(context)
        }
    }
    
    /**
     * Abre los ajustes de red del sistema (alternativa)
     * @return true si se pudo abrir la configuración, false en caso contrario
     */
    fun openNetworkSettings(context: Context): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Log.d(TAG, "✓ Ajustes de red abiertos (fallback)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error abriendo ajustes de red", e)
            // Último fallback: ajustes generales
            openGeneralSettings(context)
        }
    }
    
    /**
     * Abre los ajustes generales de la aplicación
     * @return true si se pudo abrir la configuración, false en caso contrario
     */
    fun openAppSettings(context: Context): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Log.d(TAG, "✓ Ajustes de app abiertos")
            true
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error abriendo ajustes de app", e)
            false
        }
    }
    
    /**
     * Abre ajustes generales del sistema (último fallback)
     */
    private fun openGeneralSettings(context: Context): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Log.d(TAG, "✓ Ajustes generales abiertos (fallback final)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "✗ No se pudieron abrir ningún tipo de ajustes", e)
            false
        }
    }
}
