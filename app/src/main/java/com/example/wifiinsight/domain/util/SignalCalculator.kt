package com.example.wifiinsight.domain.util

import android.util.Log
import com.example.wifiinsight.data.model.SignalLevel
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Calculador de señal con hardening profesional.
 * Incluye algoritmos de:
 * - Conversión RSSI a calidad real considerando variación
 * - Detección de estabilidad mediante análisis de varianza
 * - Estimación de distancia basada en path loss
 * - Clasificación dinámica con tendencias
 */
object SignalCalculator {
    
    private const val TAG = "SignalCalculator"
    
    // Historial para análisis de estabilidad (circular buffer)
    private const val HISTORY_SIZE = 20
    private val signalHistory = ArrayDeque<Int>(HISTORY_SIZE)
    
    // Umbrales de RSSI basados en especificaciones WiFi y experiencia real
    private const val RSSI_EXCELLENT = -50  // Muy cerca del AP
    private const val RSSI_GOOD = -60       // Buena señal
    private const val RSSI_FAIR = -70      // Aceptable
    private const val RSSI_WEAK = -80      // Débil pero usable
    private const val RSSI_POOR = -90      // Muy débil
    private const val RSSI_MIN = -100      // Límite práctico
    
    // Path loss constants para estimación de distancia
    private const val REFERENCE_RSSI_AT_1M_2_4GHZ = -40
    private const val REFERENCE_RSSI_AT_1M_5GHZ = -35
    private const val PATH_LOSS_EXPONENT = 2.0 // Free space path loss

    /**
     * Convierte RSSI (dBm) a porcentaje usando curva logarítmica real.
     * A diferencia de conversión lineal simple, esto refleja mejor
     * la percepción humana de la señal WiFi.
     */
    fun rssiToPercentage(rssi: Int): Int {
        return when {
            rssi >= RSSI_EXCELLENT -> 100
            rssi <= RSSI_MIN -> 0
            else -> {
                // Curva logarítmica para mejor representación
                val normalized = (rssi - RSSI_MIN).toDouble() / (RSSI_EXCELLENT - RSSI_MIN)
                val curved = normalized.pow(0.7) // Aplicar curva
                (curved * 100).toInt().coerceIn(0, 100)
            }
        }
    }

    /**
     * Determina nivel de señal basado en RSSI real.
     */
    fun rssiToSignalLevel(rssi: Int): SignalLevel {
        return when {
            rssi >= RSSI_EXCELLENT -> SignalLevel.EXCELLENT
            rssi >= RSSI_GOOD -> SignalLevel.GOOD
            rssi >= RSSI_FAIR -> SignalLevel.FAIR
            rssi >= RSSI_WEAK -> SignalLevel.WEAK
            rssi >= RSSI_POOR -> SignalLevel.POOR
            else -> SignalLevel.POOR
        }
    }

    /**
     * Análisis de estabilidad de señal basado en varianza del historial.
     * 
     * @return SignalStability con coeficiente de variación y clasificación
     */
    fun analyzeStability(currentRssi: Int): SignalStability {
        // Agregar al historial
        if (signalHistory.size >= HISTORY_SIZE) {
            signalHistory.removeFirst()
        }
        signalHistory.addLast(currentRssi)
        
        // Calcular estadísticas
        val mean = signalHistory.average()
        val variance = if (signalHistory.size > 1) {
            signalHistory.map { (it - mean).pow(2) }.average()
        } else 0.0
        
        val stdDev = sqrt(variance)
        val cv = if (mean != 0.0) stdDev / kotlin.math.abs(mean) else 0.0
        
        // Calcular tendencia
        val trend = calculateTrend()
        
        Log.v(TAG, "RSSI=$currentRssi, mean=${mean.toInt()}, stdDev=${stdDev.toInt()}, CV=${"%.3f".format(cv)}, trend=$trend")
        
        return SignalStability(
            currentRssi = currentRssi,
            meanRssi = mean.toInt(),
            standardDeviation = stdDev.toInt(),
            coefficientOfVariation = cv,
            trend = trend,
            sampleSize = signalHistory.size,
            classification = classifyStability(cv, trend)
        )
    }

    /**
     * Calcula tendencia de la señal basada en los últimos valores.
     */
    private fun calculateTrend(): SignalTrend {
        if (signalHistory.size < 3) return SignalTrend.STABLE
        
        val recent = signalHistory.takeLast(5)
        val firstHalf = recent.take(recent.size / 2).average()
        val secondHalf = recent.drop(recent.size / 2).average()
        
        val diff = secondHalf - firstHalf
        
        return when {
            diff > 3 -> SignalTrend.IMPROVING
            diff < -3 -> SignalTrend.DEGRADING
            else -> SignalTrend.STABLE
        }
    }

    /**
     * Clasifica estabilidad basada en coeficiente de variación.
     * CV < 0.05: Excelente estabilidad
     * CV < 0.15: Buena estabilidad  
     * CV < 0.30: Estabilidad aceptable
     * CV >= 0.30: Señal inestable
     */
    private fun classifyStability(cv: Double, trend: SignalTrend): StabilityClassification {
        return when {
            cv < 0.05 && trend == SignalTrend.STABLE -> StabilityClassification.EXCELLENT
            cv < 0.15 && trend != SignalTrend.DEGRADING -> StabilityClassification.GOOD
            cv < 0.30 -> StabilityClassification.FAIR
            else -> StabilityClassification.UNSTABLE
        }
    }

    /**
     * Estima distancia al punto de acceso basado en path loss.
     * Usa modelo de pérdida de propagación en espacio libre.
     * 
     * @param frequencyMHz Frecuencia del canal (2400-5800 MHz)
     * @param rssi RSSI medido
     * @return Distancia estimada en metros
     */
    fun estimateDistance(frequencyMHz: Int, rssi: Int): Double {
        val referenceRssi = if (frequencyMHz >= 5000) {
            REFERENCE_RSSI_AT_1M_5GHZ
        } else {
            REFERENCE_RSSI_AT_1M_2_4GHZ
        }
        
        // Fórmula de path loss: PL(d) = PL(d0) + 10*n*log10(d/d0)
        // Despejando d: d = d0 * 10^((PL(d) - PL(d0)) / (10*n))
        val pathLoss = referenceRssi - rssi
        val distance = 1.0 * 10.0.pow(pathLoss / (10.0 * PATH_LOSS_EXPONENT))
        
        // Limitar a rangos razonables
        return distance.coerceIn(0.5, 100.0)
    }

    /**
     * Convierte frecuencia a número de canal.
     */
    fun frequencyToChannel(frequency: Int): Int {
        return when {
            // 2.4 GHz band
            frequency in 2412..2484 -> (frequency - 2407) / 5
            // 5 GHz band
            frequency in 5170..5825 -> (frequency - 5000) / 5
            // 6 GHz band (WiFi 6E)
            frequency in 5955..7115 -> (frequency - 5950) / 5
            else -> -1
        }
    }

    /**
     * Determina si es banda de 5 GHz.
     */
    fun is5GHz(frequency: Int): Boolean = frequency in 5000..6000

    /**
     * Determina si es banda de 2.4 GHz.
     */
    fun is2_4GHz(frequency: Int): Boolean = frequency in 2400..2500

    /**
     * Determina si es banda de 6 GHz (WiFi 6E).
     */
    fun is6GHz(frequency: Int): Boolean = frequency >= 5925

    /**
     * Obtiene nombre descriptivo de la banda.
     */
    fun getBandName(frequency: Int): String {
        return when {
            is2_4GHz(frequency) -> "2.4 GHz"
            is5GHz(frequency) -> "5 GHz"
            is6GHz(frequency) -> "6 GHz (WiFi 6E)"
            else -> "Unknown"
        }
    }

    /**
     * Formatea RSSI para visualización.
     */
    fun formatRssi(rssi: Int): String = "$rssi dBm"

    /**
     * Formatea velocidad de enlace.
     */
    fun formatLinkSpeed(speed: Int?): String = speed?.let { "$it Mbps" } ?: "Unknown"

    /**
     * Debounce utility para prevenir spam de acciones.
     * Thread-safe para uso en ViewModels.
     */
    class ActionDebouncer(private val minIntervalMs: Long = 1000L) {
        @Volatile
        private var lastActionTime = 0L

        fun canExecute(): Boolean {
            val now = System.currentTimeMillis()
            return if (now - lastActionTime >= minIntervalMs) {
                lastActionTime = now
                true
            } else {
                false
            }
        }

        fun execute(block: () -> Unit): Boolean {
            return if (canExecute()) {
                block()
                true
            } else {
                false
            }
        }

        fun getRemainingTime(): Long {
            val now = System.currentTimeMillis()
            val elapsed = now - lastActionTime
            return (minIntervalMs - elapsed).coerceAtLeast(0L)
        }
    }

    /**
     * Genera recomendación contextual basada en condiciones actuales.
     */
    fun generateRecommendation(stability: SignalStability): String {
        return when (stability.classification) {
            StabilityClassification.EXCELLENT -> 
                "Señal excelente y estable. Ideal para streaming 4K y gaming."
            StabilityClassification.GOOD -> 
                "Buena señal estable. Perfecta para trabajo remoto y videollamadas."
            StabilityClassification.FAIR -> 
                when (stability.trend) {
                    SignalTrend.DEGRADING -> "Señal aceptable pero degradándose. Considera acercarte al router."
                    SignalTrend.IMPROVING -> "Señal aceptable y mejorando."
                    else -> "Señal aceptable. Funciona para navegación y streaming HD."
                }
            StabilityClassification.UNSTABLE -> 
                when (stability.trend) {
                    SignalTrend.DEGRADING -> "Señal inestable y degradándose. Acércate al router o verifica interferencias."
                    else -> "Señal inestable. Posible interferencia o obstáculos. Intenta cambiar de ubicación."
                }
        }
    }

    /**
     * Calcula throughput estimado basado en señal y velocidad de enlace.
     * Esto es una estimación conservadora de la velocidad real de transferencia.
     */
    fun estimateThroughput(signalLevel: SignalLevel, linkSpeed: Int?): String {
        val baseSpeed = linkSpeed ?: 54
        
        // Factores de eficiencia real basados en condiciones de señal
        val efficiencyFactor = when (signalLevel) {
            SignalLevel.EXCELLENT -> 0.75  // 75% eficiencia real
            SignalLevel.GOOD -> 0.60
            SignalLevel.FAIR -> 0.45
            SignalLevel.WEAK -> 0.25
            SignalLevel.POOR -> 0.10
        }
        
        val realSpeed = (baseSpeed * efficiencyFactor).toInt()
        
        return when {
            realSpeed >= 100 -> "> ${realSpeed} Mbps (Excelente)"
            realSpeed >= 50 -> "${realSpeed} Mbps (Bueno)"
            realSpeed >= 20 -> "${realSpeed} Mbps (Aceptable)"
            realSpeed >= 5 -> "${realSpeed} Mbps (Lento)"
            else -> "< 5 Mbps (Muy lento)"
        }
    }

    /**
     * Limpia el historial de señal.
     * Útil cuando cambia la conexión a una red diferente.
     */
    fun clearHistory() {
        signalHistory.clear()
        Log.d(TAG, "Historial de señal limpiado")
    }
}

/**
 * Data class con información completa de estabilidad de señal.
 */
data class SignalStability(
    val currentRssi: Int,
    val meanRssi: Int,
    val standardDeviation: Int,
    val coefficientOfVariation: Double,
    val trend: SignalTrend,
    val sampleSize: Int,
    val classification: StabilityClassification
) {
    /**
     * Indica si la señal es estable para aplicaciones sensibles a latencia.
     */
    val isStableForRealtime: Boolean = 
        classification == StabilityClassification.EXCELLENT || 
        classification == StabilityClassification.GOOD
    
    /**
     * Indica si la señal es usable para navegación básica.
     */
    val isUsable: Boolean = 
        classification != StabilityClassification.UNSTABLE || 
        (classification == StabilityClassification.UNSTABLE && trend != SignalTrend.DEGRADING)
}

enum class SignalTrend {
    IMPROVING,    // Señal mejorando
    STABLE,       // Sin cambio significativo
    DEGRADING     // Señal degradándose
}

enum class StabilityClassification {
    EXCELLENT,    // CV < 5%, estable
    GOOD,         // CV < 15%, estable
    FAIR,         // CV < 30%, aceptable
    UNSTABLE      // CV >= 30%, inestable
}
