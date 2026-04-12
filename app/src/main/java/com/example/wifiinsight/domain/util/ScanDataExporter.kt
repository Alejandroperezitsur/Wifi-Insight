package com.example.wifiinsight.domain.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.example.wifiinsight.data.model.WifiNetwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utilidad para exportar datos de escaneo WiFi.
 * Soporta formatos JSON y CSV.
 */
class ScanDataExporter(private val context: Context) {
    companion object {
        private const val TAG = "ScanDataExporter"
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
        private val CSV_HEADER = "SSID,BSSID,RSSI (dBm),Frequency (MHz),Band,Channel,Security,Signal %,Timestamp\n"
    }

    sealed class ExportResult {
        data class Success(val file: File, val uri: Uri) : ExportResult()
        data class Error(val message: String) : ExportResult()
    }

    /**
     * Exporta la lista de redes a formato JSON.
     */
    suspend fun exportToJson(networks: List<WifiNetwork>): ExportResult = withContext(Dispatchers.IO) {
        try {
            val jsonArray = JSONArray()
            
            networks.forEach { network ->
                val jsonObject = JSONObject().apply {
                    put("ssid", network.ssid)
                    put("bssid", network.bssid)
                    put("rssi", network.rssi)
                    put("signalPercentage", SignalCalculator.rssiToPercentage(network.rssi))
                    put("frequency", network.frequency)
                    put("band", SignalCalculator.getBandName(network.frequency))
                    put("channel", SignalCalculator.frequencyToChannel(network.frequency))
                    put("securityType", network.securityType.name)
                    put("securityDisplay", network.securityType.displayName())
                    put("capabilities", network.capabilities)
                    put("timestamp", network.timestamp)
                    put("timestampFormatted", formatTimestamp(network.timestamp))
                }
                jsonArray.put(jsonObject)
            }

            val rootObject = JSONObject().apply {
                put("exportDate", formatTimestamp(System.currentTimeMillis()))
                put("totalNetworks", networks.size)
                put("networks", jsonArray)
            }

            val fileName = "wifi_scan_${DATE_FORMAT.format(Date())}.json"
            saveToDownloads(fileName, rootObject.toString(2))

        } catch (e: Exception) {
            ExportResult.Error("Error al exportar JSON: ${e.message}")
        }
    }

    /**
     * Exporta la lista de redes a formato CSV.
     */
    suspend fun exportToCsv(networks: List<WifiNetwork>): ExportResult = withContext(Dispatchers.IO) {
        try {
            val sb = StringBuilder()
            sb.append(CSV_HEADER)

            networks.forEach { network ->
                val line = buildString {
                    append(escapeCsv(network.ssid))
                    append(",")
                    append(network.bssid)
                    append(",")
                    append(network.rssi)
                    append(",")
                    append(network.frequency)
                    append(",")
                    append(SignalCalculator.getBandName(network.frequency))
                    append(",")
                    append(SignalCalculator.frequencyToChannel(network.frequency))
                    append(",")
                    append(network.securityType.displayName())
                    append(",")
                    append(SignalCalculator.rssiToPercentage(network.rssi))
                    append(",")
                    append(formatTimestamp(network.timestamp))
                    append("\n")
                }
                sb.append(line)
            }

            val fileName = "wifi_scan_${DATE_FORMAT.format(Date())}.csv"
            saveToDownloads(fileName, sb.toString())

        } catch (e: Exception) {
            ExportResult.Error("Error al exportar CSV: ${e.message}")
        }
    }

    /**
     * Analiza la congestión de canales WiFi.
     * Retorna un mapa de canales a cantidad de redes.
     */
    fun analyzeChannelCongestion(networks: List<WifiNetwork>): ChannelAnalysis {
        val channelMap2_4GHz = mutableMapOf<Int, Int>()
        val channelMap5GHz = mutableMapOf<Int, Int>()

        networks.forEach { network ->
            val channel = SignalCalculator.frequencyToChannel(network.frequency)
            val is5GHz = SignalCalculator.is5GHz(network.frequency)
            
            if (is5GHz) {
                channelMap5GHz[channel] = channelMap5GHz.getOrDefault(channel, 0) + 1
            } else {
                channelMap2_4GHz[channel] = channelMap2_4GHz.getOrDefault(channel, 0) + 1
            }
        }

        // Canales recomendados para 2.4GHz (1, 6, 11 son no solapados)
        val recommended2_4GHz = listOf(1, 6, 11).filter { !channelMap2_4GHz.containsKey(it) }
        
        // Para 5GHz hay más canales disponibles, generalmente menos congestionados
        val leastCongested5GHz = channelMap5GHz.entries
            .sortedBy { it.value }
            .take(3)
            .map { it.key }

        return ChannelAnalysis(
            congestion2_4GHz = channelMap2_4GHz.toMap(),
            congestion5GHz = channelMap5GHz.toMap(),
            recommendedChannels2_4GHz = recommended2_4GHz,
            recommendedChannels5GHz = leastCongested5GHz,
            mostCongested2_4GHz = channelMap2_4GHz.maxByOrNull { it.value }?.toPair(),
            mostCongested5GHz = channelMap5GHz.maxByOrNull { it.value }?.toPair()
        )
    }

    private fun saveToDownloads(fileName: String, content: String): ExportResult {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, fileName)

        FileWriter(file).use { writer ->
            writer.write(content)
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        return ExportResult.Success(file, uri)
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(timestamp))
    }
}

/**
 * Análisis de congestión de canales WiFi.
 */
data class ChannelAnalysis(
    val congestion2_4GHz: Map<Int, Int>,
    val congestion5GHz: Map<Int, Int>,
    val recommendedChannels2_4GHz: List<Int>,
    val recommendedChannels5GHz: List<Int>,
    val mostCongested2_4GHz: Pair<Int, Int>?,
    val mostCongested5GHz: Pair<Int, Int>?
) {
    fun getRecommendationText(): String {
        val recommendations = mutableListOf<String>()

        if (recommendedChannels2_4GHz.isNotEmpty()) {
            recommendations.add(
                "Canales 2.4GHz recomendados: ${recommendedChannels2_4GHz.joinToString(", ")}"
            )
        }

        mostCongested2_4GHz?.let { (channel, count) ->
            if (count > 3) {
                recommendations.add(
                    "Canal 2.4GHz $channel muy congestionado ($count redes). Considera cambiar."
                )
            }
        }

        return if (recommendations.isEmpty()) {
            "No hay congestión significativa detectada."
        } else {
            recommendations.joinToString("\n")
        }
    }
}
