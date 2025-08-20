package com.example.ordersgeneratorapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import android.util.Log
import com.example.ordersgeneratorapp.repository.AlpacaRepository
import com.example.ordersgeneratorapp.util.SettingsManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.ZoneId

data class OrderWithAccount(
    val id: String,
    val clientOrderId: String?,
    val createdAt: String,
    val updatedAt: String?,
    val submittedAt: String?,
    val filledAt: String?,
    val expiredAt: String?,
    val canceledAt: String?,
    val failedAt: String?,
    val replacedAt: String?,
    val replacedBy: String?,
    val replaces: String?,
    val assetId: String, // ✅ Make non-null with default value
    val symbol: String,
    val assetClass: String, // ✅ Make non-null with default value
    val notional: String?,
    val qty: String,
    val filledQty: String, // ✅ Make non-null with default value
    val filledAvgPrice: String?,
    val orderClass: String, // ✅ Make non-null with default value
    val orderType: String,
    val side: String,
    val timeInForce: String,
    val limitPrice: String?,
    val stopPrice: String?,
    val status: String,
    val extendedHours: Boolean, // ✅ Make non-null with default value
    val legs: List<String>,
    val trailPercent: String?,
    val trailPrice: String?,
    val hwm: String?,
    val commission: String?,
    val accountName: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHistoryScreen(
    onBackClick: () -> Unit,
    alpacaRepository: AlpacaRepository
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val scope = rememberCoroutineScope()
    
    var ordersWithAccount by remember { mutableStateOf<List<OrderWithAccount>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // ✅ ADD: Filter state for tabs
    var selectedFilter by remember { mutableStateOf("all") }
    val filterOptions = listOf(
        "all" to "All Orders",
        "open" to "Open", 
        "filled" to "Filled",
        "canceled" to "Canceled"
    )
    
    val connectionSettings = remember { settingsManager.getConnectionSettings() }
    
    fun refreshOrdersFromAlpaca() {
        scope.launch {
            isLoading = true
            errorMessage = null
            
            try {
                val allOrders = mutableListOf<OrderWithAccount>()
                val processedOrderIds = mutableSetOf<String>() // ✅ PREVENT DUPLICATES
                
                connectionSettings.brokerAccounts
                    .filter { it.brokerType == "Alpaca" && it.isEnabled }
                    .forEach { account ->
                        
                        Log.d("OrderHistoryScreen", "FETCHING_ORDERS_FROM_ALPACA account=${account.accountName}")
                        
                        alpacaRepository.configureFromBrokerAccount(account)
                        
                        // ✅ FIX: Try multiple queries to get ALL orders including filled ones
                        val queries = listOf(
                            null,        // All orders
                            "open",      // Open orders only
                            "closed",    // Closed orders only
                            "all"        // Explicitly all
                        )
                        
                        var foundOrders = false
                        
                        for (status in queries) {
                            Log.d("OrderHistoryScreen", "CALLING_ALPACA_API account=${account.accountName} status=$status")
                            
                            val result = alpacaRepository.getOrders(
                                status = status,
                                limit = 100,
                                direction = "desc"
                            )
                            
                            if (result.isSuccess) {
                                val orders = result.getOrNull() ?: emptyList()
                                Log.d("OrderHistoryScreen", "FETCHED_ORDERS account=${account.accountName} status=$status count=${orders.size}")
                                
                                if (orders.isNotEmpty()) {
                                    foundOrders = true
                                    
                                    orders.forEach { order ->
                                        val uniqueKey = "${account.id}_${order.id}" // ✅ ACCOUNT+ORDER ID FOR UNIQUENESS
                                        
                                        if (!processedOrderIds.contains(uniqueKey)) {
                                            processedOrderIds.add(uniqueKey)
                                            
                                            Log.d("OrderHistoryScreen", "ADDING_UNIQUE_ORDER: ${order.id} account=${account.accountName}")
                                            
                                            allOrders.add(
                                                OrderWithAccount(
                                                    id = order.id,
                                                    clientOrderId = order.clientOrderId,
                                                    createdAt = order.createdAt,
                                                    updatedAt = order.updatedAt,
                                                    submittedAt = order.submittedAt,
                                                    filledAt = order.filledAt,
                                                    expiredAt = order.expiredAt,
                                                    canceledAt = order.canceledAt,
                                                    failedAt = order.failedAt,
                                                    replacedAt = order.replacedAt,
                                                    replacedBy = order.replacedBy,
                                                    replaces = order.replaces,
                                                    // ✅ FIX: Handle null values from Alpaca API
                                                    assetId = order.assetId ?: "", // Provide default empty string
                                                    symbol = order.symbol,
                                                    assetClass = order.assetClass ?: "us_equity", // Default asset class
                                                    notional = order.notional,
                                                    qty = order.qty,
                                                    filledQty = order.filledQty ?: "0", // Default to "0"
                                                    filledAvgPrice = order.filledAvgPrice,
                                                    orderClass = order.orderClass ?: "simple", // Default order class
                                                    orderType = order.orderType,
                                                    side = order.side,
                                                    timeInForce = order.timeInForce,
                                                    limitPrice = order.limitPrice,
                                                    stopPrice = order.stopPrice,
                                                    status = order.status,
                                                    extendedHours = order.extendedHours ?: false, // Default to false
                                                    legs = emptyList(), // Simple fix - legs aren't displayed anyway
                                                    trailPercent = order.trailPercent,
                                                    trailPrice = order.trailPrice,
                                                    hwm = order.hwm,
                                                    commission = order.commission,
                                                    accountName = account.accountName
                                                )
                                            )
                                        } else {
                                            Log.d("OrderHistoryScreen", "SKIPPING_DUPLICATE_ORDER: ${order.id} account=${account.accountName}")
                                        }
                                    }
                                    break // Found orders with this status, no need to try others
                                }
                            }
                        }
                        
                        // ✅ If still no orders found, check positions as fallback
                        if (!foundOrders) {
                            Log.d("OrderHistoryScreen", "NO_ORDERS_FOUND - checking positions for account=${account.accountName}")
                            try {
                                val positionsResult = alpacaRepository.getPositions()
                                if (positionsResult.isSuccess) {
                                    val positions = positionsResult.getOrNull() ?: emptyList()
                                    Log.d("OrderHistoryScreen", "POSITIONS_FOUND account=${account.accountName} count=${positions.size}")
                                    
                                    positions.forEach { position ->
                                        if (position.qty != "0") {
                                            val positionOrderId = "FILLED_${account.id}_${position.symbol}_${System.currentTimeMillis()}"
                                            
                                            allOrders.add(
                                                OrderWithAccount(
                                                    id = positionOrderId,
                                                    clientOrderId = "Inferred from Position",
                                                    createdAt = java.time.Instant.now().toString(),
                                                    updatedAt = null,
                                                    submittedAt = null,
                                                    filledAt = java.time.Instant.now().toString(),
                                                    expiredAt = null,
                                                    canceledAt = null,
                                                    failedAt = null,
                                                    replacedAt = null,
                                                    replacedBy = null,
                                                    replaces = null,
                                                    assetId = "",
                                                    symbol = position.symbol,
                                                    assetClass = "us_equity",
                                                    notional = null,
                                                    qty = position.qty,
                                                    filledQty = position.qty,
                                                    filledAvgPrice = position.avgEntryPrice,
                                                    orderClass = "simple",
                                                    orderType = "market",
                                                    side = if (position.qty.toDoubleOrNull()?.let { it > 0 } == true) "buy" else "sell",
                                                    timeInForce = "day",
                                                    limitPrice = null,
                                                    stopPrice = null,
                                                    status = "filled",
                                                    extendedHours = false,
                                                    legs = emptyList(),
                                                    trailPercent = null,
                                                    trailPrice = null,
                                                    hwm = null,
                                                    commission = null,
                                                    accountName = account.accountName
                                                )
                                            )
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("OrderHistoryScreen", "Error getting positions for ${account.accountName}", e)
                            }
                        }
                    }
                
                // ✅ Sort by creation date (newest first)
                ordersWithAccount = allOrders.sortedByDescending { it.createdAt }
                
                Log.d("OrderHistoryScreen", "TOTAL_UNIQUE_ORDERS_LOADED: ${ordersWithAccount.size}")
                ordersWithAccount.forEach { order ->
                    Log.d("OrderHistoryScreen", "  - Order ${order.id} | ${order.symbol} | ${order.side} | ${order.status} | ${order.accountName}")
                }
                
            } catch (e: Exception) {
                Log.e("OrderHistoryScreen", "REFRESH_ORDERS_EXCEPTION", e)
                errorMessage = "Failed to load orders: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
    
    // ✅ Filter orders based on selected tab
    val filteredOrders = remember(ordersWithAccount, selectedFilter) {
        when (selectedFilter) {
            "open" -> ordersWithAccount.filter { 
                it.status.lowercase() in listOf("new", "accepted", "pending_new", "partially_filled") 
            }
            "filled" -> ordersWithAccount.filter { 
                it.status.lowercase() == "filled" 
            }
            "canceled" -> ordersWithAccount.filter { 
                it.status.lowercase() in listOf("canceled", "rejected", "expired") 
            }
            else -> ordersWithAccount
        }
    }
    
    LaunchedEffect(Unit) {
        refreshOrdersFromAlpaca()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Order History (Live from Alpaca)") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { refreshOrdersFromAlpaca() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
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
            // ✅ ADD: Filter Tabs
            ScrollableTabRow(
                selectedTabIndex = filterOptions.indexOfFirst { it.first == selectedFilter },
                modifier = Modifier.fillMaxWidth()
            ) {
                filterOptions.forEachIndexed { index, (key, label) ->
                    val count = when (key) {
                        "open" -> ordersWithAccount.count { 
                            it.status.lowercase() in listOf("new", "accepted", "pending_new", "partially_filled") 
                        }
                        "filled" -> ordersWithAccount.count { it.status.lowercase() == "filled" }
                        "canceled" -> ordersWithAccount.count { 
                            it.status.lowercase() in listOf("canceled", "rejected", "expired") 
                        }
                        else -> ordersWithAccount.size
                    }
                    
                    Tab(
                        selected = selectedFilter == key,
                        onClick = { selectedFilter = key },
                        text = { 
                            Text("$label ($count)") 
                        }
                    )
                }
            }
            
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            errorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            if (filteredOrders.isEmpty() && !isLoading) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No ${if (selectedFilter == "all") "" else selectedFilter} orders found",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Orders from your Alpaca accounts will appear here",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredOrders) { order ->
                    OrderCard(
                        order = order,
                        onCancelOrder = { orderId ->
                            scope.launch {
                                val result = alpacaRepository.cancelOrder(orderId)
                                if (result.isSuccess) {
                                    refreshOrdersFromAlpaca()
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderCard(
    order: OrderWithAccount,
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
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Order #${order.id}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${order.side.uppercase()} ${order.qty} shares • ${order.accountName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (!order.clientOrderId.isNullOrBlank() && order.clientOrderId != order.id) {
                        Text(
                            text = "Client ID: ${order.clientOrderId}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = getStatusColor(order.status)
                    )
                ) {
                    Text(
                        text = order.status.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OrderDetailRow("Account", order.accountName)
                OrderDetailRow("Symbol", order.symbol)
                OrderDetailRow("Type", order.orderType.uppercase())
                OrderDetailRow("Time in Force", order.timeInForce.uppercase())
                order.limitPrice?.let { if (it.isNotBlank() && it != "null") OrderDetailRow("Limit Price", "$$it") }
                order.stopPrice?.let { if (it.isNotBlank() && it != "null") OrderDetailRow("Stop Price", "$$it") }
                order.filledQty?.let { if (it.isNotBlank() && it != "0") OrderDetailRow("Filled", "$it shares") }
                formatIsoTimestamp(order.createdAt)?.let { OrderDetailRow("Created", it) }
                formatIsoTimestamp(order.filledAt)?.let { OrderDetailRow("Filled", it) }
                
                OrderDetailRow("Alpaca Order ID", order.id)
                if (!order.clientOrderId.isNullOrBlank()) {
                    OrderDetailRow("Client Order ID", order.clientOrderId)
                }
            }

            if (order.status.lowercase() in listOf("new","accepted","pending_new","partially_filled")) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { onCancelOrder(order.id) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Cancel, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cancel Order")
                }
            }
        }
    }
}

@Composable
private fun OrderDetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun getStatusColor(status: String): Color {
    return when (status.lowercase()) {
        "new", "accepted", "pending_new" -> Color(0xFF2196F3)
        "partially_filled" -> Color(0xFFFF9800)
        "filled" -> Color(0xFF4CAF50)
        "canceled", "rejected" -> Color(0xFFE53E3E)
        else -> Color.Gray
    }
}

private fun formatIsoTimestamp(iso: String?): String? {
    if (iso.isNullOrBlank() || iso == "null") return null
    return try {
        val odt = OffsetDateTime.parse(iso)
        odt.atZoneSameInstant(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    } catch (e: Exception) {
        iso
    }
}