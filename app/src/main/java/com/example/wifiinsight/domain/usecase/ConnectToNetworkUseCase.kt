package com.example.wifiinsight.domain.usecase

import com.example.wifiinsight.data.model.WifiNetwork
import com.example.wifiinsight.data.repository.WifiRepository
import kotlinx.coroutines.flow.Flow

class ConnectToNetworkUseCase(private val repository: WifiRepository) {
    operator fun invoke(network: WifiNetwork, password: String? = null): Flow<Result<Boolean>> {
        return repository.connectToNetwork(network, password)
    }
}
