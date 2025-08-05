package com.example.ordersgeneratorapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.ordersgeneratorapp.repository.AlpacaRepository
import com.example.ordersgeneratorapp.api.AlpacaAccount
import com.example.ordersgeneratorapp.api.AlpacaPosition
import com.example.ordersgeneratorapp.api.AlpacaOrder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    repository: AlpacaRepository,
    onNavigateToSettings: () -> Unit,
    onNavigateToOrderPlacement: () -> Unit,
    onNavigateToOrderHistory: () -> Unit,
    onNavigateToMarketData: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    
    var account by remember { mutableStateOf<AlpacaAccount?>(null) }
    var positions by remember { mutableStateOf<List<AlpacaPosition>>(emptyList()) }
    var recentOrders by remember { mutableStateOf<List<AlpacaOrder>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isConnected by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    
    // Load data on first composition
    LaunchedEffect(Unit) {
        refreshData(repository, 
            onAccountLoaded = { account = it },
            onPositionsLoaded = { positions = it },
            onOrdersLoaded = { recentOrders = it },
            onLoadingChanged = { isLoading = it },
            onConnectionChanged = { isConnected = it },
            onError = { errorMessage = it }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trading Dashboard") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    IconButton(
                        onClick = {
                            scope.launch {
                                refreshData(repository,
                                    onAccountLoaded = { account = it },
                                    onPositionsLoaded = { positions = it },
                                    onOrdersLoaded = { recentOrders = it },
                                    onLoadingChanged = { isLoading = it },
                                    onConnectionChanged = { isConnected = it },
                                    onError = { errorMessage = it }
                                )
                            }
                        }
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Connection Status Card
            item {
                ConnectionStatusCard(
                    isConnected = isConnected,
                    isLoading = isLoading,
                    errorMessage = errorMessage,
                    onRetryClick = {
                        scope.launch {
                            refreshData(repository,
                                onAccountLoaded = { account = it },
                                onPositionsLoaded = { positions = it },
                                onOrdersLoaded = { recentOrders = it },
                                onLoadingChanged = { isLoading = it },
                                onConnectionChanged = { isConnected = it },
                                onError = { errorMessage = it }
                            )
                        }
                    }
                )
            }
            
            // Quick Actions Card
            item {
                QuickActionsCard(
                    onNavigateToOrderPlacement = onNavigateToOrderPlacement,
                    onNavigateToOrderHistory = onNavigateToOrderHistory,
                    onNavigateToMarketData = onNavigateToMarketData
                )
            }
            
            // Account Information Card
            item {
                AccountCard(
                    account = account,
                    isLoading = isLoading
                )
            }
            
            // Positions Card
            item {
                PositionsCard(
                    positions = positions,
                    onViewDetails = onNavigateToMarketData
                )
            }
            
            // Recent Orders Card
            item {
                RecentOrdersCard(
                    orders = recentOrders,
                    onViewHistory = onNavigateToOrderHistory
                )
            }
        }
    }
}

@Composable
private fun ConnectionStatusCard(
    isConnected: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    onRetryClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isLoading -> MaterialTheme.colorScheme.surface
                isConnected -> Color(0xFF4CAF50)
                else -> Color(0xFFF44336)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = when {
                    isLoading -> Icons.Default.Refresh
                    isConnected -> Icons.Default.CheckCircle
                    else -> Icons.Default.Error
                },
                contentDescription = null,
                tint = when {
                    isLoading -> MaterialTheme.colorScheme.onSurface
                    isConnected -> Color.White
                    else -> Color.White
                },
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = when {
                    isLoading -> "Connecting..."
                    isConnected -> "Connected to Alpaca"
                    else -> "Connection Failed"
                },
                style = MaterialTheme.typography.titleMedium,
                color = when {
                    isLoading -> MaterialTheme.colorScheme.onSurface
                    isConnected -> Color.White
                    else -> Color.White
                },
                fontWeight = FontWeight.Bold
            )
            
            errorMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }
            
            if (!isConnected && !isLoading) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onRetryClick) {
                    Text("Retry")
                }
            }
        }
    }
}

@Composable
private fun QuickActionsCard(
    onNavigateToOrderPlacement: () -> Unit,
    onNavigateToOrderHistory: () -> Unit,
    onNavigateToMarketData: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickActionButton(
                    icon = Icons.Default.Add,
                    text = "Place Order",
                    onClick = onNavigateToOrderPlacement
                )
                
                QuickActionButton(
                    icon = Icons.Default.History,
                    text = "Order History",
                    onClick = onNavigateToOrderHistory
                )
                
                QuickActionButton(
                    icon = Icons.Default.TrendingUp,
                    text = "Market Data",
                    onClick = onNavigateToMarketData
                )
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.size(80.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun AccountCard(
    account: AlpacaAccount?,
    isLoading: Boolean
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Account Information",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Loading account information...")
                }
            } else if (account != null) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AccountInfoRow("Account ID", account.id)
                    AccountInfoRow("Status", account.status)
                    AccountInfoRow("Buying Power", "$${account.buyingPower}")
                    AccountInfoRow("Cash", "$${account.cash}")
                    AccountInfoRow("Portfolio Value", "$${account.portfolioValue}")
                }
            } else {
                Text(
                    text = "Unable to load account information",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun AccountInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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

@Composable
private fun PositionsCard(
    positions: List<AlpacaPosition>,
    onViewDetails: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Positions (${positions.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onViewDetails) {
                    Text("View All")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (positions.isEmpty()) {
                Text(
                    text = "No positions found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                positions.take(3).forEach { position ->
                    PositionItem(position = position)
                    if (position != positions.take(3).last()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PositionItem(position: AlpacaPosition) {
    val isProfit = position.unrealizedPl?.toDoubleOrNull()?.let { it >= 0 } ?: false
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = position.symbol,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${position.qty} shares",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = position.symbol,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = if (isProfit) "+$${position.unrealizedPl}" else "-$${position.unrealizedPl?.removePrefix("-")}",
                style = MaterialTheme.typography.bodySmall,
                color = if (isProfit) Color(0xFF4CAF50) else Color(0xFFF44336)
            )
        }
    }
}

@Composable
private fun RecentOrdersCard(
    orders: List<AlpacaOrder>,
    onViewHistory: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Orders",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onViewHistory) {
                    Text("View All")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (orders.isEmpty()) {
                Text(
                    text = "No recent orders",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                orders.take(3).forEach { order ->
                    OrderItem(order = order)
                    if (order != orders.take(3).last()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderItem(order: AlpacaOrder) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = order.symbol,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${order.side} ${order.qty}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = order.symbol,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = order.status,
                style = MaterialTheme.typography.bodySmall,
                color = when (order.status.lowercase()) {
                    "filled" -> Color(0xFF4CAF50)
                    "canceled", "rejected" -> Color(0xFFF44336)
                    else -> MaterialTheme.colorScheme.primary
                }
            )
        }
    }
}

private suspend fun refreshData(
    repository: AlpacaRepository,
    onAccountLoaded: (AlpacaAccount?) -> Unit,
    onPositionsLoaded: (List<AlpacaPosition>) -> Unit,
    onOrdersLoaded: (List<AlpacaOrder>) -> Unit,
    onLoadingChanged: (Boolean) -> Unit,
    onConnectionChanged: (Boolean) -> Unit,
    onError: (String?) -> Unit
) {
    onLoadingChanged(true)
    onError(null)
    
    try {
        // Simulate network delay
        delay(1000)
        
        val accountResult = repository.getAccount()
        val accountInfo = if (accountResult.isSuccess) {
            accountResult.getOrNull()
        } else {
            null
        }
        onAccountLoaded(accountInfo)
        
        val positionsResult = repository.getPositions()
        val positions = if (positionsResult.isSuccess) {
            positionsResult.getOrNull() ?: emptyList()
        } else {
            emptyList()
        }
        onPositionsLoaded(positions)
        
        val ordersResult = repository.getOrders()
        val orders = if (ordersResult.isSuccess) {
            ordersResult.getOrNull() ?: emptyList()
        } else {
            emptyList()
        }
        onOrdersLoaded(orders)
        
        onConnectionChanged(accountResult.isSuccess)
    } catch (e: Exception) {
        onConnectionChanged(false)
        onError(e.message ?: "Unknown error occurred")
    } finally {
        onLoadingChanged(false)
    }
}