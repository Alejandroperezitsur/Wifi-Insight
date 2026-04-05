package com.example.wifiinsight.data.repository

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeWifiRepositoryTest {

    @Test
    fun scanNetworksPublishesStableBssidResults() = runBlocking {
        val repository = FakeWifiRepository()

        repository.scanNetworks()

        assertEquals(3, repository.uiState.value.scanResults.size)
        assertTrue(repository.uiState.value.scanResults.any { it.ssid == "<Red oculta>" })
        assertNotNull(repository.getNetworkByBssid("00:11:22:33:44:55"))
    }
}
