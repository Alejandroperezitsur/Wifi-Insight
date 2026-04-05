package com.example.wifiinsight.domain.usecase

import com.example.wifiinsight.data.model.ConnectionState
import com.example.wifiinsight.data.model.WifiState
import com.example.wifiinsight.data.repository.WifiRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class GetCurrentConnectionUseCase(private val repository: WifiRepository) {
    operator fun invoke(): Flow<ConnectionState> {
        // Use the repository state to get current connection
        return (repository as? com.example.wifiinsight.data.repository.WifiRepositoryImpl)?.state?.map { state: WifiState ->
            if (state.isConnected) {
                ConnectionState.Connected(
                    ssid = state.ssid ?: "Unknown",
                    bssid = state.bssid ?: "Unknown",
                    rssi = state.signalStrength ?: -100,
                    linkSpeed = state.linkSpeed ?: 0,
                    ipAddress = state.ipAddress,
                    internetStatus = state.internetStatus
                )
            } else {
                ConnectionState.Disconnected
            }
        } ?: flowOf(ConnectionState.Disconnected)
    }
}
