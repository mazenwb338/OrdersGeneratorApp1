package com.example.ordersgeneratorapp.util

import android.util.Log
import com.example.ordersgeneratorapp.data.BrokerAccount
import com.example.ordersgeneratorapp.data.HotkeyPreset
import com.example.ordersgeneratorapp.repository.AlpacaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OrderExecutionManager(
    private val repository: AlpacaRepository
) {
    companion object {
        private const val TAG = "OrderExecutionManager"
    }
    
    private val hotkeyLastFire = mutableMapOf<String, Long>()
    
    fun canFireHotkey(key: String, cooldownMs: Long = 2000): Boolean {
        val now = System.currentTimeMillis()
        val lastFire = hotkeyLastFire[key] ?: 0
        return if (now - lastFire >= cooldownMs) {
            hotkeyLastFire[key] = now
            Log.d(TAG, "HOTKEY_FIRE_ALLOWED key=$key")
            true
        } else {
            Log.d(TAG, "HOTKEY_FIRE_BLOCKED key=$key waitTime=${cooldownMs - (now - lastFire)}ms")
            false
        }
    }

    suspend fun executePresetOrder(
        preset: HotkeyPreset,
        side: String,
        accounts: List<BrokerAccount>,
        show: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        
        // Generate base timestamp ONCE for this execution
        val baseTimestamp = System.currentTimeMillis()
        Log.d(TAG, "EXEC_START preset=${preset.id} side=$side accounts=${accounts.size} baseTime=$baseTimestamp")
        
        accounts.forEachIndexed { index, account ->
            try {
                // Account-specific unique client order ID with micro-delay to ensure uniqueness
                val uniqueTimestamp = baseTimestamp + index // Add index as milliseconds
                val clientOrderId = "hk-${account.id.take(6)}-${preset.id.take(4)}-${side.take(1)}-$uniqueTimestamp"
                
                Log.d(TAG, "EXEC_ORDER account=${account.accountName} clientId=$clientOrderId")
                
                // Configure repository for this specific account
                repository.configureFromBrokerAccount(account)
                
                // Use the existing createOrder method
                val result = repository.createOrder(
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
                    Log.d(TAG, "EXEC_SUCCESS account=${account.accountName} alpacaOrderId=${order.id} clientOrderId=${order.clientOrderId}")
                    show("✓ ${side.uppercase()} ${preset.symbol} x${preset.quantity} | ${account.accountName} | Alpaca ID: ${order.id}")
                } else {
                    Log.e(TAG, "EXEC_FAIL account=${account.accountName} error=${result.exceptionOrNull()?.message}")
                    show("✗ ${account.accountName}: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "EXEC_EXCEPTION account=${account.accountName}", e)
                show("✗ ${account.accountName}: ${e.message}")
            }
        }
        
        Log.d(TAG, "EXEC_COMPLETE preset=${preset.id} side=$side")
    }
}