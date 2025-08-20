package com.example.ordersgeneratorapp.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    onNavigateToMarketData: () -> Unit,
    onNavigateToHotkeys: () -> Unit = {}
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
                    onNavigateToMarketData = onNavigateToMarketData,
                    onNavigateToHotkeys = onNavigateToHotkeys
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
                    Text("Retry Connection", color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

@Composable
private fun QuickActionsCard(
    onNavigateToOrderPlacement: () -> Unit,
    onNavigateToOrderHistory: () -> Unit,
    onNavigateToMarketData: () -> Unit,
    onNavigateToHotkeys: () -> Unit
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
                    icon = Icons.Default.ShowChart,
                    text = "Market Data",
                    onClick = onNavigateToMarketData
                )
                
                QuickActionButton(
                    icon = Icons.Default.History,
                    text = "Order History",
                    onClick = onNavigateToOrderHistory
                )
                
                QuickActionButton(
                    icon = Icons.Default.Settings,
                    text = "Hotkeys",
                    onClick = onNavigateToHotkeys
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
        modifier = Modifier.size(width = 80.dp, height = 90.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (account != null) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AccountInfoRow("Account ID", account.id)
                    AccountInfoRow("Account Number", account.accountNumber)
                    AccountInfoRow("Buying Power", "$${account.buyingPower}")
                    AccountInfoRow("Cash", "$${account.cash}")
                    AccountInfoRow("Portfolio Value", "$${account.portfolioValue}")
                    AccountInfoRow("Equity", "$${account.equity}")
                    AccountInfoRow("Day Trade Count", account.daytradeCount.toString())
                    AccountInfoRow("Currency", account.currency)
                    AccountInfoRow(
                        "Status", 
                        account.status,
                        statusColor = when (account.status) {
                            "ACTIVE" -> Color(0xFF4CAF50)
                            else -> Color(0xFFF44336)
                        }
                    )
                }
            } else {
                Text(
                    text = "Account data not available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AccountInfoRow(
    label: String, 
    value: String, 
    statusColor: Color? = null
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
            fontWeight = FontWeight.Medium,
            color = statusColor ?: MaterialTheme.colorScheme.onSurface
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
                
                if (positions.isNotEmpty()) {
                    TextButton(onClick = onViewDetails) {
                        Text("View All")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (positions.isEmpty()) {
                Text(
                    text = "No open positions",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // Show first 3 positions
                positions.take(3).forEach { position ->
                    PositionRow(position)
                    if (position != positions.take(3).last()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PositionRow(position: AlpacaPosition) {
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
        
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "$${position.marketValue}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            val unrealizedPl = position.unrealizedPl.toDoubleOrNull() ?: 0.0
            Text(
                text = "${if (unrealizedPl >= 0) "+" else ""}$${position.unrealizedPl}",
                style = MaterialTheme.typography.bodySmall,
                color = if (unrealizedPl >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
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
                    text = "Recent Orders (${orders.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                if (orders.isNotEmpty()) {
                    TextButton(onClick = onViewHistory) {
                        Text("View All")
                    }
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
                // Show first 3 orders
                orders.take(3).forEach { order ->
                    OrderRow(order)
                    if (order != orders.take(3).last()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderRow(order: AlpacaOrder) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "${order.symbol} ${order.side.uppercase()}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${order.qty} shares @ ${order.orderType}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            order.clientOrderId?.let {
                Text(
                    text = "CID ${it.takeLast(12)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Text(
            text = order.status.uppercase(),
            style = MaterialTheme.typography.bodySmall,
            color = when (order.status.lowercase()) {
                "filled" -> Color(0xFF4CAF50)
                "canceled" -> Color(0xFFF44336)
                "pending_new", "new" -> Color(0xFF2196F3)
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            fontWeight = FontWeight.Medium
        )
    }
}

// Refresh data function
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
        // Load account
        val accountResult = repository.getAccount()
        if (accountResult.isSuccess) {
            onAccountLoaded(accountResult.getOrNull())
            onConnectionChanged(true)
        } else {
            onConnectionChanged(false)
            onError("Failed to load account: ${accountResult.exceptionOrNull()?.message}")
        }
        
        // Load positions
        val positionsResult = repository.getPositions()
        if (positionsResult.isSuccess) {
            onPositionsLoaded(positionsResult.getOrNull() ?: emptyList())
        }
        
        // Load recent orders
        val ordersResult = repository.getOrders(limit = 10)
        if (ordersResult.isSuccess) {
            onOrdersLoaded(ordersResult.getOrNull() ?: emptyList())
        }
        
    } catch (e: Exception) {
        onConnectionChanged(false)
        onError("Connection error: ${e.message}")
    } finally {
        onLoadingChanged(false)
    }
}