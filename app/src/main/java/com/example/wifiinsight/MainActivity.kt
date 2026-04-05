package com.example.wifiinsight

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.wifiinsight.presentation.navigation.AppNavigation
import com.example.wifiinsight.ui.theme.WifiInsightTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            WifiInsightTheme {
                AppNavigation()
            }
        }
    }
}
