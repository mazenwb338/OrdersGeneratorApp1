package com.example.ordersgeneratorapp.repository

import android.util.Log
import com.example.ordersgeneratorapp.api.*
import com.example.ordersgeneratorapp.data.ConnectionSettings
import com.example.ordersgeneratorapp.data.BrokerAccount
import com.example.ordersgeneratorapp.util.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Interceptor
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.ordersgeneratorapp.data.AlpacaSettings
import java.util.Collections
import java.util.LinkedHashMap
import java.util.concurrent.ConcurrentHashMap

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

    private val alpacaDataService: AlpacaDataService 
        get() = apiClient.dataApi

    private var currentApiKey: String = ""
    private var currentSecretKey: String = ""
    private var currentBaseUrl: String = "https://paper-api.alpaca.markets/"
    private var retrofit: Retrofit? = null
    private var tradingApi: AlpacaApiService? = null
    private val clientLock = Any()

    @Volatile private var lastConfiguredAccountId: String? = null

    private val hotkeyDedupWindowMs = 2000L
    private val recentHotkeyOrders = Collections.synchronizedMap(
        object : LinkedHashMap<String, Long>(64, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?): Boolean {
                return size > 128
            }
        }
    )

    private val hotkeyPreflightWindowMs = 800L
    private val recentHotkeyKeys = Collections.synchronizedMap(
        object : LinkedHashMap<String, Long>(128, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?): Boolean = size > 256
        }
    )

    private val acctOrderWindowMs = 800L

    private data class ClientBundle(
        val apiKey: String,
        val secretKey: String,
        val baseUrl: String,
        val service: AlpacaApiService
    )

    private val accountClients = ConcurrentHashMap<String, ClientBundle>()

    private fun buildService(apiKey: String, secretKey: String, baseUrl: String): AlpacaApiService {
        val auth = Interceptor { chain ->
            val req = chain.request().newBuilder()
                .addHeader("APCA-API-KEY-ID", apiKey)
                .addHeader("APCA-API-SECRET-KEY", secretKey)
                .addHeader("Content-Type", "application/json")
                .build()
            chain.proceed(req)
        }
        val http = OkHttpClient.Builder()
            .addInterceptor(auth)
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(http)
            .build()
        return retrofit.create(AlpacaApiService::class.java)
    }

    private fun getServiceForAccount(acct: BrokerAccount): AlpacaApiService {
        val base = (acct.alpacaBaseUrl.ifBlank {
            if (acct.alpacaIsPaper) "https://paper-api.alpaca.markets" else "https://api.alpaca.markets"
        })
        val existing = accountClients[acct.id]
        if (existing != null &&
            existing.apiKey == acct.alpacaApiKey &&
            existing.secretKey == acct.alpacaSecretKey &&
            existing.baseUrl == base
        ) return existing.service

        val service = buildService(acct.alpacaApiKey, acct.alpacaSecretKey, base)
        accountClients[acct.id] = ClientBundle(acct.alpacaApiKey, acct.alpacaSecretKey, base, service)
        Log.d(TAG, "Built / cached client for acct=${acct.accountName} id=${acct.id} key=${acct.alpacaApiKey.take(6)}")
        return service
    }

    fun isConfigured(): Boolean = currentApiKey.isNotBlank() && currentSecretKey.isNotBlank()

    fun configureCredentials(apiKey: String, secretKey: String, baseUrl: String) {
        synchronized(clientLock) {
            currentApiKey = apiKey
            currentSecretKey = secretKey
            currentBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            buildClients()
        }
    }

    fun configureFromBrokerAccount(acct: BrokerAccount) {
        if (acct.brokerType != "Alpaca") return
        val base = (acct.alpacaBaseUrl.ifBlank { if (acct.alpacaIsPaper) "https://paper-api.alpaca.markets" else "https://api.alpaca.markets" })
            .let { if (it.endsWith("/")) it else "$it/" }
        configureCredentials(
            apiKey = acct.alpacaApiKey,
            secretKey = acct.alpacaSecretKey,
            baseUrl = base
        )
        lastConfiguredAccountId = acct.id
        Log.d(TAG, "Configured Alpaca acct=${acct.accountName} (${acct.id}) base=$base key=${acct.alpacaApiKey.take(4)}…")
        Log.d(TAG, "AcctSwitch id=${acct.id} name=${acct.accountName} apiKeyPrefix=${acct.alpacaApiKey.take(8)}")
    }

    fun ensureConfiguredForFirstEnabledAccount(connectionSettings: ConnectionSettings) {
        if (isConfigured()) return
        val first = connectionSettings.brokerAccounts.firstOrNull {
            it.isEnabled && it.brokerType == "Alpaca" &&
                it.alpacaApiKey.isNotBlank() && it.alpacaSecretKey.isNotBlank()
        } ?: return
        configureFromBrokerAccount(first)
        lastConfiguredAccountId = first.id
        Log.d(TAG, "Primed repository with first enabled Alpaca account ${first.accountName}")
    }

    private fun buildClients() {
        val authInterceptor = Interceptor { chain ->
            val req = chain.request().newBuilder()
                .addHeader("APCA-API-KEY-ID", currentApiKey)
                .addHeader("APCA-API-SECRET-KEY", currentSecretKey)
                .addHeader("Content-Type", "application/json")
                .build()
            chain.proceed(req)
        }
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val http = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()

        retrofit = Retrofit.Builder()
            .baseUrl(currentBaseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .client(http)
            .build()

        tradingApi = retrofit!!.create(AlpacaApiService::class.java)
    }

    suspend fun testCredentials(
        apiKey: String,
        secretKey: String,
        isPaper: Boolean
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (apiKey.isBlank() || secretKey.isBlank()) {
                return@withContext Result.failure(Exception("Missing API or Secret Key"))
            }

            val settings = AlpacaSettings(
                apiKey = apiKey,
                secretKey = secretKey,
                isPaper = isPaper
            )

            val authInterceptor = Interceptor { chain ->
                val req = chain.request().newBuilder()
                    .addHeader("APCA-API-KEY-ID", settings.apiKey)
                    .addHeader("APCA-API-SECRET-KEY", settings.secretKey)
                    .addHeader("Content-Type", "application/json")
                    .build()
                chain.proceed(req)
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(
                    HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
                )
                .build()

            val baseUrl = if (settings.isPaper) "https://paper-api.alpaca.markets/" else "https://api.alpaca.markets/"

            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val svc = retrofit.create(AlpacaApiService::class.java)
            val resp = svc.getAccount()
            if (resp.isSuccessful) {
                val acct = resp.body()
                Result.success("✅ ${acct?.accountNumber ?: "OK"}")
            } else {
                val err = resp.errorBody()?.string() ?: "Unknown error"
                Result.failure(Exception("HTTP ${resp.code()} $err"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "testCredentials failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun updateSettings(alpacaSettings: AlpacaSettings) {
        apiClient.updateSettings(alpacaSettings)
    }

    suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!isConfigured()) {
                return@withContext Result.failure(Exception("Alpaca API not configured"))
            }
            
            val response = tradingApi!!.getAccount()
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
            
            val response = tradingApi!!.getOrders(limit = 1)
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
            
            val response = tradingApi!!.getAccount()
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

    public suspend fun createOrder(
        symbol: String,
        quantity: Int,
        side: String,
        orderType: String = "market",
        timeInForce: String = "day",
        limitPrice: String? = null,
        stopPrice: String? = null,
        clientOrderId: String? = null
    ): Result<AlpacaOrder> = withContext(Dispatchers.IO) {
        try {
            if (!isConfigured()) {
                return@withContext Result.failure(Exception("Alpaca API not configured"))
            }

            Log.d(TAG, "API_CALL symbol=$symbol side=$side qty=$quantity clientOrderId=$clientOrderId")

            val request = CreateOrderRequest(
                symbol = symbol,
                qty = quantity.toString(),
                side = side,
                type = orderType,
                timeInForce = timeInForce.lowercase(),
                limitPrice = limitPrice,
                stopPrice = stopPrice,
                clientOrderId = clientOrderId
            )
            
            val response = tradingApi!!.createOrder(request)
            if (response.isSuccessful) {
                response.body()?.let { order ->
                    Log.d(TAG, "API_SUCCESS orderId=${order.id} clientOrderId=${order.clientOrderId}")
                    Result.success(order)
                } ?: Result.failure(Exception("Empty response"))
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "API_FAIL code=${response.code()} error=$errorBody")
                Result.failure(Exception("HTTP ${response.code()}: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "API_EXCEPTION", e)
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
            
            val response = tradingApi!!.getOrders(
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
            
            val response = tradingApi!!.getPositions()
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
            
            val quotesResponse = alpacaDataService.getLatestQuote(symbol)
            val tradesResponse = alpacaDataService.getLatestTrade(symbol)
            
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
            Log.d(TAG, "Canceling order: $orderId")
            
            val response = tradingApi!!.cancelOrder(orderId)
            
            if (response.isSuccessful) {
                Log.d(TAG, "Order canceled successfully: $orderId")
                Result.success("Order canceled")
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "Failed to cancel order: ${response.code()} ${response.message()}, Body: $errorBody")
                Result.failure(Exception("HTTP ${response.code()}: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception canceling order: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getLatestQuote(symbol: String): Result<MarketData> {
        return try {
            Log.d("AlpacaRepository", "Getting latest quote for $symbol")
            
            val response = alpacaDataService.getSnapshot(symbol)
            
            if (response.isSuccessful && response.body() != null) {
                val snapshotResponse = response.body()!!
                Log.d("AlpacaRepository", "Latest quote loaded for $symbol")
                
                // ✅ FIX: Handle ask=0.0 issue with realistic fallbacks
                val rawBidPrice = snapshotResponse.quote?.bp ?: 0.0
                val rawAskPrice = snapshotResponse.quote?.ap ?: 0.0
                val currentPrice = snapshotResponse.trade?.p ?: rawBidPrice
                
                val bidPrice = if (rawBidPrice > 0.0) rawBidPrice else currentPrice * 0.9995
                val askPrice = when {
                    rawAskPrice > 0.0 -> rawAskPrice
                    rawBidPrice > 0.0 -> rawBidPrice + (currentPrice * 0.001)
                    else -> currentPrice * 1.0005
                }
                
                Log.d(TAG, "✅ FIXED_ASK_PRICE: $symbol bid=$bidPrice ask=$askPrice (raw_ask=$rawAskPrice)")
                
                val marketData = MarketData(
                    symbol = symbol,
                    quote = Quote(
                        bidPrice = bidPrice.toString(),
                        bidSize = snapshotResponse.quote?.bs?.toString() ?: "0",
                        askPrice = askPrice.toString(),
                        askSize = snapshotResponse.quote?.`as`?.toString() ?: "0",
                        timestamp = snapshotResponse.quote?.t ?: ""
                    ),
                    trade = snapshotResponse.trade?.toLegacyTrade(),
                    timestamp = System.currentTimeMillis().toString()
                )
                
                Result.success(marketData)
            } else {
                Log.e(TAG, "Failed to get latest quote for $symbol: ${response.message()}")
                Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception getting latest quote for $symbol: ${e.message}", e)
            Result.failure(e)
        }
    }
}