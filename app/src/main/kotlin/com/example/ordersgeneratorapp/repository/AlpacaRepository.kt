package com.example.ordersgeneratorapp.repository

import android.util.Log
import com.example.ordersgeneratorapp.api.*
import com.example.ordersgeneratorapp.data.AlpacaSettings
import com.example.ordersgeneratorapp.util.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AlpacaRepository private constructor(
    private val settingsManager: SettingsManager,
    private val apiClient: AlpacaApiClient
) {
    companion object {
        private const val TAG = "AlpacaRepository"
        
        @Volatile
        private var INSTANCE: AlpacaRepository? = null
        
        fun getInstance(settingsManager: SettingsManager): AlpacaRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AlpacaRepository(settingsManager, AlpacaApiClient).also { INSTANCE = it }
            }
        }
        
        operator fun invoke(settingsManager: SettingsManager): AlpacaRepository {
            return getInstance(settingsManager)
        }
    }

    fun isConfigured(): Boolean {
        val connectionSettings = settingsManager.getConnectionSettings()
        return connectionSettings.alpaca.apiKey.isNotEmpty() && 
               connectionSettings.alpaca.secretKey.isNotEmpty()
    }
    
    fun updateSettings(alpacaSettings: AlpacaSettings) {
        apiClient.updateSettings(alpacaSettings)
    }

    suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!isConfigured()) {
                return@withContext Result.failure(Exception("Alpaca API not configured"))
            }
            
            val response = apiClient.tradingApi.getAccount()
            if (response.isSuccessful) {
                val account = response.body()
                Result.success("✅ Connected successfully! Account: ${account?.accountNumber}")
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Result.failure(Exception("Connection failed: ${response.code()} $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Test connection failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun testOrdersEndpoint(): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!isConfigured()) {
                return@withContext Result.failure(Exception("Alpaca API not configured"))
            }
            
            val response = apiClient.tradingApi.getOrders(limit = 1)
            if (response.isSuccessful) {
                Result.success("Orders endpoint accessible")
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Result.failure(Exception("Orders endpoint failed: ${response.code()} $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Test orders endpoint failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getAccount(): Result<AlpacaAccount> = withContext(Dispatchers.IO) {
        try {
            if (!isConfigured()) {
                return@withContext Result.failure(Exception("Alpaca API not configured"))
            }
            
            val response = apiClient.tradingApi.getAccount()
            if (response.isSuccessful) {
                response.body()?.let { account ->
                    Log.d(TAG, "Account loaded: ${account.accountNumber}")
                    Result.success(account)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "Failed to get account: ${response.code()} ${response.message()}, Body: $errorBody")
                Result.failure(Exception("HTTP ${response.code()}: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception getting account: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun createOrder(
        symbol: String,
        quantity: Int,
        side: String,
        orderType: String = "market",
        timeInForce: String = "day",
        limitPrice: String? = null,
        stopPrice: String? = null
    ): Result<AlpacaOrder> = withContext(Dispatchers.IO) {
        try {
            if (!isConfigured()) {
                return@withContext Result.failure(Exception("Alpaca API not configured"))
            }
            
            Log.d(TAG, "Creating order: $side $quantity $symbol at $orderType with timeInForce: $timeInForce")
            
            val request = CreateOrderRequest(
                symbol = symbol,
                qty = quantity.toString(),
                side = side,
                type = orderType,
                timeInForce = timeInForce.lowercase(),
                limitPrice = limitPrice,
                stopPrice = stopPrice
            )
            
            val response = apiClient.tradingApi.createOrder(request)
            if (response.isSuccessful) {
                response.body()?.let { order ->
                    Log.d(TAG, "Order created successfully: ${order.id}")
                    Result.success(order)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "Failed to create order: ${response.code()} ${response.message()}, Body: $errorBody")
                Result.failure(Exception("HTTP ${response.code()}: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception creating order", e)
            Result.failure(e)
        }
    }

    suspend fun getOrders(
        status: String? = null, // null means all statuses
        limit: Int? = 50,
        direction: String? = "desc"
    ): Result<List<AlpacaOrder>> = withContext(Dispatchers.IO) {
        try {
            if (!isConfigured()) {
                return@withContext Result.failure(Exception("Alpaca API not configured"))
            }
            
            val response = apiClient.tradingApi.getOrders(
                status = status, // Pass null to get all orders
                limit = limit,
                direction = direction
            )
            
            if (response.isSuccessful) {
                response.body()?.let { orders ->
                    Log.d(TAG, "Orders loaded: ${orders.size} (status: ${status ?: "all"})")
                    Result.success(orders)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "Failed to get orders: ${response.code()} ${response.message()}, Body: $errorBody")
                Result.failure(Exception("HTTP ${response.code()}: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception getting orders: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getPositions(): Result<List<AlpacaPosition>> = withContext(Dispatchers.IO) {
        try {
            if (!isConfigured()) {
                return@withContext Result.failure(Exception("Alpaca API not configured"))
            }
            
            val response = apiClient.tradingApi.getPositions()
            if (response.isSuccessful) {
                response.body()?.let { positions ->
                    Log.d(TAG, "Positions loaded: ${positions.size}")
                    Result.success(positions)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "Failed to get positions: ${response.code()} ${response.message()}, Body: $errorBody")
                Result.failure(Exception("HTTP ${response.code()}: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception getting positions: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getMarketData(symbol: String): Result<MarketData> = withContext(Dispatchers.IO) {
        try {
            if (!isConfigured()) {
                return@withContext Result.failure(Exception("Alpaca API not configured"))
            }
            
            val quotesResponse = apiClient.dataApi.getLatestQuote(symbol)
            val tradesResponse = apiClient.dataApi.getLatestTrade(symbol)
            
            val quote = if (quotesResponse.isSuccessful) {
                quotesResponse.body()?.quote?.toLegacyQuote()
            } else null
            
            val trade = if (tradesResponse.isSuccessful) {
                tradesResponse.body()?.trade?.toLegacyTrade()
            } else null
            
            val marketData = MarketData(
                symbol = symbol,
                quote = quote,
                trade = trade,
                timestamp = System.currentTimeMillis().toString()
            )
            
            Result.success(marketData)
        } catch (e: Exception) {
            Log.e(TAG, "Exception getting market data for $symbol: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun cancelOrder(orderId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!isConfigured()) {
                return@withContext Result.failure(Exception("Alpaca API not configured"))
            }
            
            val response = apiClient.tradingApi.cancelOrder(orderId)
            if (response.isSuccessful) {
                Log.d(TAG, "Order cancelled successfully: $orderId")
                Result.success("Order cancelled successfully")
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "Failed to cancel order: ${response.code()} ${response.message()}, Body: $errorBody")
                Result.failure(Exception("HTTP ${response.code()}: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception cancelling order: ${e.message}", e)
            Result.failure(e)
        }
    }
}