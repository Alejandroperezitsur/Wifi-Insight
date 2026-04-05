package com.example.wifiinsight.data.model

enum class SignalLevel(
    val label: String,
    val description: String,
    val minRssi: Int,
    val maxRssi: Int
) {
    EXCELLENT(
        label = "Excelente",
        description = "Señal muy fuerte",
        minRssi = -50,
        maxRssi = 0
    ),
    GOOD(
        label = "Buena",
        description = "Señal fuerte",
        minRssi = -60,
        maxRssi = -51
    ),
    FAIR(
        label = "Regular",
        description = "Señal aceptable",
        minRssi = -70,
        maxRssi = -61
    ),
    WEAK(
        label = "Débil",
        description = "Señal baja",
        minRssi = -80,
        maxRssi = -71
    ),
    POOR(
        label = "Muy débil",
        description = "Señal muy baja",
        minRssi = -100,
        maxRssi = -81
    ),
    DEAD(
        label = "Sin señal",
        description = "No hay señal",
        minRssi = Int.MIN_VALUE,
        maxRssi = -101
    );

    companion object {
        fun fromRssi(rssi: Int): SignalLevel {
            return when {
                rssi >= -50 -> EXCELLENT
                rssi >= -60 -> GOOD
                rssi >= -70 -> FAIR
                rssi >= -80 -> WEAK
                else -> POOR
            }
        }

        fun calculatePercentage(rssi: Int): Int {
            return when {
                rssi >= -50 -> 100
                rssi <= -100 -> 0
                else -> 2 * (rssi + 100)
            }.coerceIn(0, 100)
        }
    }
}
