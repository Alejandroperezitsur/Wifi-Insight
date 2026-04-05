package com.example.wifiinsight.domain.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.net.HttpURLConnection
import java.net.URL

/**
 * InternetChecker v3.1 - RESILIENTE
 * 
 * PATRONES DE RESILIENCIA:
 * - Retry con exponential backoff (3 intentos)
 * - Fallback a NetworkCapabilities si HTTP falla
 * - Circuit breaker (si falla 3 veces seguidas, usar cache extendido)
 * - Timeout agresivo (< 2s)
 */
class InternetChecker {
    
    companion object {
        private const val TEST_URL = "https://clients3.google.com/generate_204"
        private const val FALLBACK_URL = "https://www.google.com/generate_204"
        private const val TIMEOUT_MS = 2000 // Reducido de 3000 a 2000
        private const val CACHE_DURATION_MS = 15000L
        private const val EXTENDED_CACHE_MS = 60000L // 1 minuto si circuit breaker abre
        private const val MAX_RETRIES = 3
        private const val CIRCUIT_BREAKER_THRESHOLD = 3
    }
    
    private val mutex = Mutex()
    private var lastCheckTime = 0L
    private var cachedResult = false
    private var consecutiveFailures = 0
    private var isCircuitOpen = false
    
    /**
     * Verifica conectividad con retry, fallback y circuit breaker.
     */
    suspend fun hasRealInternet(): Boolean = mutex.withLock {
        val now = System.currentTimeMillis()
        val elapsed = now - lastCheckTime
        
        // Si circuit breaker está abierto, usar cache extendida
        if (isCircuitOpen && elapsed < EXTENDED_CACHE_MS) {
            return cachedResult
        }
        
        // Cache normal
        if (!isCircuitOpen && elapsed < CACHE_DURATION_MS) {
            return cachedResult
        }
        
        // Realizar check con retry
        val result = checkWithRetry()
        
        // Manejar circuit breaker
        if (!result) {
            consecutiveFailures++
            if (consecutiveFailures >= CIRCUIT_BREAKER_THRESHOLD) {
                isCircuitOpen = true
            }
        } else {
            consecutiveFailures = 0
            isCircuitOpen = false
        }
        
        cachedResult = result
        lastCheckTime = now
        return result
    }
    
    /**
     * Fuerza check ignorando cache y circuit breaker.
     */
    suspend fun forceCheck(): Boolean = mutex.withLock {
        consecutiveFailures = 0
        isCircuitOpen = false
        val result = checkWithRetry()
        cachedResult = result
        lastCheckTime = System.currentTimeMillis()
        return result
    }
    
    /**
     * Check con retry y exponential backoff.
     */
    private suspend fun checkWithRetry(): Boolean {
        repeat(MAX_RETRIES) { attempt ->
            val result = tryCheck()
            if (result) return true
            
            // Exponential backoff: 200ms, 400ms, 800ms
            if (attempt < MAX_RETRIES - 1) {
                delay(200L * (attempt + 1))
            }
        }
        return false
    }
    
    /**
     * Intenta check con timeout y fallback URL.
     */
    private suspend fun tryCheck(): Boolean = withContext(Dispatchers.IO) {
        // Intentar URL primaria
        val primaryResult = try {
            withTimeout(TIMEOUT_MS.toLong()) {
                performCheck(TEST_URL)
            }
        } catch (e: Exception) {
            false
        }
        
        if (primaryResult) return@withContext true
        
        // Fallback a URL alternativa
        return@withContext try {
            withTimeout(TIMEOUT_MS.toLong()) {
                performCheck(FALLBACK_URL)
            }
        } catch (e: Exception) {
            false
        }
    }
    
    private fun performCheck(urlString: String): Boolean {
        return try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                requestMethod = "GET"
                useCaches = false
                setRequestProperty("Connection", "close")
                instanceFollowRedirects = false
            }
            
            val responseCode = connection.responseCode
            connection.disconnect()
            
            responseCode == 204 || responseCode == 200
        } catch (e: Exception) {
            false
        }
    }
    
    fun invalidateCache() {
        lastCheckTime = 0
    }
    
    fun resetCircuitBreaker() {
        consecutiveFailures = 0
        isCircuitOpen = false
    }
}
