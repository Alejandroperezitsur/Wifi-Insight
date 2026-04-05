package com.example.wifiinsight.presentation.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Scan : Screen("scan")
    data object Debug : Screen("debug")
    data object Detail : Screen("detail/{bssid}") {
        fun createRoute(bssid: String) = "detail/$bssid"
    }
}
