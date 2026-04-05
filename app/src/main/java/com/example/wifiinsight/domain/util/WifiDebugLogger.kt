package com.example.wifiinsight.domain.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object WifiDebugLogger {
    private const val MAX_LOGS = 200
    private val logs = ArrayDeque<String>(MAX_LOGS)
    private val _logsFlow = MutableStateFlow<List<String>>(emptyList())
    val logsFlow: StateFlow<List<String>> = _logsFlow.asStateFlow()

    @Synchronized
    fun log(event: String) {
        val timestamp = System.currentTimeMillis()
        logs.addLast("$timestamp: $event")
        if (logs.size > MAX_LOGS) {
            logs.removeFirst()
        }
        _logsFlow.value = logs.toList()
    }

    @Synchronized
    fun getLogs(): List<String> = logs.toList()
}
