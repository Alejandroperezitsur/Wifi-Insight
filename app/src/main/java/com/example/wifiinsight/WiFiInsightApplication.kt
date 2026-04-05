package com.example.wifiinsight

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class WiFiInsightApplication : Application() {

    companion object {
        private const val TAG = "WiFiInsightApp"
    }

    override fun onCreate() {
        super.onCreate()
        setupGlobalExceptionHandler()
        Log.i(TAG, "WiFi Insight inicializada - Production Build")
    }

    /**
     * Configura handler global de excepciones
     * FIX CRÍTICO #4: Ya NO traga errores - siempre hace crash para evitar estado zombie
     */
    private fun setupGlobalExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "🚨 EXCEPCIÓN NO CAPTURADA en hilo ${thread.name}", throwable)

            // Log detallado del error para debugging
            when (throwable) {
                is NullPointerException -> Log.e(TAG, "NPE detectado - revisar null safety")
                is SecurityException -> Log.e(TAG, "SecurityException - revisar permisos")
                is IllegalStateException -> Log.e(TAG, "IllegalState - revisar estado Compose/ViewModel")
                is IndexOutOfBoundsException -> Log.e(TAG, "IndexOutOfBounds - revisar acceso a listas")
                is OutOfMemoryError -> Log.e(TAG, "OutOfMemoryError - revisar memory leaks")
                is StackOverflowError -> Log.e(TAG, "StackOverflowError - revisar recursion infinita")
            }

            // FIX: SIEMPRE delegar al handler default para hacer crash real
            // NUNCA continuar con la app en estado roto (zombie)
            defaultHandler?.uncaughtException(thread, throwable)
            // Si no hay handler default, forzar exit
            exitProcess(1)
        }
    }
}
