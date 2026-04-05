package com.example.wifiinsight.presentation.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.wifiinsight.presentation.screens.detail.NetworkDetailScreen
import com.example.wifiinsight.presentation.screens.home.HomeScreen
import com.example.wifiinsight.presentation.screens.scan.ScanScreen
import android.content.Intent
import android.provider.Settings
import android.widget.Toast

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    
    val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.NEARBY_WIFI_DEVICES)
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    
    fun hasRequiredPermissions(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
    
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
                        if (hasRequiredPermissions()) {
                            try {
                                navController.navigate(Screen.Scan.route)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error al navegar: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(
                                context,
                                "Se requieren permisos de ubicación para escanear redes. Ve a Ajustes > Aplicaciones > WiFi Insight > Permisos",
                                Toast.LENGTH_LONG
                            ).show()
                            openAppSettings()
                        }
                    }
                )
            }

            composable(Screen.Scan.route) {
                ScanScreen(
                    onNavigateToDetail = { networkId ->
                        try {
                            navController.navigate(Screen.Detail.createRoute(networkId))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            composable(Screen.Detail.route) { backStackEntry ->
                val networkId = backStackEntry.arguments?.getString("networkId") ?: ""
                // FIX CRÍTICO #5: Pasar SavedStateHandle real del backStackEntry
                NetworkDetailScreen(
                    networkId = networkId,
                    savedStateHandle = backStackEntry.savedStateHandle,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
