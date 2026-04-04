package com.example.wifiinsight.domain.usecase

import com.example.wifiinsight.data.model.ConnectionState
import com.example.wifiinsight.data.repository.WifiRepository
import kotlinx.coroutines.flow.Flow

class GetCurrentConnectionUseCase(private val repository: WifiRepository) {
    operator fun invoke(): Flow<ConnectionState> {
        return repository.getCurrentConnection()
    }
}
