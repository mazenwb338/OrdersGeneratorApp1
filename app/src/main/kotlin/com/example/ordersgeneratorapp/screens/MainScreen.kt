package com.example.ordersgeneratorapp.screens

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.ordersgeneratorapp.data.AppSettings
import com.example.ordersgeneratorapp.data.ConnectionSettings
import com.example.ordersgeneratorapp.repository.AlpacaRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: androidx.navigation.NavHostController,
    connectionSettings: ConnectionSettings,
    onSettingsChanged: (ConnectionSettings) -> Unit,
    appSettings: AppSettings,
    onAppSettingsChanged: (AppSettings) -> Unit,
    alpacaRepository: AlpacaRepository,
    contentPadding: androidx.compose.foundation.layout.PaddingValues
) {
    NavHost(
        navController = navController,
        startDestination = "dashboard",
        modifier = Modifier
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
                onHotkeySettingsChanged = { hk ->
                    onAppSettingsChanged(appSettings.copy(hotkeySettings = hk))
                }
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
                alpacaRepository = alpacaRepository,
                onNavigateToSymbolDetail = { symbol ->
                    navController.navigate("symbol_detail/$symbol")
                }
            )
        }
        composable(
            route = "symbol_detail/{symbol}",
            arguments = listOf(navArgument("symbol") { type = NavType.StringType })
        ) { backStackEntry ->
            val symbol = backStackEntry.arguments?.getString("symbol") ?: ""
            SymbolDetailScreen(
                symbol = symbol,
                alpacaRepository = alpacaRepository,
                onBack = { navController.popBackStack() },
                onOrderHistory = { navController.navigate("order_history") }
            )
        }
    }
}