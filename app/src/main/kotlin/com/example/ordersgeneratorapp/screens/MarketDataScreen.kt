package com.example.ordersgeneratorapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import android.util.Log
import com.example.ordersgeneratorapp.api.*
import com.example.ordersgeneratorapp.data.HotkeyPreset
import com.example.ordersgeneratorapp.repository.AlpacaRepository
import com.example.ordersgeneratorapp.util.SettingsManager
import com.example.ordersgeneratorapp.util.ErrorHandler
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

data class MarketDataDisplay(
    val symbol: String,
    val name: String,
    val price: String,
    val change: String,
    val changePercent: String,
    val bidPrice: String,
    val askPrice: String,
    val volume: String,
    val lastUpdated: String,
    val currentPosition: PositionInfo?,
    val pendingOrders: List<PendingOrderInfo>
) {
    companion object {
        fun fromAlpacaData(
            symbol: String, 
            marketData: MarketData, 
            position: AlpacaPosition?, 
            pendingOrders: List<AlpacaOrder>
        ): MarketDataDisplay {
            val currentPrice = marketData.trade?.price?.toDoubleOrNull() ?: 0.0
            val bidPrice = marketData.quote?.bidPrice?.toDoubleOrNull() ?: 0.0
            val askPrice = marketData.quote?.askPrice?.toDoubleOrNull() ?: 0.0
            val volume = marketData.trade?.size?.toLongOrNull() ?: 0L
            
            val change = 0.0
            val changePercent = if (currentPrice > 0) (change / currentPrice) * 100 else 0.0
            
            val positionInfo = position?.let {
                val qty = it.qty.toDoubleOrNull() ?: 0.0
                val avgPrice = it.avgEntryPrice.toDoubleOrNull() ?: 0.0
                val marketValue = it.marketValue.toDoubleOrNull() ?: 0.0
                val unrealizedPL = it.unrealizedPl.toDoubleOrNull() ?: 0.0
                val unrealizedPLPercent = it.unrealizedPlpc.toDoubleOrNull() ?: 0.0
                
                if (qty != 0.0) {
                    PositionInfo(
                        quantity = qty,
                        avgPrice = avgPrice,
                        marketValue = marketValue,
                        unrealizedPL = unrealizedPL,
                        unrealizedPLPercent = unrealizedPLPercent
                    )
                } else null
            }
            
            val pendingOrdersInfo = pendingOrders.map { order ->
                PendingOrderInfo(
                    id = order.id,
                    side = order.side,
                    quantity = order.qty,
                    orderType = order.orderType,
                    status = order.status,
                    limitPrice = order.limitPrice,
                    stopPrice = order.stopPrice,
                    timeInForce = order.timeInForce,
                    createdAt = order.createdAt
                )
            }
            
            return MarketDataDisplay(
                symbol = symbol,
                name = getCompanyName(symbol),
                price = if (currentPrice > 0) String.format("%.2f", currentPrice) else "N/A",
                change = if (change >= 0) "+${String.format("%.2f", kotlin.math.abs(change))}" else "-${String.format("%.2f", kotlin.math.abs(change))}",
                changePercent = if (changePercent >= 0) "+${String.format("%.2f", kotlin.math.abs(changePercent))}%" else "-${String.format("%.2f", kotlin.math.abs(changePercent))}%",
                bidPrice = if (bidPrice > 0) String.format("%.2f", bidPrice) else "N/A",
                askPrice = if (askPrice > 0) String.format("%.2f", askPrice) else "N/A",
                volume = if (volume > 0) volume.toString() else "N/A",
                lastUpdated = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault()).format(Date()),
                currentPosition = positionInfo,
                pendingOrders = pendingOrdersInfo
            )
        }
        
        private fun getCompanyName(symbol: String): String {
            return when (symbol.uppercase()) {
                "AAPL" -> "Apple Inc."
                "GOOGL" -> "Alphabet Inc."
                "MSFT" -> "Microsoft Corp."
                "TSLA" -> "Tesla Inc."
                "AMZN" -> "Amazon.com Inc."
                "NVDA" -> "NVIDIA Corp."
                else -> symbol
            }
        }
    }
    
    data class PositionInfo(
        val quantity: Double,
        val avgPrice: Double,
        val marketValue: Double,
        val unrealizedPL: Double,
        val unrealizedPLPercent: Double
    ) {
        val isProfit: Boolean get() = unrealizedPL >= 0
        val positionType: String get() = if (quantity > 0) "LONG" else if (quantity < 0) "SHORT" else "FLAT"
        val formattedQuantity: String get() = "${kotlin.math.abs(quantity).toInt()} shares"
    }
    
    data class PendingOrderInfo(
        val id: String,
        val side: String,
        val quantity: String,
        val orderType: String,
        val status: String,
        val limitPrice: String? = null,
        val stopPrice: String? = null,
        val timeInForce: String,
        val createdAt: String
    ) {
        val isBuyOrder: Boolean get() = side.lowercase() == "buy"
        val formattedPrice: String get() = when {
            limitPrice != null && limitPrice.isNotEmpty() -> "@ $${limitPrice}"
            stopPrice != null && stopPrice.isNotEmpty() -> "stop @ $${stopPrice}"
            else -> "market"
        }
        val statusColor: Color get() = when (status.lowercase()) {
            "new", "accepted", "pending_new" -> Color(0xFF2196F3)
            "partially_filled" -> Color(0xFFFF9800)
            "filled" -> Color(0xFF4CAF50)
            "canceled", "rejected" -> Color(0xFFE53E3E)
            else -> Color.Gray
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketDataScreen(
    onBackClick: () -> Unit,
    alpacaRepository: AlpacaRepository
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val appSettings = remember { settingsManager.getAppSettings() }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val refreshTrigger = remember { mutableIntStateOf(0) }

    var marketDataList by remember { mutableStateOf<List<MarketDataDisplay>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchSymbol by remember { mutableStateOf("") }
    var lastRefreshTime by remember { mutableStateOf("") }
    var currentPositions by remember { mutableStateOf<List<AlpacaPosition>>(emptyList()) }
    var pendingOrders by remember { mutableStateOf<List<AlpacaOrder>>(emptyList()) }

    fun loadMarketData() {
        scope.launch {
            isLoading = true
            errorMessage = null
            
            try {
                if (!alpacaRepository.isConfigured()) {
                    errorMessage = "Alpaca API not configured. Please check your connection settings."
                    marketDataList = createFallbackData()
                    return@launch
                }
                
                if (searchSymbol.isNotEmpty()) {
                    Log.d("MarketDataScreen", "Loading market data for: ${searchSymbol.uppercase()}")
                    
                    val result = alpacaRepository.getMarketData(searchSymbol.uppercase())
                    if (result.isSuccess) {
                        val alpacaData = result.getOrNull()
                        if (alpacaData != null) {
                            Log.d("MarketDataScreen", "Market data loaded successfully for ${searchSymbol.uppercase()}")
                            
                            val position = currentPositions.find { it.symbol == searchSymbol.uppercase() }
                            val symbolPendingOrders = pendingOrders.filter { it.symbol == searchSymbol.uppercase() }
                            val newData = MarketDataDisplay.fromAlpacaData(searchSymbol.uppercase(), alpacaData, position, symbolPendingOrders)
                            marketDataList = listOf(newData)
                        } else {
                            Log.w("MarketDataScreen", "Market data is null for ${searchSymbol.uppercase()}")
                            errorMessage = "No market data available for $searchSymbol"
                            marketDataList = createFallbackData()
                        }
                    } else {
                        val error = result.exceptionOrNull()
                        Log.e("MarketDataScreen", "Failed to load market data for ${searchSymbol.uppercase()}: ${error?.message}")
                        errorMessage = "Failed to load data for $searchSymbol: ${error?.message}"
                        marketDataList = createFallbackData()
                    }
                } else {
                    Log.d("MarketDataScreen", "Loading default market data")
                    val defaultSymbols = listOf("AAPL", "GOOGL", "TSLA")
                    val dataList = mutableListOf<MarketDataDisplay>()
                    
                    defaultSymbols.forEach { symbol ->
                        try {
                            val result = alpacaRepository.getMarketData(symbol)
                            if (result.isSuccess) {
                                val alpacaData = result.getOrNull()
                                if (alpacaData != null) {
                                    val position = currentPositions.find { it.symbol == symbol }
                                    val symbolPendingOrders = pendingOrders.filter { it.symbol == symbol }
                                    val marketData = MarketDataDisplay.fromAlpacaData(symbol, alpacaData, position, symbolPendingOrders)
                                    dataList.add(marketData)
                                }
                            } else {
                                Log.w("MarketDataScreen", "Failed to load data for $symbol: ${result.exceptionOrNull()?.message}")
                            }
                        } catch (e: Exception) {
                            Log.e("MarketDataScreen", "Exception loading data for $symbol: ${e.message}", e)
                        }
                    }
                    
                    marketDataList = if (dataList.isNotEmpty()) {
                        dataList
                    } else {
                        Log.w("MarketDataScreen", "No market data loaded, using fallback")
                        createFallbackData()
                    }
                }
                
                lastRefreshTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            } catch (e: Exception) {
                Log.e("MarketDataScreen", "Exception in loadMarketData: ${e.message}", e)
                errorMessage = "Error loading market data: ${e.message}"
                marketDataList = createFallbackData()
            } finally {
                isLoading = false
            }
        }
    }
    
    fun createFallbackData(): List<MarketDataDisplay> {
        return listOf(
            MarketDataDisplay(
                symbol = "AAPL",
                name = "Apple Inc.",
                price = "150.25",
                change = "+2.15",
                changePercent = "+1.45%",
                volume = "45.2M",
                bidPrice = "150.20",
                askPrice = "150.30",
                lastUpdated = "15:30:00",
                currentPosition = null,
                pendingOrders = emptyList()
            ),
            MarketDataDisplay(
                symbol = "GOOGL",
                name = "Alphabet Inc.",
                price = "2,450.80",
                change = "-15.30",
                changePercent = "-0.62%",
                volume = "1.2M",
                bidPrice = "2,450.50",
                askPrice = "2,451.10",
                lastUpdated = "15:30:00",
                currentPosition = null,
                pendingOrders = emptyList()
            )
        )
    }

    fun loadPositions() {
        scope.launch {
            try {
                val result = alpacaRepository.getPositions()
                if (result.isSuccess) {
                    currentPositions = result.getOrNull() ?: emptyList()
                    Log.d("MarketDataScreen", "Loaded ${currentPositions.size} positions")
                } else {
                    Log.e("MarketDataScreen", "Failed to load positions: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e("MarketDataScreen", "Exception loading positions: ${e.message}", e)
            }
        }
    }
    
    fun loadPendingOrders() {
        scope.launch {
            try {
                val result = alpacaRepository.getOrders(status = "open")
                if (result.isSuccess) {
                    pendingOrders = result.getOrNull() ?: emptyList()
                    Log.d("MarketDataScreen", "Loaded ${pendingOrders.size} pending orders")
                } else {
                    Log.e("MarketDataScreen", "Failed to load pending orders: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e("MarketDataScreen", "Exception loading pending orders: ${e.message}", e)
            }
        }
    }

    fun reversePosition(preset: HotkeyPreset) {
        scope.launch {
            try {
                Log.d("MarketDataScreen", "Starting reverse position for ${preset.symbol}")
                
                // First refresh positions to get the latest data
                loadPositions()
                delay(500) // Give it a moment to load
                
                val currentPosition = currentPositions.find { it.symbol == preset.symbol }
                
                if (currentPosition == null) {
                    snackbarHostState.showSnackbar(
                        message = "❌ No existing position found for ${preset.symbol} to reverse",
                        duration = SnackbarDuration.Long
                    )
                    return@launch
                }
                
                val currentQty = currentPosition.qty.toDoubleOrNull() ?: 0.0
                
                if (currentQty == 0.0) {
                    snackbarHostState.showSnackbar(
                        message = "❌ No position to reverse for ${preset.symbol} (quantity is 0)",
                        duration = SnackbarDuration.Long
                    )
                    return@launch
                }
                
                // NEW APPROACH: Close position first, then open opposite position
                // This works better with paper trading buying power limitations
                
                if (currentQty > 0) {
                    // Long position: sell all shares first, then short sell the target amount
                    val sellQty = kotlin.math.abs(currentQty).toInt()
                    val shortQty = preset.quantity.toInt()
                    
                    Log.d("MarketDataScreen", "Reversing LONG position: ${preset.symbol}, step 1: sell $sellQty shares")
                    
                    // Step 1: Close long position
                    val closeResult = alpacaRepository.createOrder(
                        symbol = preset.symbol,
                        quantity = sellQty,
                        side = "sell",
                        orderType = preset.orderType,
                        timeInForce = preset.timeInForce,
                        limitPrice = if (preset.limitPrice.isNotEmpty()) preset.limitPrice else null,
                        stopPrice = if (preset.stopPrice.isNotEmpty()) preset.stopPrice else null
                    )
                    
                    if (closeResult.isSuccess) {
                        delay(1000) // Wait for order to process
                        
                        // Step 2: Open short position
                        val shortResult = alpacaRepository.createOrder(
                            symbol = preset.symbol,
                            quantity = shortQty,
                            side = "sell", // Short sell
                            orderType = preset.orderType,
                            timeInForce = preset.timeInForce,
                            limitPrice = if (preset.limitPrice.isNotEmpty()) preset.limitPrice else null,
                            stopPrice = if (preset.stopPrice.isNotEmpty()) preset.stopPrice else null
                        )
                        
                        if (shortResult.isSuccess) {
                            snackbarHostState.showSnackbar(
                                message = "🔄 Position reversed: ${preset.symbol} LONG to SHORT - Orders placed successfully",
                                duration = SnackbarDuration.Short
                            )
                        } else {
                            snackbarHostState.showSnackbar(
                                message = "⚠️ Position closed but short order failed: ${shortResult.exceptionOrNull()?.message}",
                                duration = SnackbarDuration.Long
                            )
                        }
                    } else {
                        snackbarHostState.showSnackbar(
                            message = "❌ Failed to close long position: ${closeResult.exceptionOrNull()?.message}",
                            duration = SnackbarDuration.Long
                        )
                        return@launch
                    }
                    
                } else {
                    // Short position: buy to cover, then buy to go long
                    val coverQty = kotlin.math.abs(currentQty).toInt()
                    val longQty = preset.quantity.toInt()
                    
                    Log.d("MarketDataScreen", "Reversing SHORT position: ${preset.symbol}, step 1: buy $coverQty to cover")
                    
                    // Step 1: Cover short position
                    val coverResult = alpacaRepository.createOrder(
                        symbol = preset.symbol,
                        quantity = coverQty,
                        side = "buy",
                        orderType = preset.orderType,
                        timeInForce = preset.timeInForce,
                        limitPrice = if (preset.limitPrice.isNotEmpty()) preset.limitPrice else null,
                        stopPrice = if (preset.stopPrice.isNotEmpty()) preset.stopPrice else null
                    )
                    
                    if (coverResult.isSuccess) {
                        delay(1000) // Wait for order to process
                        
                        // Step 2: Open long position
                        val longResult = alpacaRepository.createOrder(
                            symbol = preset.symbol,
                            quantity = longQty,
                            side = "buy",
                            orderType = preset.orderType,
                            timeInForce = preset.timeInForce,
                            limitPrice = if (preset.limitPrice.isNotEmpty()) preset.limitPrice else null,
                            stopPrice = if (preset.stopPrice.isNotEmpty()) preset.stopPrice else null
                        )
                        
                        if (longResult.isSuccess) {
                            snackbarHostState.showSnackbar(
                                message = "🔄 Position reversed: ${preset.symbol} SHORT to LONG - Orders placed successfully",
                                duration = SnackbarDuration.Short
                            )
                        } else {
                            snackbarHostState.showSnackbar(
                                message = "⚠️ Short covered but long order failed: ${longResult.exceptionOrNull()?.message}",
                                duration = SnackbarDuration.Long
                            )
                        }
                    } else {
                        snackbarHostState.showSnackbar(
                            message = "❌ Failed to cover short position: ${coverResult.exceptionOrNull()?.message}",
                            duration = SnackbarDuration.Long
                        )
                        return@launch
                    }
                }
                
                // Refresh data after operations
                loadPositions()
                loadPendingOrders()
                
            } catch (e: Exception) {
                Log.e("MarketDataScreen", "Exception reversing position: ${e.message}", e)
                snackbarHostState.showSnackbar(
                    message = "❌ Error reversing position: ${e.message}",
                    duration = SnackbarDuration.Long
                )
            }
        }
    }

    fun closePosition(preset: HotkeyPreset) {
        scope.launch {
            try {
                Log.d("MarketDataScreen", "Starting close position for ${preset.symbol}")
                
                loadPositions()
                delay(500)
                
                val currentPosition = currentPositions.find { it.symbol == preset.symbol }
                
                if (currentPosition == null) {
                    snackbarHostState.showSnackbar(
                        message = "❌ No existing position found for ${preset.symbol} to close",
                        duration = SnackbarDuration.Long
                    )
                    return@launch
                }
                
                val currentQty = currentPosition.qty.toDoubleOrNull() ?: 0.0
                
                if (currentQty == 0.0) {
                    snackbarHostState.showSnackbar(
                        message = "❌ No position to close for ${preset.symbol}",
                        duration = SnackbarDuration.Long
                    )
                    return@launch
                }
                
                // Simple close: opposite side trade with same quantity
                val closeQty = kotlin.math.abs(currentQty).toInt()
                val closeSide = if (currentQty > 0) "sell" else "buy"
                
                Log.d("MarketDataScreen", "Closing position: ${preset.symbol}, qty: $closeQty, side: $closeSide")
                
                val result = alpacaRepository.createOrder(
                    symbol = preset.symbol,
                    quantity = closeQty,
                    side = closeSide,
                    orderType = preset.orderType,
                    timeInForce = preset.timeInForce,
                    limitPrice = if (preset.limitPrice.isNotEmpty()) preset.limitPrice else null,
                    stopPrice = if (preset.stopPrice.isNotEmpty()) preset.stopPrice else null
                )
                
                if (result.isSuccess) {
                    val order = result.getOrNull()!!
                    snackbarHostState.showSnackbar(
                        message = "✅ Position closed: ${preset.symbol} ($closeQty shares) - Order ID: ${order.id}",
                        duration = SnackbarDuration.Short
                    )
                    loadPositions()
                    loadPendingOrders()
                } else {
                    snackbarHostState.showSnackbar(
                        message = "❌ Failed to close position: ${result.exceptionOrNull()?.message}",
                        duration = SnackbarDuration.Long
                    )
                }
                
            } catch (e: Exception) {
                Log.e("MarketDataScreen", "Exception closing position: ${e.message}", e)
                snackbarHostState.showSnackbar(
                    message = "❌ Error closing position: ${e.message}",
                    duration = SnackbarDuration.Long
                )
            }
        }
    }

    fun onExecuteQuickTrade(preset: HotkeyPreset, side: String) {
        scope.launch {
            try {
                val result = alpacaRepository.createOrder(
                    symbol = preset.symbol,
                    quantity = preset.quantity.toInt(),
                    side = side,
                    orderType = preset.orderType,
                    timeInForce = preset.timeInForce,
                    limitPrice = if (preset.limitPrice.isNotEmpty()) preset.limitPrice else null,
                    stopPrice = if (preset.stopPrice.isNotEmpty()) preset.stopPrice else null
                )
                
                if (result.isSuccess) {
                    val order = result.getOrNull()!!
                    snackbarHostState.showSnackbar(
                        message = "✅ ${side.uppercase()} order placed! ${preset.symbol} x${preset.quantity} - Order ID: ${order.id}",
                        duration = SnackbarDuration.Short
                    )
                    loadPositions()
                    loadPendingOrders()
                } else {
                    val errorMsg = ErrorHandler.handleApiError(
                        result.exceptionOrNull() ?: Exception("Unknown error"),
                        "placing ${side} order"
                    )
                    snackbarHostState.showSnackbar(
                        message = "❌ $errorMsg",
                        duration = SnackbarDuration.Long
                    )
                }
            } catch (e: Exception) {
                val errorMsg = ErrorHandler.handleApiError(e, "execute quick trade")
                snackbarHostState.showSnackbar(
                    message = "❌ $errorMsg",
                    duration = SnackbarDuration.Long
                )
            }
        }
    }

    fun onCancelOrder(orderId: String) {
        scope.launch {
            try {
                val result = alpacaRepository.cancelOrder(orderId)
                if (result.isSuccess) {
                    snackbarHostState.showSnackbar(
                        message = "✅ Order canceled",
                        duration = SnackbarDuration.Short
                    )
                    loadPendingOrders()
                } else {
                    snackbarHostState.showSnackbar(
                        message = "❌ Failed to cancel order: ${result.exceptionOrNull()?.message}",
                        duration = SnackbarDuration.Long
                    )
                }
            } catch (e: Exception) {
                Log.e("MarketDataScreen", "Error canceling order: ${e.message}", e)
            }
        }
    }

    fun onReversePosition(preset: HotkeyPreset) {
        reversePosition(preset)
    }

    LaunchedEffect(refreshTrigger.intValue) {
        try {
            loadMarketData()
        } catch (e: Exception) {
            Log.e("MarketDataScreen", "Error loading market data: ${e.message}", e)
        }
    }

    LaunchedEffect(Unit) {
        loadPositions()
        loadPendingOrders()
        loadMarketData()
        
        while (true) {
            delay(15000)
            if (!isLoading) {
                loadPositions()
                loadPendingOrders()
                refreshTrigger.intValue++
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Market Data") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { refreshTrigger.intValue++ }) {
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = searchSymbol,
                onValueChange = { searchSymbol = it },
                label = { Text("Search Symbol") },
                placeholder = { Text("e.g., AAPL") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(
                        onClick = { 
                            if (searchSymbol.isNotEmpty()) {
                                loadMarketData()
                            }
                        }
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (errorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE53E3E).copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = Color(0xFFE53E3E)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage!!,
                            color = Color(0xFFE53E3E)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(marketDataList) { marketData ->
                    MarketDataCard(
                        marketData = marketData,
                        hotkeys = appSettings.hotkeySettings.hotkeyPresets.filter { it.symbol == marketData.symbol },
                        onExecuteQuickTrade = { preset, side -> onExecuteQuickTrade(preset, side) },
                        onReversePosition = { preset -> onReversePosition(preset) },
                        onCancelOrder = { orderId -> onCancelOrder(orderId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MarketDataCard(
    marketData: MarketDataDisplay,
    hotkeys: List<HotkeyPreset>,
    onExecuteQuickTrade: (HotkeyPreset, String) -> Unit,
    onReversePosition: (HotkeyPreset) -> Unit,
    onCancelOrder: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = marketData.symbol,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = marketData.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$${marketData.price}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${marketData.change} (${marketData.changePercent})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (marketData.change.startsWith("+")) Color(0xFF4CAF50) else Color(0xFFE53E3E)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Bid: $${marketData.bidPrice}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Column {
                    Text(
                        text = "Ask: $${marketData.askPrice}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Column {
                    Text(
                        text = "Vol: ${marketData.volume}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            if (marketData.currentPosition != null) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (marketData.currentPosition.isProfit) 
                            Color(0xFF4CAF50).copy(alpha = 0.1f) 
                        else 
                            Color(0xFFE53E3E).copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${marketData.currentPosition.positionType} ${marketData.currentPosition.formattedQuantity}",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$${String.format("%.2f", marketData.currentPosition.unrealizedPL)}",
                                color = if (marketData.currentPosition.isProfit) Color(0xFF4CAF50) else Color(0xFFE53E3E),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Avg: $${String.format("%.2f", marketData.currentPosition.avgPrice)} • Value: $${String.format("%.2f", marketData.currentPosition.marketValue)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            if (marketData.pendingOrders.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF2196F3).copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Pending Orders (${marketData.pendingOrders.size})",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2196F3)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        marketData.pendingOrders.forEach { order ->
                            PendingOrderItem(
                                order = order,
                                onCancelClick = { onCancelOrder(order.id) }
                            )
                            if (order != marketData.pendingOrders.last()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = Color(0xFF2196F3).copy(alpha = 0.3f))
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
            
            if (hotkeys.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                
                HorizontalDivider()
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "⚡ Quick Trade Hotkeys",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    hotkeys.forEach { hotkey ->
                        val hasPositionForSymbol = marketData.currentPosition != null && 
                                                   marketData.currentPosition.quantity != 0.0
                        
                        EnhancedHotkeyPresetCard(
                            hotkey = hotkey,
                            onBuyClick = { onExecuteQuickTrade(hotkey, "buy") },
                            onSellClick = { onExecuteQuickTrade(hotkey, "sell") },
                            onReverseClick = { onReversePosition(hotkey) },
                            hasPosition = hasPositionForSymbol
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingOrderItem(
    order: MarketDataDisplay.PendingOrderInfo,
    onCancelClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (order.isBuyOrder) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                    contentDescription = null,
                    tint = if (order.isBuyOrder) Color(0xFF4CAF50) else Color(0xFFE53E3E),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${order.side.uppercase()} ${order.quantity} ${order.formattedPrice}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = "${order.orderType} • ${order.timeInForce} • ${order.status.uppercase()}",
                style = MaterialTheme.typography.bodySmall,
                color = order.statusColor
            )
        }
        
        if (order.status.lowercase() in listOf("new", "pending_new", "accepted")) {
            IconButton(
                onClick = onCancelClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Cancel,
                    contentDescription = "Cancel Order",
                    tint = Color(0xFFE53E3E),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun EnhancedHotkeyPresetCard(
    hotkey: HotkeyPreset,
    onBuyClick: () -> Unit = {},
    onSellClick: () -> Unit = {},
    onReverseClick: () -> Unit = {},
    hasPosition: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "${hotkey.symbol} • ${hotkey.quantity} shares",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "${hotkey.orderType} • ${hotkey.timeInForce}${if (hotkey.limitPrice.isNotEmpty()) " • $${hotkey.limitPrice}" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onBuyClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("BUY", color = Color.White, fontWeight = FontWeight.Bold)
                }
                
                Button(
                    onClick = onSellClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53E3E)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("SELL", color = Color.White, fontWeight = FontWeight.Bold)
                }
                
                Button(
                    onClick = onReverseClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasPosition) Color(0xFF9C27B0) else Color(0xFF9C27B0).copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        "REVERSE", 
                        color = Color.White, 
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            if (hasPosition) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "• Position detected",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9C27B0),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun createFallbackData(): List<MarketDataDisplay> {
    return listOf(
        MarketDataDisplay(
            symbol = "AAPL",
            name = "Apple Inc.",
            price = "150.00",
            change = "+2.50",
            changePercent = "+1.69%",
            bidPrice = "149.95",
            askPrice = "150.05",
            volume = "1,234,567",
            lastUpdated = "Market Closed",
            currentPosition = null,
            pendingOrders = emptyList()
        ),
        MarketDataDisplay(
            symbol = "GOOGL",
            name = "Alphabet Inc.",
            price = "2800.00",
            change = "-15.00",
            changePercent = "-0.53%",
            bidPrice = "2799.50",
            askPrice = "2800.50",
            volume = "987,654",
            lastUpdated = "Market Closed",
            currentPosition = null,
            pendingOrders = emptyList()
        ),
        MarketDataDisplay(
            symbol = "TSLA",
            name = "Tesla Inc.",
            price = "250.00",
            change = "+10.00",
            changePercent = "+4.17%",
            bidPrice = "249.80",
            askPrice = "250.20",
            volume = "2,345,678",
            lastUpdated = "Market Closed",
            currentPosition = null,
            pendingOrders = emptyList()
        )
    )
}