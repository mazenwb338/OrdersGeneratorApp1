package com.example.ordersgeneratorapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ordersgeneratorapp.data.AppSettings
import com.example.ordersgeneratorapp.data.ConnectionSettings
import com.example.ordersgeneratorapp.repository.AlpacaRepository
import com.example.ordersgeneratorapp.screens.DashboardScreen
import com.example.ordersgeneratorapp.screens.OrderPlacementScreen
import com.example.ordersgeneratorapp.screens.OrderHistoryScreen
import com.example.ordersgeneratorapp.screens.MarketDataScreen
import com.example.ordersgeneratorapp.screens.ConnectionSettingsScreen
import com.example.ordersgeneratorapp.screens.HotkeySettingsScreen
import com.example.ordersgeneratorapp.util.SettingsManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OrdersGeneratorApp()
                }
            }
        }
    }
}

@Composable
fun OrdersGeneratorApp() {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val alpacaRepository = remember { AlpacaRepository(settingsManager) }
    val navController = rememberNavController()
    
    var connectionSettings by remember { 
        mutableStateOf(settingsManager.getConnectionSettings()) 
    }
    
    var appSettings by remember { 
        mutableStateOf(settingsManager.getAppSettings()) 
    }
    
    // Update repository when settings change
    LaunchedEffect(connectionSettings) {
        alpacaRepository.updateSettings(connectionSettings.alpaca)
    }

    LaunchedEffect(connectionSettings.brokerAccounts) {
        alpacaRepository.ensureConfiguredForFirstEnabledAccount(connectionSettings)
        connectionSettings.brokerAccounts.forEach { acct ->
            alpacaRepository.configureFromBrokerAccount(acct)
        }
    }

    val onSettingsChanged: (ConnectionSettings) -> Unit = { newConnectionSettings ->
        // ✅ Update connectionSettings state AND save to SettingsManager
        connectionSettings = newConnectionSettings
        settingsManager.saveConnectionSettings(newConnectionSettings)
        
        // ✅ Force trigger recomposition by updating appSettings as well
        val updatedAppSettings = appSettings.copy(
            connectionSettings = newConnectionSettings
        )
        appSettings = updatedAppSettings
        settingsManager.saveAppSettings(updatedAppSettings)
        
        Log.d("MainActivity", "Updated connection settings with ${newConnectionSettings.brokerAccounts.size} broker accounts")
    }
    
    NavHost(
        navController = navController,
        startDestination = "dashboard"
    ) {
        composable("dashboard") {
            DashboardScreen(
                repository = alpacaRepository,
                onNavigateToSettings = { navController.navigate("connection_settings") },
                onNavigateToOrderPlacement = { navController.navigate("order_placement") },
                onNavigateToOrderHistory = { navController.navigate("order_history") },
                onNavigateToMarketData = { navController.navigate("market_data") },
                onNavigateToHotkeys = { navController.navigate("hotkey_settings") }
            )
        }
        
        composable("order_placement") {
            OrderPlacementScreen(
                onBackClick = { navController.popBackStack() },
                alpacaRepository = alpacaRepository
            )
        }
        
        composable("order_history") {
            OrderHistoryScreen(
                onBackClick = { navController.popBackStack() },
                alpacaRepository = alpacaRepository
            )
        }
        
        composable("market_data") {
            MarketDataScreen(
                onBackClick = { navController.popBackStack() },
                alpacaRepository = alpacaRepository
            )
        }
        
        composable("connection_settings") {
            ConnectionSettingsScreen(
                onBackClick = { navController.popBackStack() },
                connectionSettings = connectionSettings,
                onSettingsChanged = onSettingsChanged,
                onNavigateToHotkeys = { navController.navigate("hotkey_settings") }
            )
        }

        composable("hotkey_settings") {
            HotkeySettingsScreen(
                onBackClick = { navController.popBackStack() },
                hotkeySettings = appSettings.hotkeySettings,
                connectionSettings = connectionSettings,
                onHotkeySettingsChanged = { newHotkeySettings ->
                    val updatedAppSettings = appSettings.copy(hotkeySettings = newHotkeySettings)
                    appSettings = updatedAppSettings
                    settingsManager.saveAppSettings(updatedAppSettings)
                }
            )
        }
    }
}
