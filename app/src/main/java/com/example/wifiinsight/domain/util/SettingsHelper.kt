package com.example.wifiinsight.domain.util

import android.content.Context
import android.provider.Settings

/**
 * Helper para acceder a settings del sistema
 */
object SettingsHelper {
    
    fun isAirplaneModeOn(context: Context): Boolean {
        return Settings.Global.getInt(
            context.contentResolver,
            Settings.Global.AIRPLANE_MODE_ON, 0
        ) != 0
    }
    
    fun isLocationEnabled(context: Context): Boolean {
        return try {
            Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.LOCATION_MODE
            ) != Settings.Secure.LOCATION_MODE_OFF
        } catch (e: Settings.SettingNotFoundException) {
            false
        }
    }
}
