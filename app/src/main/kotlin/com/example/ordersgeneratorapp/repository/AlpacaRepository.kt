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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

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

    // Safe accessor (build fallback if apiClient not yet updated)
    private var localDataApi: AlpacaDataService? = null
    private fun dataApiOrNull(): AlpacaDataService? {
        // First try the shared client if credentials registered
        val shared = runCatching { apiClient.dataApi }.getOrNull()
        if (shared != null) return shared

        // Fallback: build once from locally stored creds
        if (localDataApi == null && currentApiKey.isNotBlank() && currentSecretKey.isNotBlank()) {
            Log.d(TAG, "Building fallback localDataApi (apiClient not yet configured)")
            localDataApi = buildDataService(currentApiKey, currentSecretKey)
        }
        return localDataApi
    }

    private fun buildDataService(apiKey: String, secretKey: String): AlpacaDataService {
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
            .baseUrl("https://data.alpaca.markets/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(http)
            .build()
        return retrofit.create(AlpacaDataService::class.java)
    }

    private var currentApiKey: String = ""
    private var currentSecretKey: String = ""
    private var currentBaseUrl: String = "https://paper-api.alpaca.markets/"
    private var retrofit: Retrofit? = null
    private var tradingApi: AlpacaApiService? = null
    private val clientLock = Any()

    @Volatile private var lastConfiguredAccountId: String? = null

    @Volatile private var autoConfigured = false
    private fun configureIfNeeded(): Boolean {
        if (isConfigured()) return true
        if (autoConfigured) return isConfigured()
        autoConfigured = true
        return try {
            val cs: ConnectionSettings = settingsManager.getConnectionSettings()
            val acct = cs.brokerAccounts.firstOrNull {
                it.isEnabled &&
                    it.brokerType == "Alpaca" &&
                    it.alpacaApiKey.isNotBlank() &&
                    it.alpacaSecretKey.isNotBlank()
            }
            if (acct != null) {
                Log.d(TAG, "Auto-config (broker account) id=${acct.id}")
                configureFromBrokerAccount(acct)
                return isConfigured()
            }
            if (cs.alpaca.apiKey.isNotBlank() && cs.alpaca.secretKey.isNotBlank()) {
                Log.d(TAG, "Auto-config (legacy settings)")
                configureCredentials(
                    apiKey = cs.alpaca.apiKey,
                    secretKey = cs.alpaca.secretKey,
                    baseUrl = cs.alpaca.baseUrl.ifBlank { "https://paper-api.alpaca.markets/" }
                )
                return isConfigured()
            }
            Log.w(TAG, "Auto-config failed: no credentials found")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Auto-config exception: ${e.message}", e)
            false
        }
    }

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

    fun isConfigured(): Boolean =
        apiClient.isConfigured() || (currentApiKey.isNotBlank() && currentSecretKey.isNotBlank())

    fun configureCredentials(apiKey: String, secretKey: String, baseUrl: String) {
        synchronized(clientLock) {
            currentApiKey = apiKey
            currentSecretKey = secretKey
            currentBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val isPaper = currentBaseUrl.contains("paper")
            // Sync shared apiClient so dataApi works
            apiClient.updateSettings(
                AlpacaSettings(
                    apiKey = apiKey,
                    secretKey = secretKey,
                    baseUrl = currentBaseUrl,
                    isPaper = isPaper
                )
            )
            Log.d(TAG, "Configured credentials (isPaper=$isPaper) base=$currentBaseUrl keyPrefix=${apiKey.take(6)}")
            buildClients()
            // Reset fallback so future calls use shared client
            localDataApi = null
        }
    }

    fun configureFromBrokerAccount(acct: BrokerAccount) {
        if (acct.brokerType != "Alpaca") return
        val base = (acct.alpacaBaseUrl.ifBlank {
            if (acct.alpacaIsPaper) "https://paper-api.alpaca.markets" else "https://api.alpaca.markets"
        }).let { if (it.endsWith("/")) it else "$it/" }
        configureCredentials(
            apiKey = acct.alpacaApiKey,
            secretKey = acct.alpacaSecretKey,
            baseUrl = base
        )
        // Also push settings to apiClient explicitly (already done in configureCredentials, keeps log clarity)
        apiClient.updateSettings(
            AlpacaSettings(
                apiKey = acct.alpacaApiKey,
                secretKey = acct.alpacaSecretKey,
                baseUrl = base,
                isPaper = acct.alpacaIsPaper || base.contains("paper")
            )
        )
        lastConfiguredAccountId = acct.id
        Log.d(TAG, "Configured Alpaca acct=${acct.accountName} id=${acct.id} base=$base key=${acct.alpacaApiKey.take(4)}… (data client ready)")
        localDataApi = null
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

    // Order event stream (new / cancel / replace)
    private val _orderEvents = MutableSharedFlow<OrderEvent>(extraBufferCapacity = 32)
    val orderEvents: SharedFlow<OrderEvent> = _orderEvents

    sealed class OrderEvent {
        data class Created(val order: AlpacaOrder) : OrderEvent()
        data class Canceled(val orderId: String, val success: Boolean) : OrderEvent()
        data class Updated(val order: AlpacaOrder) : OrderEvent()
    }

    // Emit helper
    private fun emitOrderCreated(order: AlpacaOrder) {
        _orderEvents.tryEmit(OrderEvent.Created(order))
    }
    private fun emitOrderCanceled(orderId: String, ok: Boolean) {
        _orderEvents.tryEmit(OrderEvent.Canceled(orderId, ok))
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
                    emitOrderCreated(order)
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

    // Single canonical market data fetch
    suspend fun getMarketData(symbol: String): Result<MarketData> = withContext(Dispatchers.IO) {
        try {
            if (!configureIfNeeded()) {
                return@withContext Result.failure(Exception("Alpaca API not configured"))
            }
            val dataApi = dataApiOrNull()
                ?: return@withContext Result.failure(Exception("Alpaca data API unavailable"))

            Log.d(TAG, "Fetching market data for $symbol")

            val quoteResp = dataApi.getLatestQuote(symbol)
            val tradeResp = dataApi.getLatestTrade(symbol)

            val legacyQuote = if (quoteResp.isSuccessful) quoteResp.body()?.quote?.toLegacyQuote() else null
            val legacyTrade = if (tradeResp.isSuccessful) tradeResp.body()?.trade?.toLegacyTrade() else null

            if (legacyQuote == null && legacyTrade == null) {
                return@withContext Result.failure(
                    IllegalStateException(
                        "Empty quote/trade data for $symbol (HTTP ${quoteResp.code()}/${tradeResp.code()})"
                    )
                )
            }

            Result.success(
                MarketData(
                    symbol = symbol,
                    quote = legacyQuote,
                    trade = legacyTrade,
                    timestamp = System.currentTimeMillis().toString()
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception getting market data for $symbol: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Backwards compatibility wrapper
    suspend fun getLatestQuote(symbol: String): Result<MarketData> = getMarketData(symbol)

    // Batch fetch (single implementation)
    suspend fun getLatestQuotes(symbols: List<String>): Map<String, Result<MarketData>> = withContext(Dispatchers.IO) {
        if (!configureIfNeeded()) {
            return@withContext symbols.associateWith {
                Result.failure(IllegalStateException("Alpaca not configured"))
            }
        }
        val dataApi = dataApiOrNull() ?: return@withContext symbols.associateWith {
            Result.failure(IllegalStateException("Data API unavailable"))
        }

        val resultMap = mutableMapOf<String, Result<MarketData>>()
        for (s in symbols) {
            try {
                val qResp = dataApi.getLatestQuote(s)
                val tResp = dataApi.getLatestTrade(s)
                val legacyQuote = if (qResp.isSuccessful) qResp.body()?.quote?.toLegacyQuote() else null
                val legacyTrade = if (tResp.isSuccessful) tResp.body()?.trade?.toLegacyTrade() else null
                if (legacyQuote == null && legacyTrade == null) {
                    resultMap[s] = Result.failure(IllegalStateException("No data"))
                } else {
                    resultMap[s] = Result.success(
                        MarketData(
                            symbol = s,
                            quote = legacyQuote,
                            trade = legacyTrade,
                            timestamp = System.currentTimeMillis().toString()
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Batch quote error for $s: ${e.message}")
                resultMap[s] = Result.failure(e)
            }
        }
        resultMap
    }

    // Provide cancelOrder expected by OrderHistoryScreen
    suspend fun cancelOrder(orderId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (!isConfigured()) return@withContext Result.failure(Exception("Alpaca API not configured"))
            val api = tradingApi ?: return@withContext Result.failure(Exception("Trading API not ready"))
            Log.d(TAG, "Cancelling order $orderId")
            val resp = api.cancelOrder(orderId)
            val ok = resp.isSuccessful
            emitOrderCanceled(orderId, ok)
            if (ok) Result.success(true)
            else {
                val err = resp.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "Cancel failed $orderId HTTP ${resp.code()} $err")
                Result.failure(Exception("Cancel failed: HTTP ${resp.code()} $err"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Cancel order exception $orderId: ${e.message}", e)
            Result.failure(e)
        }
    }

    // --- Candles / Bars support (add) ---
    suspend fun getRecentBars(
        symbol: String,
        timeframe: String = "1Min",
        limit: Int = 100
    ): Result<List<CandleBar>> = withContext(Dispatchers.IO) {
        try {
            if (!configureIfNeeded()) return@withContext Result.failure(Exception("Not configured"))
            val dataApi = dataApiOrNull() ?: return@withContext Result.failure(Exception("Data API unavailable"))
            val resp = dataApi.getBars(symbol, timeframe = timeframe, limit = limit)
            if (resp.isSuccessful) {
                val bars = resp.body()?.bars ?: emptyList()
                Result.success(bars.map {
                    CandleBar(
                        timestamp = it.t,
                        open = it.o,
                        high = it.h,
                        low = it.l,
                        close = it.c,
                        volume = it.v
                    )
                })
            } else {
                Result.failure(Exception("HTTP ${resp.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// Data model for chart (app layer)
data class CandleBar(
    val timestamp: String,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long
)