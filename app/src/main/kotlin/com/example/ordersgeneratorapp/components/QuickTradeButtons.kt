package com.example.ordersgeneratorapp.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ordersgeneratorapp.api.CreateOrderRequest
import com.example.ordersgeneratorapp.data.HotkeyPreset
import com.example.ordersgeneratorapp.repository.AlpacaRepository
import kotlinx.coroutines.launch
import android.util.Log

@Composable
fun QuickTradeButtons(
    hotkeyPresets: List<HotkeyPreset>,
    alpacaRepository: AlpacaRepository,
    onOrderResult: (String) -> Unit,
    onOrderError: (String) -> Unit,
    symbol: String? = null, // Optional: filter by symbol for market data screen
    modifier: Modifier = Modifier,
    refreshTrigger: MutableIntState // ✅ ADD: Missing parameter
) {
    val scope = rememberCoroutineScope()
    
    // Filter presets by symbol if provided, otherwise show all
    val filteredPresets = if (symbol != null) {
        hotkeyPresets.filter { it.symbol.equals(symbol, ignoreCase = true) }
    } else {
        hotkeyPresets
    }
    
    if (filteredPresets.isNotEmpty()) {
        Card(
            modifier = modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = if (symbol != null) "Quick Trade - $symbol" else "Quick Trade",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Display presets in a vertical list for full width
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filteredPresets) { preset ->
                        HotkeyPresetRow(
                            preset = preset,
                            onBuyClick = {
                                scope.launch {
                                    try {
                                        val result = alpacaRepository.createOrder(
                                            symbol = preset.symbol,
                                            quantity = preset.quantity.toInt(),
                                            side = "buy",
                                            orderType = preset.orderType,
                                            timeInForce = preset.timeInForce,
                                            limitPrice = if (preset.limitPrice.isNotEmpty()) preset.limitPrice else null,
                                            stopPrice = if (preset.stopPrice.isNotEmpty()) preset.stopPrice else null
                                        )
                                        // Handle result
                                        if (result.isSuccess) {
                                            val order = result.getOrNull()!!
                                            onOrderResult("BUY order placed! ${preset.symbol} x${preset.quantity} - Order ID: ${order.id}")
                                        } else {
                                            onOrderError(result.exceptionOrNull()?.message ?: "Failed to place buy order")
                                        }
                                    } catch (e: Exception) {
                                        onOrderError("Error placing buy order: ${e.message}")
                                    }
                                }
                            },
                            onSellClick = {
                                scope.launch {
                                    try {
                                        val result = alpacaRepository.createOrder(
                                            symbol = preset.symbol,
                                            quantity = preset.quantity.toInt(),
                                            side = "sell",
                                            orderType = preset.orderType,
                                            timeInForce = preset.timeInForce,
                                            limitPrice = if (preset.limitPrice.isNotEmpty()) preset.limitPrice else null,
                                            stopPrice = if (preset.stopPrice.isNotEmpty()) preset.stopPrice else null
                                        )
                                        // Handle result
                                        if (result.isSuccess) {
                                            val order = result.getOrNull()!!
                                            onOrderResult("SELL order placed! ${preset.symbol} x${preset.quantity} - Order ID: ${order.id}")
                                        } else {
                                            onOrderError(result.exceptionOrNull()?.message ?: "Failed to place sell order")
                                        }
                                    } catch (e: Exception) {
                                        onOrderError("Error placing sell order: ${e.message}")
                                    }
                                }
                            },
                            onReverseClick = {
                                scope.launch {
                                    executeReverseOrder(preset, alpacaRepository, onOrderResult, onOrderError)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // ✅ FIX: Add missing loadPositions call
    LaunchedEffect(refreshTrigger.intValue) {
        try {
            val result = alpacaRepository.getPositions()
            if (result.isSuccess) {
                val currentPositions = result.getOrNull() ?: emptyList()
                // Handle positions update
                Log.d("QuickTradeButtons", "Loaded ${currentPositions.size} positions")
            }
        } catch (e: Exception) {
            Log.e("QuickTradeButtons", "Error loading positions: ${e.message}", e)
        }
    }
}

@Composable
private fun HotkeyPresetRow(
    preset: HotkeyPreset,
    onBuyClick: () -> Unit,
    onSellClick: () -> Unit,
    onReverseClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Header with preset info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = preset.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${preset.symbol} × ${preset.quantity}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(
                    horizontalAlignment = androidx.compose.ui.Alignment.End
                ) {
                    Text(
                        text = preset.orderType.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = preset.timeInForce.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Show limit/stop prices if set
            if (preset.limitPrice.isNotEmpty() || preset.stopPrice.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (preset.limitPrice.isNotEmpty()) {
                        Text(
                            text = "Limit: $${preset.limitPrice}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (preset.stopPrice.isNotEmpty()) {
                        Text(
                            text = "Stop: $${preset.stopPrice}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Buy button
                Button(
                    onClick = onBuyClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("BUY", color = Color.White, fontWeight = FontWeight.Bold)
                }
                
                // Sell button  
                Button(
                    onClick = onSellClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53E3E)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("SELL", color = Color.White, fontWeight = FontWeight.Bold)
                }
                
                // Reverse button
                Button(
                    onClick = onReverseClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF9C27B0) // Purple color for reverse
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("REVERSE", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Keep the existing helper functions
private suspend fun executeOrder(
    preset: HotkeyPreset,
    side: String,
    alpacaRepository: AlpacaRepository,
    onOrderResult: (String) -> Unit,
    onOrderError: (String) -> Unit
) {
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
            onOrderResult("${side.uppercase()} order placed! ${preset.symbol} x${preset.quantity} - Order ID: ${order.id}")
        } else {
            onOrderError(result.exceptionOrNull()?.message ?: "Failed to place ${side} order")
        }
    } catch (e: Exception) {
        onOrderError("Error placing ${side} order: ${e.message}")
    }
}

private suspend fun executeReverseOrder(
    preset: HotkeyPreset,
    alpacaRepository: AlpacaRepository,
    onOrderResult: (String) -> Unit,
    onOrderError: (String) -> Unit
) {
    try {
        // First, get current positions to determine what we need to reverse
        val positionsResult = alpacaRepository.getPositions()
        
        if (positionsResult.isFailure) {
            onOrderError("Failed to get positions: ${positionsResult.exceptionOrNull()?.message}")
            return
        }
        
        val positions = positionsResult.getOrNull() ?: emptyList()
        val currentPosition = positions.find { it.symbol == preset.symbol }
        
        if (currentPosition == null) {
            onOrderError("No existing position found for ${preset.symbol} to reverse")
            return
        }
        
        val currentQty = currentPosition.qty.toDoubleOrNull() ?: 0.0
        
        if (currentQty == 0.0) {
            onOrderError("No position to reverse for ${preset.symbol} (quantity is 0)")
            return
        }
        
        // Simple reverse logic: buy/sell double the current position
        val reverseQty = (kotlin.math.abs(currentQty) * 2).toInt()
        val reverseSide = if (currentQty > 0) "sell" else "buy"
        
        val result = alpacaRepository.createOrder(
            symbol = preset.symbol,
            quantity = reverseQty,
            side = reverseSide,
            orderType = preset.orderType,
            timeInForce = preset.timeInForce,
            limitPrice = if (preset.limitPrice.isNotEmpty()) preset.limitPrice else null,
            stopPrice = if (preset.stopPrice.isNotEmpty()) preset.stopPrice else null
        )
        
        if (result.isSuccess) {
            val order = result.getOrNull()!!
            val positionDescription = if (currentQty > 0) "LONG to SHORT" else "SHORT to LONG"
            onOrderResult("🔄 REVERSE order placed! ${preset.symbol} ${positionDescription} x${reverseQty} - Order ID: ${order.id}")
        } else {
            onOrderError(result.exceptionOrNull()?.message ?: "Failed to place reverse order")
        }
        
    } catch (e: Exception) {
        onOrderError("Error placing reverse order: ${e.message}")
    }
}