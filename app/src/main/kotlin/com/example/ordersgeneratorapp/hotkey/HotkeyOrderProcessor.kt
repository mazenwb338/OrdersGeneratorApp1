package com.example.ordersgeneratorapp.hotkey

import android.util.Log
import com.example.ordersgeneratorapp.data.BrokerAccount
import com.example.ordersgeneratorapp.data.HotkeyPreset
import com.example.ordersgeneratorapp.repository.AlpacaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Independent hotkey order processing module
 * Handles order execution for hotkey presets across multiple accounts
 */
class HotkeyOrderProcessor(
    private val alpacaRepository: AlpacaRepository
) {
    companion object {
        private const val TAG = "HotkeyOrderProcessor"
    }

    /**
     * Execute a hotkey order across multiple accounts
     */
    suspend fun executeHotkeyOrder(
        preset: HotkeyPreset,
        side: String, // "buy" or "sell"
        accounts: List<BrokerAccount>
    ): HotkeyExecutionResult = withContext(Dispatchers.IO) {
        
        val sessionId = UUID.randomUUID().toString().take(8)
        Log.d(TAG, "HOTKEY_EXEC_START sessionId=$sessionId preset=${preset.name} side=$side accounts=${accounts.size}")
        
        val results = mutableListOf<AccountOrderResult>()
        
        accounts.forEach { account ->
            val accountResult = executeOrderForAccount(
                preset = preset,
                side = side,
                account = account,
                sessionId = sessionId
            )
            results.add(accountResult)
        }
        
        val successCount = results.count { it.success }
        val totalCount = results.size
        
        Log.d(TAG, "HOTKEY_EXEC_COMPLETE sessionId=$sessionId success=$successCount/$totalCount")
        
        HotkeyExecutionResult(
            sessionId = sessionId,
            preset = preset,
            side = side,
            accountResults = results,
            successCount = successCount,
            totalCount = totalCount
        )
    }
    
    /**
     * Execute order for a single account using existing repository method
     */
    private suspend fun executeOrderForAccount(
        preset: HotkeyPreset,
        side: String,
        account: BrokerAccount,
        sessionId: String
    ): AccountOrderResult {
        
        return try {
            // Generate unique client order ID for this account
            val clientOrderId = "HOTKEY_${sessionId}_${account.id}_${side}_${System.currentTimeMillis()}"
            
            Log.d(TAG, "ACCOUNT_ORDER_START account=${account.accountName} clientId=$clientOrderId")
            
            // Configure repository for this account
            alpacaRepository.configureFromBrokerAccount(account)
            
            // ✅ Use the existing createOrder method (no CreateOrderRequest needed)
            val result = alpacaRepository.createOrder(
                symbol = preset.symbol,
                quantity = preset.quantity.toIntOrNull() ?: 1,
                side = side,
                orderType = preset.orderType,
                timeInForce = preset.timeInForce,
                limitPrice = preset.limitPrice.ifBlank { null },
                stopPrice = preset.stopPrice.ifBlank { null },
                clientOrderId = clientOrderId
            )
            
            if (result.isSuccess) {
                val order = result.getOrNull()!!
                
                Log.d(TAG, "ACCOUNT_ORDER_SUCCESS account=${account.accountName}")
                Log.d(TAG, "  - Alpaca Server Order ID: ${order.id}")
                Log.d(TAG, "  - Client Order ID: ${order.clientOrderId}")
                Log.d(TAG, "  - Symbol: ${order.symbol}")
                Log.d(TAG, "  - Side: ${order.side}")
                Log.d(TAG, "  - Quantity: ${order.qty}")
                Log.d(TAG, "  - Status: ${order.status}")
                
                AccountOrderResult(
                    account = account,
                    success = true,
                    alpacaOrderId = order.id,
                    clientOrderId = order.clientOrderId ?: clientOrderId,
                    symbol = order.symbol,
                    side = order.side,
                    quantity = order.qty,
                    status = order.status,
                    errorMessage = null
                )
            } else {
                val error = result.exceptionOrNull()?.message ?: "Unknown error"
                Log.e(TAG, "ACCOUNT_ORDER_FAILED account=${account.accountName} error=$error")
                
                AccountOrderResult(
                    account = account,
                    success = false,
                    alpacaOrderId = null,
                    clientOrderId = clientOrderId,
                    symbol = preset.symbol,
                    side = side,
                    quantity = preset.quantity,
                    status = "FAILED",
                    errorMessage = error
                )
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "ACCOUNT_ORDER_EXCEPTION account=${account.accountName}", e)
            
            AccountOrderResult(
                account = account,
                success = false,
                alpacaOrderId = null,
                clientOrderId = "ERROR_${System.currentTimeMillis()}",
                symbol = preset.symbol,
                side = side,
                quantity = preset.quantity,
                status = "EXCEPTION",
                errorMessage = e.message ?: "Unknown exception"
            )
        }
    }
}

/**
 * Result of executing a hotkey across multiple accounts
 */
data class HotkeyExecutionResult(
    val sessionId: String,
    val preset: HotkeyPreset,
    val side: String,
    val accountResults: List<AccountOrderResult>,
    val successCount: Int,
    val totalCount: Int
) {
    val isFullSuccess: Boolean get() = successCount == totalCount
    val hasPartialSuccess: Boolean get() = successCount > 0 && successCount < totalCount
    val isCompleteFailure: Boolean get() = successCount == 0
    
    val summary: String get() = "$side ${preset.symbol}: $successCount/$totalCount orders placed"
    
    val successfulOrders: List<AccountOrderResult> get() = accountResults.filter { it.success }
    val failedOrders: List<AccountOrderResult> get() = accountResults.filter { !it.success }
}

/**
 * Result of executing an order for a single account
 */
data class AccountOrderResult(
    val account: BrokerAccount,
    val success: Boolean,
    val alpacaOrderId: String?, // This is the UNIQUE Alpaca server order ID
    val clientOrderId: String,   // This is our client-generated unique ID
    val symbol: String,
    val side: String,
    val quantity: String,
    val status: String,
    val errorMessage: String?
) {
    val displaySummary: String get() = "${account.accountName}: ${if (success) "✅" else "❌"} $side $symbol"
    
    val detailedSummary: String get() = buildString {
        append("${account.accountName}: ")
        if (success) {
            append("✅ $side $quantity x $symbol")
            append(" | Alpaca ID: $alpacaOrderId")
            append(" | Status: $status")
        } else {
            append("❌ $side $quantity x $symbol")
            append(" | Error: $errorMessage")
        }
    }
}