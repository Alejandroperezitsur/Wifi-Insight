package com.example.wifiinsight.data.repository

import com.example.wifiinsight.data.model.ConnectionState
import com.example.wifiinsight.data.model.WifiNetwork
import kotlinx.coroutines.flow.Flow

interface WifiRepository {
    fun scanNetworks(): Flow<Result<List<WifiNetwork>>>
    fun getCurrentConnection(): Flow<ConnectionState>
    fun monitorSignalStrength(): Flow<Int>
    fun connectToNetwork(network: WifiNetwork, password: String?): Flow<Result<Boolean>>
    fun isWifiEnabled(): Boolean
    fun setWifiEnabled(enabled: Boolean)
    fun getScanHistory(): List<WifiNetwork>
    fun clearScanHistory()
}
