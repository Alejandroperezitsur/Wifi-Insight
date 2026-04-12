package com.example.wifiinsight.domain.usecase

import com.example.wifiinsight.data.model.WifiNetwork
import com.example.wifiinsight.domain.util.ConnectionResult
import com.example.wifiinsight.domain.util.WifiConnector
import javax.inject.Inject

/**
 * Caso de uso para conectar a una red WiFi específica.
 * Abstrae la lógica de conexión usando WifiNetworkSpecifier.
 */
class ConnectToNetworkUseCase @Inject constructor(
    private val wifiConnector: WifiConnector
) {
    companion object {
        private const val TAG = "ConnectToNetworkUseCase"
    }

    data class ConnectionAttempt(
        val network: WifiNetwork,
        val password: String?,
        val timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Intenta conectar a una red WiFi.
     *
     * @param network La red objetivo
     * @param password Contraseña para redes protegidas
     * @return Resultado de la conexión con mensaje descriptivo
     */
    suspend fun execute(network: WifiNetwork, password: String?): Result<String> {
        return when (val result = wifiConnector.connectToNetwork(network, password)) {
            is ConnectionResult.Success -> {
                Result.success(result.message)
            }
            is ConnectionResult.Error -> {
                Result.failure(Exception(result.message))
            }
        }
    }

    /**
     * Valida si la contraseña es requerida para la red.
     */
    fun isPasswordRequired(network: WifiNetwork): Boolean {
        return network.securityType.isSecure()
    }

    /**
     * Valida la contraseña antes de intentar conexión.
     * Retorna null si es válida, o mensaje de error si no.
     */
    fun validatePassword(network: WifiNetwork, password: String?): String? {
        if (!isPasswordRequired(network)) {
            return null // No se requiere contraseña
        }

        if (password.isNullOrBlank()) {
            return "Se requiere contraseña para redes ${network.securityType.displayName()}"
        }

        // Validaciones adicionales según el tipo de seguridad
        return when (network.securityType) {
            // WPA/WPA2: mínimo 8 caracteres
            else -> if (password.length < 8) {
                "La contraseña debe tener al menos 8 caracteres"
            } else {
                null
            }
        }
    }
}
