package com.example.wifiinsight.domain.usecase

import com.example.wifiinsight.data.model.WifiState
import com.example.wifiinsight.data.repository.WifiRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class MonitorSignalUseCase(private val repository: WifiRepository) {
    operator fun invoke(): Flow<Int> {
        // Use the repository state to monitor signal strength from WifiState
        return (repository as? com.example.wifiinsight.data.repository.WifiRepositoryImpl)?.state?.map { state: WifiState -> 
            state.signalStrength ?: 0
        } ?: flowOf(0)
    }
}
