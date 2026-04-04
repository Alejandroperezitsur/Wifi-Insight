package com.example.wifiinsight.presentation.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Scan : Screen("scan")
    data object Detail : Screen("detail/{networkId}") {
        fun createRoute(networkId: String) = "detail/$networkId"
    }
}
