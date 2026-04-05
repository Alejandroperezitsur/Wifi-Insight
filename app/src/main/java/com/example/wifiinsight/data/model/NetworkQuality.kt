package com.example.wifiinsight.data.model

data class NetworkQuality(
    val level: SignalLevel,
    val recommendation: String,
    val canStreamHD: Boolean,
    val canStream4K: Boolean,
    val suitableForGaming: Boolean
) {
    companion object {
        fun fromRssi(rssi: Int): NetworkQuality {
            val level = SignalLevel.fromRssi(rssi)
            return when (level) {
                SignalLevel.EXCELLENT -> NetworkQuality(
                    level = level,
                    recommendation = "Conexión óptima. Ideal para streaming 4K, gaming y videollamadas.",
                    canStreamHD = true,
                    canStream4K = true,
                    suitableForGaming = true
                )
                SignalLevel.GOOD -> NetworkQuality(
                    level = level,
                    recommendation = "Buena conexión. Perfecta para streaming HD y trabajo remoto.",
                    canStreamHD = true,
                    canStream4K = true,
                    suitableForGaming = true
                )
                SignalLevel.FAIR -> NetworkQuality(
                    level = level,
                    recommendation = "Señal aceptable. Funciona para navegación y streaming HD.",
                    canStreamHD = true,
                    canStream4K = false,
                    suitableForGaming = false
                )
                SignalLevel.WEAK -> NetworkQuality(
                    level = level,
                    recommendation = "Señal débil. Acércate al router para mejorar la conexión.",
                    canStreamHD = false,
                    canStream4K = false,
                    suitableForGaming = false
                )
                SignalLevel.POOR -> NetworkQuality(
                    level = level,
                    recommendation = "Señal muy débil. Múvete más cerca del router o verifica obstáculos.",
                    canStreamHD = false,
                    canStream4K = false,
                    suitableForGaming = false
                )
                SignalLevel.DEAD -> NetworkQuality(
                    level = level,
                    recommendation = "Sin señal. Verifica que el WiFi esté activado o acércate al router.",
                    canStreamHD = false,
                    canStream4K = false,
                    suitableForGaming = false
                )
            }
        }
    }
}
