package com.example.ordersgeneratorapp.screens

import android.util.Log
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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
import com.example.ordersgeneratorapp.repository.CandleBar
import com.example.ordersgeneratorapp.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Real Quote data class - no dummy data
data class Quote(
    val symbol: String,
    val lastPrice: Double,
    val change: Double,
    val changePercent: Double,
    val volume: Long,
    val high: Double,
    val low: Double,
    val open: Double,
    val bid: Double,
    val ask: Double,
    val bidSize: Int,
    val askSize: Int,
    val isLiveData: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SymbolDetailScreen(
    symbol: String,
    alpacaRepository: AlpacaRepository,
    onBack: () -> Unit,
    onOrderHistory: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var selectedTimeframe by remember { mutableStateOf("1D") }
    var showOrderDialog by remember { mutableStateOf(false) }
    var orderDialogSide by remember { mutableStateOf("") }
    var orderDialogQuantity by remember { mutableStateOf("") }
    
    // Button press states for visual feedback
    var buyButtonStates by remember { mutableStateOf(mapOf<String, Boolean>()) }
    var sellButtonStates by remember { mutableStateOf(mapOf<String, Boolean>()) }
    
    // Order confirmation
    var showOrderConfirmation by remember { mutableStateOf(false) }
    var lastOrderInfo by remember { mutableStateOf("") }
    
    // State for real quote data
    var currentQuote by remember { mutableStateOf<Quote?>(null) }
    var isLoadingQuote by remember { mutableStateOf(true) }
    var quoteError by remember { mutableStateOf<String?>(null) }
    
    // Load real quote data function
    fun loadRealQuoteData() {
        scope.launch {
            try {
                isLoadingQuote = true
                quoteError = null
                
                Log.d("SymbolDetailScreen", "Loading REAL data for $symbol")
                
                // Use real Alpaca API call
                val quoteResult = alpacaRepository.getLatestQuote(symbol)
                
                if (quoteResult.isSuccess) {
                    val marketData = quoteResult.getOrNull()
                    if (marketData != null) {
                        // Extract real data from Alpaca response
                        val currentPrice = marketData.trade?.price?.toDoubleOrNull() ?: 0.0
                        val bidPrice = marketData.quote?.bidPrice?.toDoubleOrNull() ?: 0.0
                        val askPrice = marketData.quote?.askPrice?.toDoubleOrNull() ?: 0.0
                        val bidSize = marketData.quote?.bidSize?.toIntOrNull() ?: 0
                        val askSize = marketData.quote?.askSize?.toIntOrNull() ?: 0
                        
                        if (currentPrice > 0.0) {
                            currentQuote = Quote(
                                symbol = symbol,
                                lastPrice = currentPrice,
                                change = 0.0, // Calculate from previous close when available
                                changePercent = 0.0,
                                volume = 0L, // Get from separate API call if needed
                                high = 0.0,
                                low = 0.0,
                                open = 0.0,
                                bid = bidPrice,
                                ask = askPrice,
                                bidSize = bidSize,
                                askSize = askSize,
                                isLiveData = true
                            )
                            
                            Log.d("SymbolDetailScreen", "✅ REAL DATA: $symbol price=$currentPrice")
                        } else {
                            quoteError = "Invalid price data received"
                            Log.w("SymbolDetailScreen", "⚠️ INVALID DATA: $symbol price=$currentPrice")
                        }
                    } else {
                        quoteError = "No market data received"
                        Log.e("SymbolDetailScreen", "❌ NO DATA: $symbol")
                    }
                } else {
                    quoteError = "API call failed: ${quoteResult.exceptionOrNull()?.message}"
                    Log.e("SymbolDetailScreen", "❌ API FAILED: $symbol - ${quoteResult.exceptionOrNull()?.message}")
                }
                
            } catch (e: Exception) {
                quoteError = "Failed to load quote data: ${e.message}"
                Log.e("SymbolDetailScreen", "❌ EXCEPTION: $symbol - ${e.message}", e)
            } finally {
                isLoadingQuote = false
            }
        }
    }
    
    // Load quote data on screen load
    LaunchedEffect(symbol) {
        loadRealQuoteData()
    }
    
    // Function to handle quick order placement
    fun placeQuickOrder(side: String, quantity: String) {
        // Update button state for visual feedback
        if (side == "BUY") {
            buyButtonStates = buyButtonStates + (quantity to true)
        } else {
            sellButtonStates = sellButtonStates + (quantity to true)
        }
        
        // Show confirmation
        val price = currentQuote?.lastPrice ?: 0.0
        lastOrderInfo = "$side $quantity shares of $symbol at $${String.format("%.2f", price)}"
        showOrderConfirmation = true
        
        // Reset button state after animation
        scope.launch {
            delay(300)
            if (side == "BUY") {
                buyButtonStates = buyButtonStates - quantity
            } else {
                sellButtonStates = sellButtonStates - quantity
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar with Live Price
        TopAppBar(
            title = { 
                Column {
                    Text(
                        text = symbol,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (isLoadingQuote) {
                        Text(
                            text = "Loading...",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (currentQuote != null && currentQuote!!.lastPrice > 0.0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$${String.format("%.2f", currentQuote!!.lastPrice)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            if (currentQuote!!.change != 0.0) {
                                Text(
                                    text = "${if (currentQuote!!.change >= 0) "+" else ""}${String.format("%.2f", currentQuote!!.change)} (${String.format("%.2f", currentQuote!!.changePercent)}%)",
                                    fontSize = 12.sp,
                                    color = if (currentQuote!!.change >= 0) BullGreen else BearRed,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "No price data",
                            fontSize = 14.sp,
                            color = BearRed
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.ArrowBack, 
                        "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            actions = {
                // Refresh button
                IconButton(onClick = { loadRealQuoteData() }) {
                    if (isLoadingQuote) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.Refresh,
                            "Refresh",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )
        
        // Content
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            
            // Error Message
            quoteError?.let { error ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "⚠️ Data Issue",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
            
            // Real-time Quote Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Real-time Quote",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        if (currentQuote != null && currentQuote!!.lastPrice > 0.0) {
                            // Current Price & Change
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Current Price",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "$${String.format("%.2f", currentQuote!!.lastPrice)}",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                if (currentQuote!!.change != 0.0) {
                                    Column(
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text(
                                            text = "Day Change",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        val changeColor = if (currentQuote!!.change >= 0) BullGreen else BearRed
                                        Text(
                                            text = "${if (currentQuote!!.change >= 0) "+" else ""}${String.format("%.2f", currentQuote!!.change)}",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = changeColor
                                        )
                                        Text(
                                            text = "(${String.format("%.2f", currentQuote!!.changePercent)}%)",
                                            fontSize = 14.sp,
                                            color = changeColor
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "No real-time data available",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 20.dp)
                            )
                        }
                    }
                }
            }
            
            // Quick Trading Section - only show if we have price data
            if (currentQuote != null && currentQuote!!.lastPrice > 0.0) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Quick Trade @ $${String.format("%.2f", currentQuote!!.lastPrice)}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // BUY Buttons
                            Text("BUY", color = BullGreen, fontWeight = FontWeight.Bold)
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                items(listOf("25", "50", "100", "200", "500")) { qty ->
                                    val isPressed = buyButtonStates[qty] == true
                                    Button(
                                        onClick = { placeQuickOrder("BUY", qty) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isPressed) BullGreen.copy(alpha = 0.8f) else BullGreen
                                        ),
                                        modifier = Modifier.height(40.dp)
                                    ) {
                                        if (isPressed) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("✓", color = Color.White, fontSize = 12.sp)
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(qty, color = Color.White, fontSize = 10.sp)
                                            }
                                        } else {
                                            Text(qty, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // SELL Buttons
                            Text("SELL", color = BearRed, fontWeight = FontWeight.Bold)
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                items(listOf("25", "50", "100", "200", "500")) { qty ->
                                    val isPressed = sellButtonStates[qty] == true
                                    Button(
                                        onClick = { placeQuickOrder("SELL", qty) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isPressed) BearRed.copy(alpha = 0.8f) else BearRed
                                        ),
                                        modifier = Modifier.height(40.dp)
                                    ) {
                                        if (isPressed) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("✓", color = Color.White, fontSize = 12.sp)
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(qty, color = Color.White, fontSize = 10.sp)
                                            }
                                        } else {
                                            Text(qty, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Advanced Order Button
                            OutlinedButton(
                                onClick = { 
                                    orderDialogSide = "CUSTOM"
                                    showOrderDialog = true 
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Advanced Order")
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = { /* existing buy/sell actions */ }) { Text("Trade") }
            OutlinedButton(onClick = onOrderHistory) { Text("Order History") }
        }

        // Candle chart area
        var candles by remember { mutableStateOf<List<CandleBar>>(emptyList()) }
        var candleError by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(symbol) {
            scope.launch {
                val res = alpacaRepository.getRecentBars(symbol, timeframe = "1Min", limit = 120)
                if (res.isSuccess) candles = res.getOrNull()!! else candleError = res.exceptionOrNull()?.message
            }
        }
        CandleChart(candles)
        candleError?.let { Text(it, color = Color.Red) }
    }
    
    // Order Confirmation Toast
    if (showOrderConfirmation) {
        LaunchedEffect(showOrderConfirmation) {
            delay(3000)
            showOrderConfirmation = false
        }
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = BullGreen
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Order Placed Successfully!",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = lastOrderInfo,
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
    
    // Advanced Order Dialog
    if (showOrderDialog) {
        AlertDialog(
            onDismissRequest = { showOrderDialog = false },
            title = { 
                Text("Advanced Order - $symbol", fontWeight = FontWeight.Bold) 
            },
            text = { 
                Column {
                    Text("Advanced order placement with custom quantities, order types, and time-in-force options.")
                    Spacer(modifier = Modifier.height(8.dp))
                    if (currentQuote != null && currentQuote!!.lastPrice > 0.0) {
                        Text("Current Price: $${String.format("%.2f", currentQuote!!.lastPrice)}", 
                             fontSize = 12.sp, 
                             color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (currentQuote!!.bid > 0.0 && currentQuote!!.ask > 0.0) {
                            Text("Bid/Ask: $${String.format("%.2f", currentQuote!!.bid)} / $${String.format("%.2f", currentQuote!!.ask)}", 
                                 fontSize = 12.sp, 
                                 color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showOrderDialog = false }) {
                    Text("Coming Soon", color = BullGreen)
                }
            }
        )
    }
}

@Composable
fun CandleChart(bars: List<CandleBar>) {
    if (bars.isEmpty()) {
        Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        // Minimal textual fallback
        Column(Modifier.height(180.dp).verticalScroll(rememberScrollState())) {
            bars.takeLast(30).forEach {
                Text("${it.timestamp.takeLast(8)} O:${it.open} H:${it.high} L:${it.low} C:${it.close}")
            }
        }
    }
}

@Composable
private fun MarketDataItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}