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
                    accountId = account.id,
                    accountName = account.accountName,
                    success = true,
                    alpacaOrderId = order.id,
                    clientOrderId = order.clientOrderId ?: clientOrderId,
                    errorMessage = null,
                    executionTimeMs = System.currentTimeMillis()
                )
            } else {
                val error = result.exceptionOrNull()?.message ?: "Unknown error"
                Log.e(TAG, "ACCOUNT_ORDER_FAILED account=${account.accountName} error=$error")
                
                AccountOrderResult(
                    accountId = account.id,
                    accountName = account.accountName,
                    success = false,
                    alpacaOrderId = null,
                    clientOrderId = clientOrderId,
                    errorMessage = handleOrderError(error),
                    executionTimeMs = System.currentTimeMillis()
                )
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "ACCOUNT_ORDER_EXCEPTION account=${account.accountName}", e)
            
            AccountOrderResult(
                accountId = account.id,
                accountName = account.accountName,
                success = false,
                alpacaOrderId = null,
                clientOrderId = "ERROR_${System.currentTimeMillis()}",
                errorMessage = e.message ?: "Unknown exception",
                executionTimeMs = System.currentTimeMillis()
            )
        }
    }

    /**
     * Handle order error and return user-friendly message
     */
    private fun handleOrderError(error: String): String {
        return when {
            error.contains("wash trade detected") -> 
                "⚠️ Wash trade detected - you have opposite orders. Cancel existing orders first."
            error.contains("403") -> 
                "🚫 Order rejected by broker - check account permissions"
            error.contains("insufficient buying power") ->
                "💰 Insufficient buying power"
            else -> 
                "❌ Order failed: ${error.take(100)}"
        }
    }
}

/**
 * Result of hotkey execution across multiple accounts
 */
data class HotkeyExecutionResult(
    val sessionId: String,
    val preset: HotkeyPreset,
    val side: String,
    val accountResults: List<AccountOrderResult>,
    val successCount: Int,
    val totalCount: Int
) {
    // ✅ ADD: Missing properties that MarketDataScreen expects
    val isFullSuccess: Boolean get() = successCount == totalCount && totalCount > 0
    val hasPartialSuccess: Boolean get() = successCount > 0 && successCount < totalCount
    val isCompleteFailure: Boolean get() = successCount == 0
    
    val summary: String get() = when {
        isFullSuccess -> "All orders successful ($successCount/$totalCount)"
        hasPartialSuccess -> "Partial success ($successCount/$totalCount)"
        else -> "All orders failed (0/$totalCount)"
    }
}

/**
 * Result for individual account order execution
 */
data class AccountOrderResult(
    val accountId: String,
    val accountName: String,
    val success: Boolean,
    val alpacaOrderId: String? = null,
    val clientOrderId: String? = null,
    val errorMessage: String? = null,
    val executionTimeMs: Long = 0
) {
    val statusEmoji: String get() = if (success) "✅" else "❌"
}