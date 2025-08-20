package com.example.ordersgeneratorapp.screens

import android.util.Log
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ordersgeneratorapp.data.HotkeyPreset
import com.example.ordersgeneratorapp.data.HotkeyConfig
import com.example.ordersgeneratorapp.repository.AlpacaRepository
import com.example.ordersgeneratorapp.util.SettingsManager
import com.example.ordersgeneratorapp.hotkey.HotkeyOrderProcessor
import com.example.ordersgeneratorapp.hotkey.HotkeyManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MarketDataScreen(
    onBackClick: () -> Unit,
    alpacaRepository: AlpacaRepository
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    Log.d("MarketDataScreen", "SCREEN_LAUNCHED_SUCCESSFULLY")
    
    // Create independent hotkey processing system
    val hotkeyProcessor = remember { HotkeyOrderProcessor(alpacaRepository) }
    val hotkeyManager = remember { HotkeyManager(hotkeyProcessor) }
    
    // Load connection settings and hotkey presets
    val connectionSettings = remember { settingsManager.getConnectionSettings() }
    val hotkeyPresets = remember { settingsManager.getHotkeyPresets() }
    
    // State for execution results
    var lastExecutionResult by remember { mutableStateOf<com.example.ordersgeneratorapp.hotkey.HotkeyExecutionResult?>(null) }
    
    // Hotkey execution function using new independent system
    fun executeHotkey(preset: HotkeyPreset, side: String) {
        Log.d("MarketDataScreen", "HOTKEY_TRIGGER preset=${preset.name} side=$side")
        
        scope.launch {
            hotkeyManager.executeHotkey(
                preset = preset,
                side = side,
                connectionSettings = connectionSettings
            ) { result ->
                lastExecutionResult = result
                
                // Show snackbar with results
                scope.launch {
                    val message = when {
                        result.isFullSuccess -> "✅ ${result.summary}"
                        result.hasPartialSuccess -> "⚠️ ${result.summary}"
                        result.isCompleteFailure -> "❌ ${result.summary}"
                        else -> "🔄 ${result.summary}"
                    }
                    
                    snackbarHostState.showSnackbar(
                        message = message,
                        duration = if (result.isFullSuccess) SnackbarDuration.Short else SnackbarDuration.Long
                    )
                }
            }
        }
    }
    
    // Keyboard event handler
    fun handleKeyEvent(event: KeyEvent): Boolean {
        if (event.type == KeyEventType.KeyDown) {
            for (preset in hotkeyPresets) {
                if (preset.buyHotkey.matches(event)) {
                    Log.d("MarketDataScreen", "BUY_HOTKEY_DETECTED: ${preset.name}")
                    executeHotkey(preset, "buy")
                    return true
                }
                if (preset.sellHotkey.matches(event)) {
                    Log.d("MarketDataScreen", "SELL_HOTKEY_DETECTED: ${preset.name}")
                    executeHotkey(preset, "sell")
                    return true
                }
            }
        }
        return false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Market Data - Testing") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .focusable()
                .onKeyEvent { event ->
                    handleKeyEvent(event)
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Status Card
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Independent Hotkey System",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = buildString {
                                append("🚀 Independent order processing module active\n")
                                append("✅ ${connectionSettings.brokerAccounts.size} broker accounts configured\n")
                                append("✅ ${hotkeyPresets.size} hotkey presets loaded\n")
                                append("🔄 Each order gets unique Alpaca server ID\n")
                                append("📊 Detailed execution tracking & logging")
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                // Last Execution Result Card
                lastExecutionResult?.let { result ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                result.isFullSuccess -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                                result.hasPartialSuccess -> Color(0xFFFF9800).copy(alpha = 0.1f)
                                else -> Color(0xFFF44336).copy(alpha = 0.1f)
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Last Execution Results",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Session: ${result.sessionId}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = result.summary,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            
                            if (result.accountResults.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                result.accountResults.forEach { accountResult ->
                                    Text(
                                        text = accountResult.displaySummary,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Test Trading Buttons
                if (hotkeyPresets.isNotEmpty()) {
                    hotkeyPresets.take(3).forEach { preset ->
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = preset.name.ifEmpty { "Unnamed Preset" },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${preset.symbol} × ${preset.quantity} (${preset.orderType})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Buy Button
                                    Button(
                                        onClick = { 
                                            Log.d("MarketDataScreen", "MANUAL_BUY_PRESSED: ${preset.name}")
                                            executeHotkey(preset, "buy")
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF4CAF50)
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(56.dp)
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "BUY",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = preset.buyHotkey.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.White
                                            )
                                        }
                                    }
                                    
                                    // Sell Button
                                    Button(
                                        onClick = { 
                                            Log.d("MarketDataScreen", "MANUAL_SELL_PRESSED: ${preset.name}")
                                            executeHotkey(preset, "sell")
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFF44336)
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(56.dp)
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "SELL",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = preset.sellHotkey.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No hotkey presets configured",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Go to Settings → Hotkey Settings to add your first hotkey preset",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                Button(
                    onClick = { 
                        Log.d("MarketDataScreen", "TEST_BUTTON_PRESSED") 
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Test Button - Check Logs")
                }
                
                // Show connection settings info
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Settings Status",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Accounts: ${connectionSettings.brokerAccounts.size}\n" +
                                  "Alpaca accounts: ${connectionSettings.brokerAccounts.count { it.brokerType == "Alpaca" }}\n" +
                                  "Enabled accounts: ${connectionSettings.brokerAccounts.count { it.isEnabled }}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

// Extension function to check if hotkey matches event
private fun HotkeyConfig.matches(event: KeyEvent): Boolean {
    val eventKey = when (event.key) {
        Key.A -> "A"; Key.B -> "B"; Key.C -> "C"; Key.D -> "D"; Key.E -> "E"
        Key.F -> "F"; Key.G -> "G"; Key.H -> "H"; Key.I -> "I"; Key.J -> "J"
        Key.K -> "K"; Key.L -> "L"; Key.M -> "M"; Key.N -> "N"; Key.O -> "O"
        Key.P -> "P"; Key.Q -> "Q"; Key.R -> "R"; Key.S -> "S"; Key.T -> "T"
        Key.U -> "U"; Key.V -> "V"; Key.W -> "W"; Key.X -> "X"; Key.Y -> "Y"
        Key.Z -> "Z"; Key.One -> "1"; Key.Two -> "2"; Key.Three -> "3"
        Key.Four -> "4"; Key.Five -> "5"; Key.Six -> "6"; Key.Seven -> "7"
        Key.Eight -> "8"; Key.Nine -> "9"; Key.Zero -> "0"
        else -> return false
    }
    
    return eventKey == this.key &&
           event.isCtrlPressed == this.ctrl &&
           event.isAltPressed == this.alt &&
           event.isShiftPressed == this.shift
}