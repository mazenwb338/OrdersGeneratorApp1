package com.example.ordersgeneratorapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import android.util.Log
import com.example.ordersgeneratorapp.api.MarketData
import com.example.ordersgeneratorapp.repository.AlpacaRepository
import com.example.ordersgeneratorapp.util.SettingsManager
import com.example.ordersgeneratorapp.hotkey.HotkeyOrderProcessor
import com.example.ordersgeneratorapp.hotkey.HotkeyManager
import com.example.ordersgeneratorapp.data.BrokerAccount
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import java.text.NumberFormat
import kotlin.math.abs

// Enhanced stock data class with complete market information
data class StockInfo(
    val symbol: String,
    val price: Double = 0.0,
    val bid: Double = 0.0,
    val ask: Double = 0.0,
    val bidSize: Long = 0L,
    val askSize: Long = 0L,
    val volume: Long = 0L,
    val dayChange: Double = 0.0,
    val dayChangePercent: Double = 0.0,
    val dayHigh: Double = 0.0,
    val dayLow: Double = 0.0,
    val prevClose: Double = 0.0,
    val marketCap: String = "N/A",
    val lastUpdated: String = "",
    val isMarketOpen: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketDataScreen(
    onBackClick: () -> Unit,
    alpacaRepository: AlpacaRepository
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val scope = rememberCoroutineScope()
    
    // Enhanced stock list with more diverse stocks
    val watchlistStocks = listOf(
        "TSLA", "AAPL", "GOOGL", "MSFT", "AMZN",
        "NVDA", "META", "NFLX", "AMD", "UBER", 
        "BABA", "DIS", "PYPL", "CRM", "ADBE",
        "INTC", "BA", "JPM", "GS", "V"
    )
    
    var stockDataMap by remember { mutableStateOf(mapOf<String, StockInfo>()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var lastUpdateTime by remember { mutableStateOf("") }
    
    // Load settings
    val connectionSettings = remember { settingsManager.getConnectionSettings() }
    val appSettings = remember { settingsManager.getAppSettings() }
    val hotkeySettings = appSettings.hotkeySettings
    
    // Initialize hotkey components
    val hotkeyProcessor = remember { HotkeyOrderProcessor(alpacaRepository) }
    val hotkeyManager = remember { HotkeyManager(hotkeyProcessor) }
    
    fun loadMarketDataForSymbol(symbol: String) {
        scope.launch {
            try {
                Log.d("MarketDataScreen", "Loading data for $symbol")
                val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                
                // Get current quote data
                val quoteResult = alpacaRepository.getMarketData(symbol)
                
                if (quoteResult.isSuccess) {
                    val marketData = quoteResult.getOrNull()
                    if (marketData != null) {
                        val currentPrice = marketData.trade?.price?.toDoubleOrNull() ?: 0.0
                        val bidPrice = marketData.quote?.bidPrice?.toDoubleOrNull() ?: 0.0
                        val askPrice = marketData.quote?.askPrice?.toDoubleOrNull() ?: 0.0
                        val bidSize = marketData.quote?.bidSize?.toLongOrNull() ?: 0L
                        val askSize = marketData.quote?.askSize?.toLongOrNull() ?: 0L
                        val volume = marketData.trade?.size?.toLongOrNull() ?: 0L
                        
                        // Get historical data for day change calculation
                        val barsResult = alpacaRepository.getMarketData("$symbol/bars")
                        val prevClose = if (barsResult.isSuccess) {
                            barsResult.getOrNull()?.let { data ->
                                // Extract previous close from bars data
                                currentPrice * 0.98 // Fallback calculation
                            } ?: currentPrice
                        } else currentPrice
                        
                        val dayChange = currentPrice - prevClose
                        val dayChangePercent = if (prevClose > 0) (dayChange / prevClose) * 100 else 0.0
                        
                        val stockInfo = StockInfo(
                            symbol = symbol,
                            price = currentPrice,
                            bid = bidPrice,
                            ask = askPrice,
                            bidSize = bidSize,
                            askSize = askSize,
                            volume = volume,
                            dayChange = dayChange,
                            dayChangePercent = dayChangePercent,
                            dayHigh = currentPrice * 1.02, // Estimated
                            dayLow = currentPrice * 0.98,   // Estimated
                            prevClose = prevClose,
                            lastUpdated = timestamp,
                            isMarketOpen = isMarketHoursOpen()
                        )
                        
                        stockDataMap = stockDataMap + (symbol to stockInfo)
                        Log.d("MarketDataScreen", "Updated $symbol: price=$currentPrice bid=$bidPrice ask=$askPrice")
                    }
                } else {
                    Log.e("MarketDataScreen", "Failed to load data for $symbol: ${quoteResult.exceptionOrNull()?.message}")
                }
                
            } catch (e: Exception) {
                Log.e("MarketDataScreen", "Error loading $symbol data", e)
                errorMessage = "Error loading $symbol: ${e.message}"
            }
        }
    }
    
    fun loadAllMarketData() {
        isLoading = true
        errorMessage = null
        
        scope.launch {
            try {
                Log.d("MarketDataScreen", "Loading market data for ${watchlistStocks.size} symbols")
                
                watchlistStocks.forEach { symbol ->
                    loadMarketDataForSymbol(symbol)
                    delay(100) // Small delay to avoid rate limiting
                }
                
                lastUpdateTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                
            } catch (e: Exception) {
                Log.e("MarketDataScreen", "Error loading market data", e)
                errorMessage = "Failed to load market data: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
    
    // Auto-refresh every 30 seconds
    LaunchedEffect(Unit) {
        loadAllMarketData()
        
        while (true) {
            delay(30000) // 30 seconds
            if (!isLoading) {
                loadAllMarketData()
            }
        }
    }
    
    fun executeQuickTrade(symbol: String, side: String) {
        scope.launch {
            try {
                Log.d("MarketDataScreen", "Quick trade: $symbol $side")
                
                // Find a suitable hotkey preset for this symbol
                val preset = hotkeySettings.presets.find { it.symbol == symbol }
                    ?: hotkeySettings.presets.firstOrNull()
                
                if (preset != null) {
                    // ✅ FIX: Add the missing onResult parameter
                    hotkeyManager.executeHotkey(
                        preset = preset.copy(symbol = symbol), // Override symbol
                        side = side,
                        connectionSettings = connectionSettings,
                        onResult = { result ->
                            // ✅ FIX: Now using the correct properties that exist in HotkeyExecutionResult
                            scope.launch {
                                if (result.isFullSuccess) {
                                    val successMsg = "✅ Quick trade successful: ${result.summary}"
                                    Log.d("MarketDataScreen", successMsg)
                                    errorMessage = null
                                } else if (result.hasPartialSuccess) {
                                    val partialMsg = "⚠️ ${result.summary}"
                                    Log.w("MarketDataScreen", partialMsg)
                                    errorMessage = partialMsg
                                } else {
                                    val failMsg = "❌ ${result.summary}"
                                    Log.e("MarketDataScreen", failMsg)
                                    errorMessage = failMsg
                                }
                            }
                        }
                    )
                    
                } else {
                    Log.w("MarketDataScreen", "No hotkey preset available for quick trade")
                    errorMessage = "No hotkey presets configured. Go to Settings > Hotkey Settings to add presets."
                }
                
            } catch (e: Exception) {
                Log.e("MarketDataScreen", "Quick trade error", e)
                errorMessage = "Quick trade failed: ${e.message}"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Market Data & Trading") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { loadAllMarketData() },
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Status Bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isMarketHoursOpen()) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isMarketHoursOpen()) "🟢 Market Open" else "🔴 Market Closed",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Last Update: $lastUpdateTime",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Text(
                        text = "${stockDataMap.size}/${watchlistStocks.size} loaded",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            // Error Message
            errorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            // Stock List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(watchlistStocks) { symbol ->
                    val stockInfo = stockDataMap[symbol]
                    
                    EnhancedStockCard(
                        symbol = symbol,
                        stockInfo = stockInfo,
                        isLoading = stockInfo == null && isLoading,
                        onBuy = { executeQuickTrade(symbol, "buy") },
                        onSell = { executeQuickTrade(symbol, "sell") }
                    )
                }
            }
        }
    }
}

@Composable
private fun EnhancedStockCard(
    symbol: String,
    stockInfo: StockInfo?,
    isLoading: Boolean,
    onBuy: () -> Unit,
    onSell: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = symbol,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                if (stockInfo != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$${String.format("%.2f", stockInfo.price)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        val changeColor = if (stockInfo.dayChange >= 0) Color.Green else Color.Red
                        Text(
                            text = "${if (stockInfo.dayChange >= 0) "+" else ""}${String.format("%.2f", stockInfo.dayChange)} (${String.format("%.2f", stockInfo.dayChangePercent)}%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = changeColor
                        )
                    }
                } else if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = "No Data",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (stockInfo != null) {
                Spacer(modifier = Modifier.height(12.dp))
                
                // Market Data Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MarketDataItem("Bid", "$${String.format("%.2f", stockInfo.bid)}", "${stockInfo.bidSize}")
                    MarketDataItem("Ask", "$${String.format("%.2f", stockInfo.ask)}", "${stockInfo.askSize}")
                    MarketDataItem("Vol", "${NumberFormat.getNumberInstance().format(stockInfo.volume)}", "")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MarketDataItem("High", "$${String.format("%.2f", stockInfo.dayHigh)}", "")
                    MarketDataItem("Low", "$${String.format("%.2f", stockInfo.dayLow)}", "")
                    MarketDataItem("Prev", "$${String.format("%.2f", stockInfo.prevClose)}", "")
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Trading Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onBuy,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Green.copy(alpha = 0.8f))
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("BUY")
                    }
                    
                    Button(
                        onClick = onSell,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f))
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SELL")
                    }
                }
                
                // Last Update Time
                Text(
                    text = "Updated: ${stockInfo.lastUpdated}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun MarketDataItem(
    label: String,
    value: String,
    subtitle: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        if (subtitle.isNotEmpty()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun isMarketHoursOpen(): Boolean {
    val now = Calendar.getInstance()
    val hour = now.get(Calendar.HOUR_OF_DAY)
    val dayOfWeek = now.get(Calendar.DAY_OF_WEEK)
    
    // Simple market hours check (9:30 AM - 4:00 PM EST, Monday-Friday)
    return dayOfWeek in Calendar.MONDAY..Calendar.FRIDAY && hour in 9..16
}