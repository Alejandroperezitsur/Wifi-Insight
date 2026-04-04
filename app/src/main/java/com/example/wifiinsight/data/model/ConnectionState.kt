package com.example.wifiinsight.data.model

sealed class ConnectionState {
    abstract val message: String

    data object Disconnected : ConnectionState() {
        override val message: String = "Desconectado"
    }

    data object Scanning : ConnectionState() {
        override val message: String = "Buscando redes..."
    }

    data object Connecting : ConnectionState() {
        override val message: String = "Conectando..."
    }

    data class Connected(
        val ssid: String,
        val ipAddress: String? = null,
        val linkSpeed: Int? = null,
        val rssi: Int? = null
    ) : ConnectionState() {
        override val message: String = "Conectado a $ssid"
    }

    data class Error(val error: String) : ConnectionState() {
        override val message: String = "Error: $error"
    }
}
