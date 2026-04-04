package com.example.wifiinsight.domain.usecase

import com.example.wifiinsight.data.repository.WifiRepository
import kotlinx.coroutines.flow.Flow

class MonitorSignalUseCase(private val repository: WifiRepository) {
    operator fun invoke(): Flow<Int> {
        return repository.monitorSignalStrength()
    }
}
