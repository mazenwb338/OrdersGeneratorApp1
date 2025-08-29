package com.example.ordersgeneratorapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ordersgeneratorapp.api.CreateOrderRequest
import com.example.ordersgeneratorapp.repository.AlpacaRepository
import com.example.ordersgeneratorapp.util.SettingsManager  // ✅ FIX: Correct import path
import kotlinx.coroutines.launch

data class OrderRequest(
    val symbol: String = "",
    val orderType: String = "Market", // Market, Limit, Stop, Stop Limit
    val side: String = "Buy", // Buy, Sell
    val quantity: String = "",
    val limitPrice: String = "",
    val stopPrice: String = "",
    val timeInForce: String = "DAY", // DAY, GTC, IOC
    val broker: String = "Alpaca" // Alpaca, IBKR
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderPlacementScreen(
    onBackClick: () -> Unit,
    alpacaRepository: AlpacaRepository? = null
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) } // ✅ FIX: Correct usage
    val repository = alpacaRepository ?: remember { AlpacaRepository(settingsManager) } // ✅ FIX: Correct usage

    var orderRequest by remember { mutableStateOf(OrderRequest()) }
    var showConfirmation by remember { mutableStateOf(false) }
    var isPlacingOrder by remember { mutableStateOf(false) }
    var orderResult by remember { mutableStateOf<String?>(null) }
    var orderError by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()

    fun placeOrder() {
        scope.launch {
            isPlacingOrder = true
            orderError = null
            try {
                // Ensure credentials (auto)
                repository.isConfigured()
                val result = repository.createOrder(
                    symbol = orderRequest.symbol,
                    quantity = orderRequest.quantity.toInt(),
                    side = orderRequest.side.lowercase(),
                    orderType = orderRequest.orderType.lowercase(),
                    timeInForce = orderRequest.timeInForce.lowercase(),
                    limitPrice = orderRequest.limitPrice.ifBlank { null },
                    stopPrice = orderRequest.stopPrice.ifBlank { null }
                )
                if (result.isSuccess) {
                    val order = result.getOrNull()!!
                    orderResult = "Order placed: ${order.id}"
                    // Optimistic UI reset
                    orderRequest = OrderRequest()
                } else {
                    orderError = result.exceptionOrNull()?.message ?: "Failed"
                }
            } catch (e: Exception) {
                orderError = e.message
            } finally {
                isPlacingOrder = false
                showConfirmation = false
            }
        }
    }

    if (showConfirmation) {
        OrderConfirmationDialog(
            order = orderRequest,
            onConfirm = { placeOrder() },
            onDismiss = { showConfirmation = false },
            isLoading = isPlacingOrder
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Place Order") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Order Result/Error Messages
            if (orderResult != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = orderResult!!,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
            }
            
            if (orderError != null) {
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
                            text = orderError!!,
                            color = Color(0xFFE53E3E)
                        )
                    }
                }
            }
            
            // Broker Selection
            BrokerSelectionCard(
                selectedBroker = orderRequest.broker,
                onBrokerSelected = { orderRequest = orderRequest.copy(broker = it) }
            )

            // Symbol Input
            OutlinedTextField(
                value = orderRequest.symbol,
                onValueChange = { orderRequest = orderRequest.copy(symbol = it.uppercase()) },
                label = { Text("Symbol") },
                placeholder = { Text("e.g., AAPL") },
                modifier = Modifier.fillMaxWidth()
            )

            // Order Side
            OrderSideSelection(
                selectedSide = orderRequest.side,
                onSideSelected = { orderRequest = orderRequest.copy(side = it) }
            )

            // Order Type
            OrderTypeSelection(
                selectedType = orderRequest.orderType,
                onTypeSelected = { orderRequest = orderRequest.copy(orderType = it) }
            )

            // Quantity
            OutlinedTextField(
                value = orderRequest.quantity,
                onValueChange = { orderRequest = orderRequest.copy(quantity = it) },
                label = { Text("Quantity") },
                placeholder = { Text("Number of shares") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            // Conditional Price Fields
            if (orderRequest.orderType == "Limit" || orderRequest.orderType == "Stop Limit") {
                OutlinedTextField(
                    value = orderRequest.limitPrice,
                    onValueChange = { orderRequest = orderRequest.copy(limitPrice = it) },
                    label = { Text("Limit Price") },
                    placeholder = { Text("Price per share") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (orderRequest.orderType == "Stop" || orderRequest.orderType == "Stop Limit") {
                OutlinedTextField(
                    value = orderRequest.stopPrice,
                    onValueChange = { orderRequest = orderRequest.copy(stopPrice = it) },
                    label = { Text("Stop Price") },
                    placeholder = { Text("Stop trigger price") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Time in Force
            TimeInForceSelection(
                selectedTIF = orderRequest.timeInForce,
                onTIFSelected = { orderRequest = orderRequest.copy(timeInForce = it) }
            )

            // Order Summary
            OrderSummaryCard(orderRequest)

            // Submit Button
            Button(
                onClick = { 
                    orderResult = null
                    orderError = null
                    showConfirmation = true 
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isOrderValid(orderRequest) && !isPlacingOrder
            ) {
                if (isPlacingOrder) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Review Order", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun OrderConfirmationDialog(
    order: OrderRequest,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isLoading: Boolean = false
) {
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("Confirm Order") },
        text = {
            Column {
                Text("Are you sure you want to place this order?")
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "${order.side} ${order.quantity} shares of ${order.symbol}",
                    fontWeight = FontWeight.Bold
                )
                Text("Order Type: ${order.orderType}")
                if (order.limitPrice.isNotEmpty()) {
                    Text("Limit Price: $${order.limitPrice}")
                }
                if (order.stopPrice.isNotEmpty()) {
                    Text("Stop Price: $${order.stopPrice}")
                }
                Text("Time in Force: ${order.timeInForce}")
                Text("Broker: ${order.broker}")
                
                if (isLoading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Placing order...")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isLoading
            ) {
                Text("Place Order")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun BrokerSelectionCard(
    selectedBroker: String,
    onBrokerSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Select Broker",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Alpaca", "IBKR").forEach { broker ->
                    FilterChip(
                        selected = selectedBroker == broker,
                        onClick = { onBrokerSelected(broker) },
                        label = { Text(broker) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderSideSelection(
    selectedSide: String,
    onSideSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Order Side",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Buy", "Sell").forEach { side ->
                    FilterChip(
                        selected = selectedSide == side,
                        onClick = { onSideSelected(side) },
                        label = { Text(side) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderTypeSelection(
    selectedType: String,
    onTypeSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Order Type",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Market", "Limit").forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { onTypeSelected(type) },
                        label = { Text(type) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeInForceSelection(
    selectedTIF: String,
    onTIFSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Time in Force",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("DAY", "GTC", "IOC").forEach { tif ->
                    FilterChip(
                        selected = selectedTIF == tif,
                        onClick = { onTIFSelected(tif) },
                        label = { Text(tif) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderSummaryCard(order: OrderRequest) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Order Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            if (order.symbol.isNotEmpty()) {
                SummaryRow("Symbol", order.symbol)
            }
            if (order.quantity.isNotEmpty()) {
                SummaryRow("Quantity", "${order.quantity} shares")
            }
            SummaryRow("Side", order.side)
            SummaryRow("Type", order.orderType)
            SummaryRow("Broker", order.broker)
            
            if (order.limitPrice.isNotEmpty()) {
                SummaryRow("Limit Price", "$${order.limitPrice}")
            }
            if (order.stopPrice.isNotEmpty()) {
                SummaryRow("Stop Price", "$${order.stopPrice}")
            }
            
            SummaryRow("Time in Force", order.timeInForce)
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun isOrderValid(order: OrderRequest): Boolean {
    return order.symbol.isNotEmpty() &&
            order.quantity.isNotEmpty() &&
            order.quantity.toDoubleOrNull() != null &&
            order.quantity.toDouble() > 0 &&
            (order.orderType == "Market" || order.limitPrice.isNotEmpty()) &&
            (order.orderType !in listOf("Stop", "Stop Limit") || order.stopPrice.isNotEmpty())
}