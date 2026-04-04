package com.example.wifiinsight.domain.util

import com.example.wifiinsight.data.model.SignalLevel
import kotlin.math.log10
import kotlin.math.pow

object SignalCalculator {

    fun rssiToPercentage(rssi: Int): Int {
        return SignalLevel.calculatePercentage(rssi)
    }

    fun rssiToSignalLevel(rssi: Int): SignalLevel {
        return SignalLevel.fromRssi(rssi)
    }

    fun calculateDistance(frequencyMHz: Int, rssi: Int): Double {
        val referenceRssiAt1Meter = when {
            frequencyMHz >= 5000 -> -35
            else -> -40
        }
        val pathLossExponent = 2.0

        return 10.0.pow((referenceRssiAt1Meter - rssi) / (10.0 * pathLossExponent))
    }

    fun frequencyToChannel(frequency: Int): Int {
        return when {
            frequency >= 2412 && frequency <= 2484 -> (frequency - 2407) / 5
            frequency >= 5170 && frequency <= 5825 -> (frequency - 5000) / 5
            else -> -1
        }
    }

    fun is5GHz(frequency: Int): Boolean {
        return frequency >= 5000
    }

    fun is2_4GHz(frequency: Int): Boolean {
        return frequency in 2412..2484
    }

    fun getBandName(frequency: Int): String {
        return when {
            is2_4GHz(frequency) -> "2.4 GHz"
            is5GHz(frequency) -> "5 GHz"
            else -> "Unknown"
        }
    }

    fun formatRssi(rssi: Int): String {
        return "${rssi} dBm"
    }

    fun formatLinkSpeed(speed: Int?): String {
        return speed?.let { "$it Mbps" } ?: "Unknown"
    }

    fun estimateMaxThroughput(signalLevel: SignalLevel, linkSpeed: Int?): String {
        val baseSpeed = linkSpeed ?: 54
        return when (signalLevel) {
            SignalLevel.EXCELLENT -> "${(baseSpeed * 0.8).toInt()} Mbps"
            SignalLevel.GOOD -> "${(baseSpeed * 0.6).toInt()} Mbps"
            SignalLevel.FAIR -> "${(baseSpeed * 0.4).toInt()} Mbps"
            SignalLevel.WEAK -> "${(baseSpeed * 0.2).toInt()} Mbps"
            SignalLevel.POOR -> "< ${(baseSpeed * 0.1).toInt()} Mbps"
        }
    }
}
