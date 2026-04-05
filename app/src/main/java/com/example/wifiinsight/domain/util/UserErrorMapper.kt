package com.example.wifiinsight.domain.util

/**
 * Mapea errores técnicos a mensajes humanos comprensibles ampliado.
 * NUNCA mostrar excepciones crudas al usuario.
 */
object UserErrorMapper {

    /**
     * Mapea cualquier Throwable a un mensaje user-friendly completo.
     */
    fun map(error: Throwable?): String {
        return when (error) {
            null -> "Ocurrió un error inesperado"
            is SecurityException -> mapSecurityError(error)
            is IllegalStateException -> mapStateError(error)
            is UnsupportedOperationException -> "Esta función no está soportada en tu dispositivo"
            is java.net.SocketTimeoutException,
            is java.util.concurrent.TimeoutException -> "La operación tardó demasiado. Intenta de nuevo."
            is java.net.UnknownHostException,
            is java.net.ConnectException -> "No se pudo conectar. Verifica tu conexión."
            else -> when {
                error.message?.contains("throttle", ignoreCase = true) == true ->
                    "Android limita los escaneos frecuentes. Espera unos segundos."
                error.message?.contains("permission", ignoreCase = true) == true ->
                    "Necesitamos permisos para buscar redes WiFi"
                error.message?.contains("airplane", ignoreCase = true) == true ->
                    "Desactiva el modo avión para usar WiFi"
                error.message?.contains("disabled", ignoreCase = true) == true ->
                    "Activa el WiFi para continuar"
                error.message?.contains("location", ignoreCase = true) == true ->
                    "Activa la ubicación para buscar redes WiFi"
                else -> error.message ?: "Algo salió mal. Intenta de nuevo."
            }
        }
    }

    private fun mapSecurityError(error: SecurityException): String {
        val msg = error.message ?: ""
        return when {
            msg.contains("NEARBY_WIFI_DEVICES") ->
                "Permite el permiso 'Dispositivos WiFi cercanos' para escanear"
            msg.contains("ACCESS_FINE_LOCATION") || msg.contains("location") ->
                "Necesitamos acceso a ubicación para encontrar redes WiFi"
            msg.contains("network_settings") ->
                "No tienes permisos para cambiar configuraciones de red"
            else -> "Permisos insuficientes. Revisa la configuración de la app."
        }
    }

    private fun mapStateError(error: IllegalStateException): String {
        val msg = error.message ?: ""
        return when {
            msg.contains("wifi", ignoreCase = true) && msg.contains("disabled", ignoreCase = true) ->
                "El WiFi está apagado. Actívalo para continuar."
            msg.contains("airplane", ignoreCase = true) ->
                "Modo avión activado. Desactívalo primero."
            msg.contains("scan", ignoreCase = true) ->
                "No se pudo buscar redes. Intenta de nuevo."
            else -> "Estado inválido. Reinicia la app si el problema persiste."
        }
    }

    /**
     * Mensajes específicos para estados de UI
     */
    object Messages {
        const val NO_PERMISSIONS = "Necesitamos permisos para buscar redes WiFi cercanas"
        const val LOCATION_DISABLED = "Activa la ubicación para encontrar redes"
        const val WIFI_DISABLED = "WiFi apagado. Actívalo para analizar redes"
        const val AIRPLANE_MODE = "Modo avión activo. Desactívalo para usar WiFi"
        const val NO_NETWORKS_FOUND = "No encontramos redes WiFi. Intenta en otro lugar."
        const val SCAN_THROTTLED = "Android limita las búsquedas. Espera unos segundos..."
        const val CONNECTING = "Conectando..."
        const val CONNECTION_FAILED = "No se pudo conectar. Verifica la contraseña."
        const val SIGNAL_WEAK = "Señal débil. Acércate al router para mejor conexión."
        const val SIGNAL_INVALID = "Sin datos de señal disponibles"
    }
}
