package com.example.wifiinsight.data.manager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.provider.Settings
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Detecta el estado del modo avión globalmente
 * FIX ESTABILIDAD #12: Detectar modo avión globalmente
 */
class AirplaneModeManager(private val context: Context) {

    /**
     * Flow que emite el estado actual del modo avión
     */
    val isAirplaneModeOn: Flow<Boolean> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_AIRPLANE_MODE_CHANGED) {
                    trySend(isAirplaneModeOn())
                }
            }
        }

        // Enviar estado inicial
        trySend(isAirplaneModeOn())

        // Registrar receiver
        context.registerReceiver(
            receiver,
            IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED)
        )

        awaitClose {
            context.unregisterReceiver(receiver)
        }
    }

    private fun isAirplaneModeOn(): Boolean {
        return Settings.Global.getInt(
            context.contentResolver,
            Settings.Global.AIRPLANE_MODE_ON,
            0
        ) != 0
    }
}
