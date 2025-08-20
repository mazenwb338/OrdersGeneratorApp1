package com.example.ordersgeneratorapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.ordersgeneratorapp.data.AppSettings
import com.example.ordersgeneratorapp.data.ConnectionSettings
import com.example.ordersgeneratorapp.repository.AlpacaRepository
import com.example.ordersgeneratorapp.util.SettingsManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavHostController,
    connectionSettings: ConnectionSettings,
    onSettingsChanged: (ConnectionSettings) -> Unit,
    appSettings: AppSettings = AppSettings(),
    onAppSettingsChanged: (AppSettings) -> Unit = {},
    alpacaRepository: AlpacaRepository
) {
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
                connectionSettings = connectionSettings, // This should now have brokerAccounts
                onHotkeySettingsChanged = { newHotkeySettings ->
                    val updatedAppSettings = appSettings.copy(hotkeySettings = newHotkeySettings)
                    onAppSettingsChanged(updatedAppSettings)
                }
            )
        }
    }
}