package com.example.wifiinsight.data.reducer

import com.example.wifiinsight.data.model.InternetStatus
import com.example.wifiinsight.data.model.PermissionState
import com.example.wifiinsight.data.model.UserAction
import com.example.wifiinsight.data.model.WifiEvent
import com.example.wifiinsight.data.model.WifiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WifiStateReducerTest {

    @Test
    fun wifiDisabledClearsConnectionState() {
        val initialState = WifiState(
            wifiEnabled = true,
            isConnected = true,
            ssid = "OfficeWiFi",
            bssid = "00:11:22:33:44:55",
            internetStatus = InternetStatus.AVAILABLE,
            signalStrength = -50,
            signalHistory = listOf(-48, -50)
        )

        val reducedState = WifiStateReducer.reduce(
            initialState,
            WifiEvent.WifiToggled(false)
        )

        assertFalse(reducedState.wifiEnabled)
        assertFalse(reducedState.isConnected)
        assertNull(reducedState.ssid)
        assertNull(reducedState.bssid)
        assertEquals(InternetStatus.UNKNOWN, reducedState.internetStatus)
        assertTrue(reducedState.signalHistory.isEmpty())
    }

    @Test
    fun permissionUpdateWritesSingleSourceState() {
        val reducedState = WifiStateReducer.reduce(
            WifiState(),
            WifiEvent.PermissionUpdated(PermissionState.PermanentlyDenied)
        )

        assertEquals(PermissionState.PermanentlyDenied, reducedState.permissionState)
    }

    @Test
    fun disconnectedConnectionClearsImpossibleState() {
        val initialState = WifiState(
            wifiEnabled = true,
            isConnected = true,
            ssid = "OfficeWiFi",
            bssid = "00:11:22:33:44:55",
            ipAddress = "192.168.1.10",
            linkSpeed = 433,
            signalStrength = -48,
            signalHistory = listOf(-48, -50),
            internetStatus = InternetStatus.AVAILABLE
        )

        val reducedState = WifiStateReducer.reduce(
            initialState,
            WifiEvent.ConnectionChanged(null, null, null, null, null, false)
        )

        assertFalse(reducedState.isConnected)
        assertNull(reducedState.ssid)
        assertNull(reducedState.bssid)
        assertNull(reducedState.ipAddress)
        assertNull(reducedState.linkSpeed)
        assertNull(reducedState.signalStrength)
        assertTrue(reducedState.signalHistory.isEmpty())
        assertEquals(InternetStatus.UNKNOWN, reducedState.internetStatus)
    }

    @Test
    fun connectionRefreshEventsToggleVisibleFeedback() {
        val startedState = WifiStateReducer.reduce(
            WifiState(),
            WifiEvent.ConnectionRefreshStarted
        )
        val finishedState = WifiStateReducer.reduce(
            startedState,
            WifiEvent.ConnectionRefreshFinished
        )

        assertTrue(startedState.isRefreshingConnection)
        assertFalse(finishedState.isRefreshingConnection)
    }

    @Test
    fun actionTimeoutAddsVisibleErrorOnlyForActiveAction() {
        val startedState = WifiStateReducer.reduce(
            WifiState(),
            WifiEvent.ActionStarted(UserAction.Scan, token = 7L)
        )

        val timedOutState = WifiStateReducer.reduce(
            startedState,
            WifiEvent.ActionTimeout(UserAction.Scan, token = 7L)
        )

        assertTrue(timedOutState.isProcessing)
        assertEquals("Tardó demasiado. Intenta de nuevo", timedOutState.errorQueue.firstOrNull()?.message)
        assertEquals(7L, timedOutState.activeActionToken)
    }
}
