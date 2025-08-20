package com.example.ordersgeneratorapp.hotkey

import android.util.Log
import com.example.ordersgeneratorapp.data.BrokerAccount
import com.example.ordersgeneratorapp.data.ConnectionSettings
import com.example.ordersgeneratorapp.data.HotkeyPreset
import com.example.ordersgeneratorapp.repository.AlpacaRepository

/**
 * Manages hotkey execution with account filtering and spam prevention
 */
class HotkeyManager(
    private val processor: HotkeyOrderProcessor
) {
    companion object {
        private const val TAG = "HotkeyManager"
        private const val SPAM_PROTECTION_MS = 2000L // 2 seconds
    }
    
    // Track last execution times for spam prevention
    private val lastExecutionTimes = mutableMapOf<String, Long>()
    
    /**
     * Execute hotkey with account filtering and spam prevention
     */
    suspend fun executeHotkey(
        preset: HotkeyPreset,
        side: String,
        connectionSettings: ConnectionSettings,
        onResult: (HotkeyExecutionResult) -> Unit
    ) {
        val hotkeyKey = "${preset.id}_$side"
        val now = System.currentTimeMillis()
        
        // Check spam protection
        val lastExecution = lastExecutionTimes[hotkeyKey] ?: 0
        if (now - lastExecution < SPAM_PROTECTION_MS) {
            val waitTime = SPAM_PROTECTION_MS - (now - lastExecution)
            Log.d(TAG, "SPAM_BLOCKED hotkey=$hotkeyKey waitTime=${waitTime}ms")
            return
        }
        
        lastExecutionTimes[hotkeyKey] = now
        Log.d(TAG, "HOTKEY_EXECUTE preset=${preset.name} side=$side")
        
        // Filter accounts for this hotkey
        val eligibleAccounts = getEligibleAccounts(preset, connectionSettings)
        
        Log.d(TAG, "ACCOUNT_FILTER:")
        Log.d(TAG, "  - Total accounts: ${connectionSettings.brokerAccounts.size}")
        Log.d(TAG, "  - Alpaca accounts: ${connectionSettings.brokerAccounts.count { it.brokerType == "Alpaca" }}")
        Log.d(TAG, "  - Enabled Alpaca: ${connectionSettings.brokerAccounts.count { it.brokerType == "Alpaca" && it.isEnabled }}")
        Log.d(TAG, "  - Selected for preset: ${eligibleAccounts.size}")
        
        eligibleAccounts.forEach { account ->
            Log.d(TAG, "    ✓ ${account.accountName} (ID: ${account.id})")
        }
        
        if (eligibleAccounts.isEmpty()) {
            Log.w(TAG, "NO_ELIGIBLE_ACCOUNTS preset=${preset.name}")
            return
        }
        
        // Execute orders
        val result = processor.executeHotkeyOrder(
            preset = preset,
            side = side,
            accounts = eligibleAccounts
        )
        
        // Log detailed results
        logExecutionResults(result)
        
        // Return results to UI
        onResult(result)
    }
    
    /**
     * Get accounts eligible for this hotkey preset
     */
    private fun getEligibleAccounts(
        preset: HotkeyPreset,
        connectionSettings: ConnectionSettings
    ): List<BrokerAccount> {
        return connectionSettings.brokerAccounts.filter { account ->
            // Must be Alpaca account
            account.brokerType == "Alpaca" &&
            // Must be enabled
            account.isEnabled &&
            // Must be selected for this preset (or no specific selection = all accounts)
            (preset.selectedBrokerIds.isEmpty() || preset.selectedBrokerIds.contains(account.id))
        }
    }
    
    /**
     * Log detailed execution results
     */
    private fun logExecutionResults(result: HotkeyExecutionResult) {
        Log.d(TAG, "EXECUTION_RESULTS sessionId=${result.sessionId}")
        Log.d(TAG, "  - Summary: ${result.summary}")
        Log.d(TAG, "  - Status: ${when {
            result.isFullSuccess -> "FULL_SUCCESS"
            result.hasPartialSuccess -> "PARTIAL_SUCCESS"
            result.isCompleteFailure -> "COMPLETE_FAILURE"
            else -> "UNKNOWN"
        }}")
        
        result.successfulOrders.forEach { order ->
            Log.d(TAG, "  ✅ SUCCESS: ${order.detailedSummary}")
        }
        
        result.failedOrders.forEach { order ->
            Log.e(TAG, "  ❌ FAILED: ${order.detailedSummary}")
        }
    }
}