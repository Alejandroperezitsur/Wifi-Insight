package com.example.wifiinsight.domain.util

import android.util.Log
import com.example.wifiinsight.data.model.SignalLevel
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * SignalCalculator v3.2 - PRODUCTION READY
 *
 * FIXES:
 * - ELIMINADO: signalHistory duplicado (SSOT ahora en Repository)
 * - Métodos puros: reciben List<Int> como parámetro en lugar de estado global
 * - Sin estado interno mutable
 */
object SignalCalculator {

    private const val TAG = "SignalCalculator"

    // Umbrales de RSSI basados en especificaciones WiFi y experiencia real
    private const val RSSI_EXCELLENT = -50
    private const val RSSI_GOOD = -60
    private const val RSSI_FAIR = -70
    private const val RSSI_WEAK = -80
    private const val RSSI_POOR = -90
    private const val RSSI_MIN = -100

    // Path loss constants para estimación de distancia
    private const val REFERENCE_RSSI_AT_1M_2_4GHZ = -40
    private const val REFERENCE_RSSI_AT_1M_5GHZ = -35
    private const val PATH_LOSS_EXPONENT = 2.0

    /**
     * Convierte RSSI (dBm) a porcentaje usando curva logarítmica real.
     */
    fun rssiToPercentage(rssi: Int): Int {
        return when {
            rssi >= RSSI_EXCELLENT -> 100
            rssi <= RSSI_MIN -> 0
            else -> {
                val normalized = (rssi - RSSI_MIN).toDouble() / (RSSI_EXCELLENT - RSSI_MIN)
                val curved = normalized.pow(0.7)
                (curved * 100).toInt().coerceIn(0, 100)
            }
        }
    }

    /**
     * Clasifica nivel de señal basado en RSSI.
     */
    fun rssiToSignalLevel(rssi: Int): SignalLevel {
        return when (rssi) {
            in RSSI_EXCELLENT..Int.MAX_VALUE -> SignalLevel.EXCELLENT
            in RSSI_GOOD..RSSI_EXCELLENT -> SignalLevel.GOOD
            in RSSI_FAIR..RSSI_GOOD -> SignalLevel.FAIR
            in RSSI_WEAK..RSSI_FAIR -> SignalLevel.WEAK
            in RSSI_POOR..RSSI_WEAK -> SignalLevel.POOR
            else -> SignalLevel.DEAD
        }
    }

    /**
     * Analiza estabilidad de señal basado en historial proporcionado.
     * RECIBE: List<Int> (del Repository, SSOT)
     */
    fun analyzeStability(signalHistory: List<Int>): StabilityResult {
        if (signalHistory.size < 3) {
            return StabilityResult(
                isStable = true,
                mean = signalHistory.lastOrNull()?.toDouble() ?: 0.0,
                variance = 0.0,
                trend = SignalTrend.STABLE
            )
        }

        val mean = signalHistory.average()
        val variance = signalHistory.map { (it - mean).pow(2) }.average()
        val stdDev = sqrt(variance)
        val cv = if (mean != 0.0) stdDev / mean else 0.0

        val trend = when {
            signalHistory.size < 2 -> SignalTrend.STABLE
            else -> {
                val recent = signalHistory.takeLast(5).average()
                val older = signalHistory.dropLast(5).takeLast(5).average()
                when {
                    recent > older + 2 -> SignalTrend.IMPROVING
                    recent < older - 2 -> SignalTrend.DEGRADING
                    else -> SignalTrend.STABLE
                }
            }
        }

        return StabilityResult(
            isStable = cv < 0.1 && variance < 25,
            mean = mean,
            variance = variance,
            trend = trend
        )
    }

    /**
     * Estima distancia al AP basado en RSSI y frecuencia.
     */
    fun estimateDistance(rssi: Int, frequency: Int = 2437): Double {
        val is5Ghz = frequency > 5000
        val referenceRssi = if (is5Ghz) REFERENCE_RSSI_AT_1M_5GHZ else REFERENCE_RSSI_AT_1M_2_4GHZ

        val pathLoss = referenceRssi - rssi
        return if (pathLoss > 0) {
            10.0.pow(pathLoss / (10 * PATH_LOSS_EXPONENT))
        } else 0.0
    }

    /**
     * Convierte frecuencia a número de canal.
     */
    fun frequencyToChannel(frequency: Int): Int {
        return when {
            frequency in 2412..2484 -> (frequency - 2407) / 5
            frequency in 5160..5885 -> (frequency - 5000) / 5
            frequency in 5925..6425 -> (frequency - 5920) / 5 + 1
            else -> -1
        }
    }

    fun is5GHz(frequency: Int) = frequency in 5160..5885
    fun is2_4GHz(frequency: Int) = frequency in 2412..2484
    fun is6GHz(frequency: Int) = frequency in 5925..6425

    fun getBandName(frequency: Int): String {
        return when {
            is2_4GHz(frequency) -> "2.4 GHz"
            is5GHz(frequency) -> "5 GHz"
            is6GHz(frequency) -> "6 GHz"
            else -> "Unknown"
        }
    }

    fun formatRssi(rssi: Int): String = "$rssi dBm"
    fun formatLinkSpeed(linkSpeed: Int): String = "$linkSpeed Mbps"

    /**
     * Genera recomendación basada en estabilidad.
     * RECIBE: List<Int> (del Repository, SSOT)
     */
    fun generateRecommendation(signalHistory: List<Int>): String {
        if (signalHistory.isEmpty()) return "Esperando datos de señal..."

        val currentRssi = signalHistory.last()
        val stability = analyzeStability(signalHistory)

        return when {
            !stability.isStable && stability.trend == SignalTrend.DEGRADING ->
                "Señal inestable y degradándose. Considera acercarte al router."
            !stability.isStable ->
                "Señal inestable. Evita obstrucciones entre el dispositivo y el router."
            currentRssi < RSSI_WEAK ->
                "Señal muy débil. Busca una ubicación más cercana al router."
            currentRssi < RSSI_FAIR ->
                "Señal aceptable pero podría mejorar. Prueba ajustar la posición."
            else ->
                "Señal estable y de buena calidad."
        }
    }

    /**
     * Estima throughput basado en señal y velocidad de enlace.
     */
    fun estimateThroughput(rssi: Int, linkSpeed: Int): Double {
        val signalPercentage = rssiToPercentage(rssi) / 100.0
        val efficiencyFactor = when {
            signalPercentage > 0.8 -> 0.75
            signalPercentage > 0.6 -> 0.6
            signalPercentage > 0.4 -> 0.45
            else -> 0.3
        }
        return linkSpeed * efficiencyFactor
    }
}

data class StabilityResult(
    val isStable: Boolean,
    val mean: Double,
    val variance: Double,
    val trend: SignalTrend
)

enum class SignalTrend {
    IMPROVING,
    STABLE,
    DEGRADING
}
