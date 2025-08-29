package com.example.ordersgeneratorapp.screens

import android.util.Log
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ordersgeneratorapp.repository.AlpacaRepository
import com.example.ordersgeneratorapp.hotkey.*
import com.example.ordersgeneratorapp.util.SettingsManager
import com.example.ordersgeneratorapp.ui.theme.*
import com.example.ordersgeneratorapp.data.ConnectionSettings
import com.example.ordersgeneratorapp.data.AppSettings
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.GlobalScope
import java.text.SimpleDateFormat
import java.util.*
import java.text.NumberFormat
import java.util.Calendar

// Enhanced stock data class
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

// Market hours function
fun isMarketHoursOpen(): Boolean {
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    
    return dayOfWeek in Calendar.MONDAY..Calendar.FRIDAY && hour in 9..15
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketDataScreen(
    onBackClick: () -> Unit,
    alpacaRepository: AlpacaRepository,
    onNavigateToSymbolDetail: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val scope = rememberCoroutineScope()
    
    val watchlistStocks = listOf(
        "TSLA", "AAPL", "GOOGL", "MSFT", "AMZN",
        "NVDA", "META", "NFLX", "AMD", "UBER"
    )
    
    var stockDataMap by remember { mutableStateOf(mapOf<String, StockInfo>()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var lastUpdateTime by remember { mutableStateOf("") }
    
    // ⭐ Fix settings loading - collect from Flow
    var connectionSettings by remember { mutableStateOf(ConnectionSettings()) }
    var appSettings by remember { mutableStateOf(AppSettings()) }
    
    // Collect settings from Flows
    LaunchedEffect(Unit) {
        launch {
            settingsManager.connectionSettings.collect {
                connectionSettings = it
            }
        }
    }
    
    LaunchedEffect(Unit) {
        launch {
            settingsManager.appSettings.collect {
                appSettings = it
            }
        }
    }
    
    // ⭐ Now hotkeySettings is properly accessible
    val hotkeySettings = appSettings.hotkeySettings
    
    // Initialize hotkey components  
    val hotkeyProcessor = remember { HotkeyOrderProcessor(alpacaRepository) }
    val hotkeyManager = remember { HotkeyManager(hotkeyProcessor) }
    
    fun loadMarketDataForSymbol(symbol: String) {
        scope.launch {
            try {
                Log.d("MarketDataScreen", "Loading REAL data for $symbol")
                val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                
                // ⭐ ONLY use real Alpaca data - no fallbacks
                val quoteResult = alpacaRepository.getLatestQuote(symbol)
                
                if (quoteResult.isSuccess) {
                    val marketData = quoteResult.getOrNull()
                    if (marketData != null) {
                        // ⭐ Extract REAL data from Alpaca response
                        val currentPrice = marketData.trade?.price?.toDoubleOrNull() ?: 0.0
                        val bidPrice = marketData.quote?.bidPrice?.toDoubleOrNull() ?: 0.0
                        val askPrice = marketData.quote?.askPrice?.toDoubleOrNull() ?: 0.0
                        val bidSize = marketData.quote?.bidSize?.toLongOrNull() ?: 0L
                        val askSize = marketData.quote?.askSize?.toLongOrNull() ?: 0L
                        
                        // Only create StockInfo if we have valid price data
                        if (currentPrice > 0.0) {
                            val stockInfo = StockInfo(
                                symbol = symbol,
                                price = currentPrice,
                                bid = bidPrice,
                                ask = askPrice,
                                bidSize = bidSize,
                                askSize = askSize,
                                volume = 0L, // We'll get this from separate API call if needed
                                dayChange = 0.0, // Calculate from previous close when available
                                dayChangePercent = 0.0,
                                dayHigh = 0.0,
                                dayLow = 0.0,
                                prevClose = 0.0,
                                lastUpdated = timestamp,
                                isMarketOpen = isMarketHoursOpen()
                            )
                            
                            stockDataMap = stockDataMap + (symbol to stockInfo)
                            Log.d("MarketDataScreen", "✅ REAL DATA: $symbol price=$currentPrice bid=$bidPrice ask=$askPrice")
                        } else {
                            Log.w("MarketDataScreen", "⚠️ INVALID DATA: $symbol price=$currentPrice - not adding to list")
                        }
                    } else {
                        Log.e("MarketDataScreen", "❌ NO DATA: $symbol - marketData is null")
                    }
                } else {
                    Log.e("MarketDataScreen", "❌ API FAILED: $symbol - ${quoteResult.exceptionOrNull()?.message}")
                }
                
            } catch (e: Exception) {
                Log.e("MarketDataScreen", "❌ EXCEPTION: $symbol - ${e.message}", e)
            }
        }
    }
    
    fun loadAllMarketData() {
        isLoading = true
        errorMessage = null
        
        scope.launch {
            try {
                Log.d("MarketDataScreen", "🔄 Loading REAL market data for ${watchlistStocks.size} symbols")
                
                watchlistStocks.forEach { symbol ->
                    loadMarketDataForSymbol(symbol)
                    delay(200) // Longer delay to respect rate limits
                }
                
                lastUpdateTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                
            } catch (e: Exception) {
                Log.e("MarketDataScreen", "❌ Error loading market data", e)
                errorMessage = "Failed to load market data: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
    
    // Auto-refresh every 60 seconds (longer to avoid rate limits)
    LaunchedEffect(Unit) {
        loadAllMarketData()
        while (true) {
            delay(60000) // 60 seconds instead of 30
            if (!isLoading) {
                loadAllMarketData()
            }
        }
    }
    
    fun executeQuickTrade(symbol: String, side: String) {
        scope.launch {
            try {
                Log.d("MarketDataScreen", "Executing quick trade: $side $symbol")
                
                val presets = hotkeySettings.presets
                if (presets.isNotEmpty()) {
                    val preset = presets.first()
                    
                    // ⭐ Fixed method call - connectionSettings is now a value, not Flow
                    hotkeyManager.executeHotkey(
                        preset = preset,
                        side = side,
                        connectionSettings = connectionSettings, // ⭐ Now properly typed
                        onResult = { result ->
                            scope.launch {
                                if (result.isFullSuccess) {
                                    Log.d("MarketDataScreen", "Quick trade successful: ${result.summary}")
                                    errorMessage = null
                                } else {
                                    Log.e("MarketDataScreen", "Quick trade failed: ${result.summary}")
                                    errorMessage = result.summary
                                }
                            }
                        }
                    )
                    
                } else {
                    Log.w("MarketDataScreen", "No hotkey preset available")
                    errorMessage = "No hotkey presets configured."
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
            
            // ⭐ Show message when no data is available
            if (!isLoading && stockDataMap.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No market data available",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Check your Alpaca connection and try refreshing",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Stock List - only show stocks with real data
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(watchlistStocks.filter { stockDataMap.containsKey(it) }) { symbol ->
                    val stockInfo = stockDataMap[symbol]
                    
                    if (stockInfo != null) {
                        EnhancedStockCard(
                            symbol = symbol,
                            stockInfo = stockInfo,
                            isLoading = false,
                            onBuy = { executeQuickTrade(symbol, "buy") },
                            onSell = { executeQuickTrade(symbol, "sell") },
                            onNavigateToDetail = { 
                                onNavigateToSymbolDetail(symbol)
                            }
                        )
                    }
                }
                
                // Show loading placeholders for symbols being loaded
                if (isLoading) {
                    items(watchlistStocks.filter { !stockDataMap.containsKey(it) }) { symbol ->
                        EnhancedStockCard(
                            symbol = symbol,
                            stockInfo = null,
                            isLoading = true,
                            onBuy = { },
                            onSell = { },
                            onNavigateToDetail = { }
                        )
                    }
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
    onSell: () -> Unit,
    onNavigateToDetail: (String) -> Unit = {}
) {
    var buyButtonPressed by remember { mutableStateOf(false) }
    var sellButtonPressed by remember { mutableStateOf(false) }
    var showOrderConfirmation by remember { mutableStateOf(false) }
    var lastOrderSide by remember { mutableStateOf("") }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { 
                if (stockInfo != null) {
                    onNavigateToDetail(symbol) 
                }
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
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
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = if (stockInfo != null) {
                        Modifier.clickable { onNavigateToDetail(symbol) }
                    } else Modifier
                )
                
                if (stockInfo != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$${String.format("%.2f", stockInfo.price)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        // Only show change if we have the data
                        if (stockInfo.dayChange != 0.0) {
                            val changeColor = if (stockInfo.dayChange >= 0) BullGreen else BearRed
                            Text(
                                text = "${if (stockInfo.dayChange >= 0) "+" else ""}${String.format("%.2f", stockInfo.dayChange)} (${String.format("%.2f", stockInfo.dayChangePercent)}%)",
                                style = MaterialTheme.typography.bodySmall,
                                color = changeColor
                            )
                        } else {
                            Text(
                                text = "Real-time price",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
                
                // Market Data Grid - only show if we have bid/ask data
                if (stockInfo.bid > 0.0 || stockInfo.ask > 0.0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        if (stockInfo.bid > 0.0) {
                            MarketDataItem("Bid", "$${String.format("%.2f", stockInfo.bid)}", "${stockInfo.bidSize}")
                        }
                        if (stockInfo.ask > 0.0) {
                            MarketDataItem("Ask", "$${String.format("%.2f", stockInfo.ask)}", "${stockInfo.askSize}")
                        }
                        MarketDataItem("Price", "$${String.format("%.2f", stockInfo.price)}", "Live")
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
                // Trading Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            buyButtonPressed = true
                            lastOrderSide = "BUY"
                            onBuy()
                            showOrderConfirmation = true
                            GlobalScope.launch {
                                delay(200)
                                buyButtonPressed = false
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (buyButtonPressed) BullGreen.copy(alpha = 0.9f) else BullGreen
                        )
                    ) {
                        if (buyButtonPressed) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("✓", color = Color.White, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("BUYING...", color = Color.White, fontSize = 10.sp)
                            }
                        } else {
                            Text("BUY", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Button(
                        onClick = {
                            sellButtonPressed = true
                            lastOrderSide = "SELL"
                            onSell()
                            showOrderConfirmation = true
                            GlobalScope.launch {
                                delay(200)
                                sellButtonPressed = false
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (sellButtonPressed) BearRed.copy(alpha = 0.9f) else BearRed
                        )
                    ) {
                        if (sellButtonPressed) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("✓", color = Color.White, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("SELLING...", color = Color.White, fontSize = 10.sp)
                            }
                        } else {
                            Text("SELL", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
    
    // Order Confirmation
    if (showOrderConfirmation) {
        LaunchedEffect(showOrderConfirmation) {
            delay(2000)
            showOrderConfirmation = false
        }
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (lastOrderSide == "BUY") BullGreen else BearRed
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📊", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$lastOrderSide order placed for $symbol",
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun MarketDataItem(label: String, value: String, subtitle: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
        if (subtitle.isNotEmpty()) {
            Text(
                text = subtitle,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}