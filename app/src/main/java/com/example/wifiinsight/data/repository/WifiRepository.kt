package com.example.wifiinsight.data.repository

import android.app.Activity
import com.example.wifiinsight.data.model.WifiNetwork
import com.example.wifiinsight.data.model.WifiState
import kotlinx.coroutines.flow.StateFlow

interface WifiRepository {
    val uiState: StateFlow<WifiState>

    suspend fun scanNetworks()

    suspend fun reEvaluateConnection()

    suspend fun connectToNetwork(network: WifiNetwork, password: String?): Result<String>

    fun refreshSystemState(activity: Activity? = null)

    fun refreshPermissions(activity: Activity? = null)

    fun softReset()

    fun markPermissionRequested()

    fun clearError()

    fun openWifiSettings(): Boolean

    fun openAppSettings(): Boolean

    fun openLocationSettings(): Boolean

    fun getNetworkByBssid(bssid: String): WifiNetwork?
}
