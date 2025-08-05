package com.example.ordersgeneratorapp.api

import com.example.ordersgeneratorapp.data.AlpacaSettings
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object AlpacaApiClient {
    
    private const val PAPER_BASE_URL = "https://paper-api.alpaca.markets/"
    private const val LIVE_BASE_URL = "https://api.alpaca.markets/"
    
    private var currentSettings: AlpacaSettings? = null
    private var _tradingApi: AlpacaApiService? = null
    private var _dataApi: AlpacaDataService? = null
    
    fun updateSettings(settings: AlpacaSettings) {
        currentSettings = settings
        // Reset APIs to force recreation with new settings
        _tradingApi = null
        _dataApi = null
    }
    
    private fun createAuthInterceptor(settings: AlpacaSettings): Interceptor {
        return Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("APCA-API-KEY-ID", settings.apiKey)
                .addHeader("APCA-API-SECRET-KEY", settings.secretKey)
                .addHeader("Content-Type", "application/json")
                .build()
            chain.proceed(request)
        }
    }
    
    private fun createOkHttpClient(settings: AlpacaSettings): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(createAuthInterceptor(settings))
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    private fun createRetrofit(baseUrl: String, settings: AlpacaSettings): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(createOkHttpClient(settings))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    val tradingApi: AlpacaApiService
        get() {
            val settings = currentSettings ?: throw IllegalStateException("Alpaca settings not configured")
            
            if (_tradingApi == null) {
                val baseUrl = if (settings.isPaper) PAPER_BASE_URL else LIVE_BASE_URL
                _tradingApi = createRetrofit(baseUrl, settings).create(AlpacaApiService::class.java)
            }
            return _tradingApi!!
        }
    
    val dataApi: AlpacaDataService
        get() {
            val settings = currentSettings ?: throw IllegalStateException("Alpaca settings not configured")
            
            if (_dataApi == null) {
                val dataUrl = "https://data.alpaca.markets/"
                _dataApi = createRetrofit(dataUrl, settings).create(AlpacaDataService::class.java)
            }
            return _dataApi!!
        }
    
    fun isConfigured(): Boolean = currentSettings != null && 
                                 currentSettings!!.apiKey.isNotEmpty() && 
                                 currentSettings!!.secretKey.isNotEmpty()
    
    fun getBaseUrl(): String {
        return if (currentSettings?.isPaper == true) PAPER_BASE_URL else LIVE_BASE_URL
    }
    
    fun isPaperTrading(): Boolean {
        return currentSettings?.isPaper ?: true
    }
    
    fun getCurrentSettings(): AlpacaSettings? {
        return currentSettings
    }
}