package com.example.wifiinsight.presentation.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.wifiinsight.presentation.screens.detail.NetworkDetailScreen
import com.example.wifiinsight.presentation.screens.home.HomeScreen
import com.example.wifiinsight.presentation.screens.scan.ScanScreen
import android.net.Uri

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToScan = {
                        navController.navigate(Screen.Scan.route)
                    }
                )
            }

            composable(Screen.Scan.route) {
                ScanScreen(
                    onNavigateToDetail = { bssid ->
                        navController.navigate(Screen.Detail.createRoute(Uri.encode(bssid)))
                    }
                )
            }

            composable(Screen.Detail.route) {
                NetworkDetailScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
