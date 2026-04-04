package com.example.wifiinsight.domain.usecase

import com.example.wifiinsight.data.model.WifiNetwork
import com.example.wifiinsight.data.repository.WifiRepository
import kotlinx.coroutines.flow.Flow

class ScanWifiNetworksUseCase(private val repository: WifiRepository) {
    operator fun invoke(): Flow<Result<List<WifiNetwork>>> {
        return repository.scanNetworks()
    }
}
