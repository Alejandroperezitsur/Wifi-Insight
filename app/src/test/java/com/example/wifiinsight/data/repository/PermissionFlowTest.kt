package com.example.wifiinsight.data.repository

import com.example.wifiinsight.data.model.PermissionState
import org.junit.Assert.assertEquals
import org.junit.Test

class PermissionFlowTest {

    @Test
    fun refreshPermissionsPushesPermanentDenialToState() {
        val repository = FakeWifiRepository()
        repository.setPermissionState(PermissionState.PermanentlyDenied)

        repository.refreshPermissions(null)

        assertEquals(PermissionState.PermanentlyDenied, repository.uiState.value.permissionState)
    }
}
