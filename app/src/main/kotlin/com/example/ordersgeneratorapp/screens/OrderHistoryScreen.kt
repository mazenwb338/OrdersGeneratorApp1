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
import android.util.Log
import com.example.ordersgeneratorapp.api.AlpacaOrder
import com.example.ordersgeneratorapp.repository.AlpacaRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHistoryScreen(
    onBackClick: () -> Unit,
    alpacaRepository: AlpacaRepository
) {
    var orders by remember { mutableStateOf<List<AlpacaOrder>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedStatus by remember { mutableStateOf("all") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Status options for filtering
    val statusOptions = listOf(
        "all" to "All Orders",
        "filled" to "Filled", 
        "open" to "Open",
        "canceled" to "Canceled",
        "pending_new" to "Pending"
    )

    fun loadOrders() {
        scope.launch {
            isLoading = true
            errorMessage = null
            
            try {
                val status = if (selectedStatus == "all") null else selectedStatus
                val result = alpacaRepository.getOrders(
                    status = status,
                    limit = 100,
                    direction = "desc"
                )
                
                if (result.isSuccess) {
                    orders = result.getOrNull() ?: emptyList()
                    Log.d("OrderHistoryScreen", "Loaded ${orders.size} orders with status: $selectedStatus")
                } else {
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to load orders"
                    Log.e("OrderHistoryScreen", "Failed to load orders: $errorMessage")
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
                Log.e("OrderHistoryScreen", "Exception loading orders: ${e.message}", e)
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(selectedStatus) {
        loadOrders()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Order History") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { loadOrders() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
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
            // Status Filter
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Filter by Status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(statusOptions) { (status, label) ->
                            FilterChip(
                                selected = selectedStatus == status,
                                onClick = { selectedStatus = status },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Loading orders...")
                        }
                    }
                }
                
                errorMessage != null -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorMessage!!,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { loadOrders() }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                
                orders.isEmpty() -> {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Assignment,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (selectedStatus == "all") "No orders found" else "No ${statusOptions.find { it.first == selectedStatus }?.second?.lowercase()} orders found",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Orders will appear here after you place them",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(orders) { order ->
                            OrderCard(
                                order = order,
                                onCancelOrder = { orderId ->
                                    scope.launch {
                                        val result = alpacaRepository.cancelOrder(orderId)
                                        if (result.isSuccess) {
                                            snackbarHostState.showSnackbar("Order cancelled successfully")
                                            loadOrders()
                                        } else {
                                            snackbarHostState.showSnackbar(
                                                "Failed to cancel order: ${result.exceptionOrNull()?.message}"
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderCard(
    order: AlpacaOrder,
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
                        text = order.symbol,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${order.side.uppercase()} ${order.qty} shares",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                OrderDetailRow("Type", order.orderType.uppercase())
                OrderDetailRow("Time in Force", order.timeInForce.uppercase())
                
                order.limitPrice?.let { price ->
                    if (price.isNotEmpty() && price != "null") {
                        OrderDetailRow("Limit Price", "$$price")
                    }
                }
                
                order.stopPrice?.let { price ->
                    if (price.isNotEmpty() && price != "null") {
                        OrderDetailRow("Stop Price", "$$price")
                    }
                }
                
                order.filledQty?.let { filled ->
                    if (filled.isNotEmpty() && filled != "0") {
                        OrderDetailRow("Filled", "$filled shares")
                    }
                }
                
                OrderDetailRow("Order ID", order.id)
                
                // Format and show created date (moved outside composable)
                val formattedDate = remember(order.createdAt) {
                    try {
                        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault())
                        val outputFormat = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
                        val date = inputFormat.parse(order.createdAt)
                        if (date != null) {
                            outputFormat.format(date)
                        } else {
                            order.createdAt
                        }
                    } catch (e: Exception) {
                        order.createdAt
                    }
                }
                
                OrderDetailRow("Created", formattedDate)
            }
            
            // Show cancel button for open orders
            if (order.status.lowercase() in listOf("new", "accepted", "pending_new", "partially_filled")) {
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
        "new", "accepted", "pending_new" -> Color(0xFF2196F3) // Blue
        "partially_filled" -> Color(0xFFFF9800) // Orange
        "filled" -> Color(0xFF4CAF50) // Green
        "canceled", "rejected" -> Color(0xFFE53E3E) // Red
        else -> Color.Gray
    }
}