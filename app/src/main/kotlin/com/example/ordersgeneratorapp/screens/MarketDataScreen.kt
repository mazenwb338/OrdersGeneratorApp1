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
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import java.text.NumberFormat
import kotlin.math.abs

// Enhanced stock data model
data class StockInfo(
    val symbol: String,
    val name: String,
    val price: Double = 0.0,
    val change: Double = 0.0,
    val changePercent: Double = 0.0,
    val volume: Long = 0L,
    val high: Double = 0.0,
    val low: Double = 0.0,
    val open: Double = 0.0,
    val previousClose: Double = 0.0,
    val marketCap: String = "N/A",
    val sector: String = "",
    val lastUpdated: String = "",
    val bid: Double = 0.0,
    val ask: Double = 0.0,
    val bidSize: Long = 0L,
    val askSize: Long = 0L
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
    val hotkeyProcessor = remember { HotkeyOrderProcessor(alpacaRepository) }
    
    // State management
    var stockData by remember { mutableStateOf<Map<String, StockInfo>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var lastRefreshTime by remember { mutableStateOf<String?>(null) }
    var selectedSymbol by remember { mutableStateOf<String?>(null) }
    
    // Enhanced stock watchlist with major market symbols
    val watchlistStocks = listOf(
        "AAPL" to "Apple Inc.",
        "MSFT" to "Microsoft Corporation", 
        "GOOGL" to "Alphabet Inc.",
        "AMZN" to "Amazon.com Inc.",
        "TSLA" to "Tesla Inc.",
        "NVDA" to "NVIDIA Corporation",
        "META" to "Meta Platforms Inc.",
        "NFLX" to "Netflix Inc.",
        "AMD" to "Advanced Micro Devices",
        "PYPL" to "PayPal Holdings Inc.",
        "DIS" to "The Walt Disney Company",
        "BABA" to "Alibaba Group",
        "CRM" to "Salesforce Inc.",
        "UBER" to "Uber Technologies",
        "SNAP" to "Snap Inc.",
        "SPY" to "SPDR S&P 500 ETF",
        "QQQ" to "Invesco QQQ Trust",
        "IWM" to "iShares Russell 2000 ETF",
        "VTI" to "Vanguard Total Stock Market",
        "GLD" to "SPDR Gold Shares"
    )
    
    val connectionSettings = remember { settingsManager.getConnectionSettings() }
    val hotkeySettings = remember { settingsManager.getAppSettings().hotkeySettings }
    
    // Function to refresh market data
    fun refreshMarketData() {
        scope.launch {
            isLoading = true
            errorMessage = null
            
            try {
                val updatedStocks = mutableMapOf<String, StockInfo>()
                val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                
                Log.d("MarketDataScreen", "REFRESHING_MARKET_DATA symbols=${watchlistStocks.size}")
                
                watchlistStocks.forEach { (symbol, name) ->
                    try {
                        Log.d("MarketDataScreen", "FETCHING_DATA_FOR symbol=$symbol")
                        
                        // Get latest quote
                        val quoteResult = alpacaRepository.getLatestQuote(symbol)
                        var stockInfo = StockInfo(symbol = symbol, name = name)
                        
                        if (quoteResult.isSuccess) {
                            val quote = quoteResult.getOrNull()
                            if (quote != null) {
                                stockInfo = stockInfo.copy(
                                    bid = quote.bidPrice.toDoubleOrNull() ?: 0.0,
                                    ask = quote.askPrice.toDoubleOrNull() ?: 0.0,
                                    bidSize = quote.bidSize.toLongOrNull() ?: 0L,
                                    askSize = quote.askSize.toLongOrNull() ?: 0L,
                                    price = (quote.bidPrice.toDoubleOrNull() ?: 0.0 + quote.askPrice.toDoubleOrNull() ?: 0.0) / 2,
                                    lastUpdated = timestamp
                                )
                                
                                Log.d("MarketDataScreen", "QUOTE_SUCCESS symbol=$symbol bid=${quote.bidPrice} ask=${quote.askPrice}")
                            }
                        } else {
                            Log.w("MarketDataScreen", "QUOTE_FAILED symbol=$symbol error=${quoteResult.exceptionOrNull()?.message}")
                        }
                        
                        // Get latest trade for more price info
                        val marketDataResult = alpacaRepository.getMarketData(symbol)
                        if (marketDataResult.isSuccess) {
                            val marketData = marketDataResult.getOrNull()
                            marketData?.trade?.let { trade ->
                                stockInfo = stockInfo.copy(
                                    price = trade.price.toDoubleOrNull() ?: 0.0,
                                    volume = trade.size.toLongOrNull() ?: 0L,
                                    lastUpdated = timestamp
                                )
                                
                                Log.d("MarketDataScreen", "TRADE_SUCCESS symbol=$symbol price=${trade.price} volume=${trade.size}")
                            }
                        }
                        
                        // Get daily bars for OHLC data
                        val barsResult = alpacaRepository.getHistoricalBars(
                            symbol = symbol,
                            timeframe = "1Day",
                            start = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                            limit = 2
                        )
                        
                        if (barsResult.isSuccess) {
                            val bars = barsResult.getOrNull()?.bars?.get(symbol)
                            if (!bars.isNullOrEmpty()) {
                                val latestBar = bars.first()
                                val previousBar = if (bars.size > 1) bars[1] else null
                                
                                val change = if (previousBar != null) {
                                    latestBar.close - previousBar.close
                                } else 0.0
                                
                                val changePercent = if (previousBar != null && previousBar.close > 0) {
                                    (change / previousBar.close) * 100
                                } else 0.0
                                
                                stockInfo = stockInfo.copy(
                                    open = latestBar.open,
                                    high = latestBar.high,
                                    low = latestBar.low,
                                    price = if (stockInfo.price == 0.0) latestBar.close else stockInfo.price,
                                    previousClose = previousBar?.close ?: latestBar.close,
                                    change = change,
                                    changePercent = changePercent,
                                    volume = if (stockInfo.volume == 0L) latestBar.volume else stockInfo.volume,
                                    lastUpdated = timestamp
                                )
                                
                                Log.d("MarketDataScreen", "BARS_SUCCESS symbol=$symbol price=${latestBar.close} change=$change%")
                            }
                        }
                        
                        // Add sector information (static mapping for demo)
                        stockInfo = stockInfo.copy(
                            sector = when (symbol) {
                                "AAPL" -> "Technology"
                                "MSFT" -> "Technology"
                                "GOOGL" -> "Technology"
                                "AMZN" -> "Consumer Discretionary"
                                "TSLA" -> "Consumer Discretionary"
                                "NVDA" -> "Technology"
                                "META" -> "Technology"
                                "NFLX" -> "Communication Services"
                                "AMD" -> "Technology"
                                "PYPL" -> "Financial Services"
                                "DIS" -> "Communication Services"
                                "BABA" -> "Consumer Discretionary"
                                "CRM" -> "Technology"
                                "UBER" -> "Technology"
                                "SNAP" -> "Communication Services"
                                "SPY", "QQQ", "IWM", "VTI" -> "ETF"
                                "GLD" -> "Commodities"
                                else -> "Other"
                            },
                            marketCap = when (symbol) {
                                "AAPL" -> "3.0T"
                                "MSFT" -> "2.8T"
                                "GOOGL" -> "1.7T"
                                "AMZN" -> "1.5T"
                                "TSLA" -> "800B"
                                "NVDA" -> "1.8T"
                                "META" -> "800B"
                                else -> "N/A"
                            }
                        )
                        
                        updatedStocks[symbol] = stockInfo
                        
                    } catch (e: Exception) {
                        Log.e("MarketDataScreen", "ERROR_FETCHING_DATA symbol=$symbol", e)
                        // Add error stock info
                        updatedStocks[symbol] = StockInfo(
                            symbol = symbol,
                            name = name,
                            lastUpdated = timestamp
                        )
                    }
                    
                    // Small delay to avoid rate limiting
                    delay(100)
                }
                
                stockData = updatedStocks
                lastRefreshTime = timestamp
                
                Log.d("MarketDataScreen", "REFRESH_COMPLETE stocks=${stockData.size} timestamp=$timestamp")
                
            } catch (e: Exception) {
                Log.e("MarketDataScreen", "REFRESH_EXCEPTION", e)
                errorMessage = "Failed to refresh market data: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
    
    // Auto-refresh every 30 seconds
    LaunchedEffect(Unit) {
        refreshMarketData()
        while (true) {
            delay(30_000) // 30 seconds
            if (!isLoading) {
                refreshMarketData()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Market Data")
                        lastRefreshTime?.let {
                            Text(
                                text = "Last updated: $it",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { refreshMarketData() },
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
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
            // Market overview card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "📈 Market Overview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MarketStatCard("Symbols", "${stockData.size}")
                        MarketStatCard("Updated", lastRefreshTime ?: "Never")
                        MarketStatCard("Status", if (isLoading) "Loading..." else "Live")
                    }
                }
            }
            
            // Error message
            errorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Stock list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(stockData.values.sortedBy { it.symbol }) { stock ->
                    StockCard(
                        stock = stock,
                        isSelected = selectedSymbol == stock.symbol,
                        onClick = { 
                            selectedSymbol = if (selectedSymbol == stock.symbol) null else stock.symbol
                        },
                        onBuy = {
                            scope.launch {
                                // Execute buy order using first available preset
                                val preset = hotkeySettings.presets.firstOrNull()
                                if (preset != null) {
                                    val updatedPreset = preset.copy(symbol = stock.symbol)
                                    val result = hotkeyProcessor.executeHotkeyOrder(updatedPreset, "buy")
                                    Log.d("MarketDataScreen", "BUY_ORDER_RESULT: $result")
                                }
                            }
                        },
                        onSell = {
                            scope.launch {
                                // Execute sell order using first available preset
                                val preset = hotkeySettings.presets.firstOrNull()
                                if (preset != null) {
                                    val updatedPreset = preset.copy(symbol = stock.symbol)
                                    val result = hotkeyProcessor.executeHotkeyOrder(updatedPreset, "sell")
                                    Log.d("MarketDataScreen", "SELL_ORDER_RESULT: $result")
                                }
                            }
                        }
                    )
                }
                
                // Add spacing at bottom
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun MarketStatCard(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StockCard(
    stock: StockInfo,
    isSelected: Boolean,
    onClick: () -> Unit,
    onBuy: () -> Unit,
    onSell: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.secondaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stock.symbol,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        
                        if (stock.sector.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                                )
                            ) {
                                Text(
                                    text = stock.sector,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    
                    Text(
                        text = stock.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = if (stock.price > 0) "$${String.format("%.2f", stock.price)}" else "N/A",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (stock.change != 0.0) {
                        val changeColor = if (stock.change > 0) Color(0xFF4CAF50) else Color(0xFFE53E3E)
                        val changeText = "${if (stock.change > 0) "+" else ""}${String.format("%.2f", stock.change)} (${String.format("%.2f", stock.changePercent)}%)"
                        
                        Text(
                            text = changeText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = changeColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            if (isSelected) {
                Spacer(modifier = Modifier.height(12.dp))
                
                // Detailed information
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (stock.bid > 0 && stock.ask > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Bid: $${String.format("%.2f")} (${stock.bidSize})", style = MaterialTheme.typography.bodySmall)
                            Text("Ask: $${String.format("%.2f", stock.ask)} (${stock.askSize})", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (stock.open > 0) Text("Open: $${String.format("%.2f", stock.open)}", style = MaterialTheme.typography.bodySmall)
                        if (stock.volume > 0) Text("Volume: ${NumberFormat.getInstance().format(stock.volume)}", style = MaterialTheme.typography.bodySmall)
                    }
                    
                    if (stock.high > 0 && stock.low > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("High: $${String.format("%.2f", stock.high)}", style = MaterialTheme.typography.bodySmall)
                            Text("Low: $${String.format("%.2f", stock.low)}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    
                    if (stock.marketCap != "N/A") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Market Cap: ${stock.marketCap}", style = MaterialTheme.typography.bodySmall)
                            if (stock.lastUpdated.isNotEmpty()) {
                                Text("Updated: ${stock.lastUpdated}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onBuy,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("BUY")
                    }
                    
                    Button(
                        onClick = onSell,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE53E3E)
                        )
                    ) {
                        Icon(Icons.Default.TrendingDown, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SELL")
                    }
                }
            }
        }
    }
}