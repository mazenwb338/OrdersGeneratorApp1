package com.example.ordersgeneratorapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.example.ordersgeneratorapp.data.AppSettings
import com.example.ordersgeneratorapp.data.ConnectionSettings
import com.example.ordersgeneratorapp.repository.AlpacaRepository
import com.example.ordersgeneratorapp.screens.MainScreen
import com.example.ordersgeneratorapp.ui.theme.OrdersGeneratorAppTheme
import com.example.ordersgeneratorapp.util.SettingsManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val settingsManager = remember { SettingsManager(this) }
            val repository = remember { AlpacaRepository.getInstance(settingsManager) }

            val connectionSettings by settingsManager.connectionSettings.collectAsState(initial = ConnectionSettings())
            val appSettings by settingsManager.appSettings.collectAsState(initial = AppSettings())

            val navController = rememberNavController()
            val drawerState = rememberDrawerState(DrawerValue.Closed)
            val scope = rememberCoroutineScope()

            val destinations = listOf(
                "dashboard" to "Dashboard",
                "market_data" to "Market Data",
                "order_placement" to "Place Order",
                "order_history" to "Order History",
                "connection_settings" to "Connection Settings",
                "hotkey_settings" to "Hotkeys"
            )

            OrdersGeneratorAppTheme {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            Text(
                                "Navigate",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(16.dp)
                            )
                            destinations.forEach { (route, label) ->
                                NavigationDrawerItem(
                                    label = { Text(label) },
                                    selected = false,
                                    onClick = {
                                        scope.launch { drawerState.close() }
                                        navController.navigate(route) { launchSingleTop = true }
                                    },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                )
                            }
                        }
                    }
                ) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text("Orders Generator") },
                                navigationIcon = {
                                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                                    }
                                }
                            )
                        }
                    ) { paddingValues ->
                        MainScreen(
                            navController = navController,
                            connectionSettings = connectionSettings,
                            onSettingsChanged = { newCs ->
                                lifecycleScope.launch { settingsManager.saveConnectionSettings(newCs) }
                            },
                            appSettings = appSettings,
                            onAppSettingsChanged = { newApp ->
                                lifecycleScope.launch { settingsManager.saveAppSettings(newApp) }
                            },
                            alpacaRepository = repository,
                            contentPadding = paddingValues
                        )
                    }
                }
            }
        }
    }
}
