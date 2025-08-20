package com.example.ordersgeneratorapp.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.ordersgeneratorapp.data.*
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    // Add StateFlow for reactive settings
    private val _appSettings = MutableStateFlow(loadAppSettings())
    val appSettings: Flow<AppSettings> = _appSettings.asStateFlow()
    
    private val _connectionSettings = MutableStateFlow(getConnectionSettings())
    val connectionSettings: Flow<ConnectionSettings> = _connectionSettings.asStateFlow()
    
    companion object {
        private const val KEY_CONNECTION_SETTINGS = "connection_settings"
        private const val KEY_APP_SETTINGS = "app_settings"
    }
    
    fun saveConnectionSettings(settings: ConnectionSettings) {
        val json = gson.toJson(settings)
        prefs.edit().putString(KEY_CONNECTION_SETTINGS, json).apply()
        _connectionSettings.value = settings
    }
    
    fun getConnectionSettings(): ConnectionSettings {
        val json = prefs.getString(KEY_CONNECTION_SETTINGS, null)
        return if (json != null) {
            try {
                val loaded = gson.fromJson(json, ConnectionSettings::class.java)
                migrateConnectionSettings(loaded)
            } catch (e: JsonSyntaxException) {
                ConnectionSettings()
            }
        } else {
            ConnectionSettings()
        }
    }
    
    fun saveAppSettings(settings: AppSettings) {
        val json = gson.toJson(settings)
        prefs.edit().putString(KEY_APP_SETTINGS, json).apply()
        _appSettings.value = settings
    }
    
    fun getAppSettings(): AppSettings {
        val json = prefs.getString(KEY_APP_SETTINGS, null)
        return if (json != null) {
            try {
                gson.fromJson(json, AppSettings::class.java)
            } catch (e: JsonSyntaxException) {
                AppSettings()
            }
        } else {
            AppSettings()
        }
    }
    
    private fun loadAppSettings(): AppSettings {
        return try {
            val json = prefs.getString(KEY_APP_SETTINGS, null)
            if (json != null) {
                gson.fromJson(json, AppSettings::class.java)
            } else {
                // Create default settings with sample hotkey presets
                Log.d("SettingsManager", "Creating default app settings with sample hotkeys")
                AppSettings()
            }
        } catch (e: Exception) {
            Log.e("SettingsManager", "Error loading app settings: ${e.message}", e)
            AppSettings()
        }
    }

    fun clearAllSettings() {
        prefs.edit().clear().apply()
    }

    fun getHotkeyPresets(): List<HotkeyPreset> {
        return getAppSettings().hotkeySettings.presets
    }

    private fun migrateConnectionSettings(cs: ConnectionSettings): ConnectionSettings {
        if (cs.brokerAccounts.isNotEmpty()) return cs
        val mutable = mutableListOf<BrokerAccount>()
        // Legacy Alpaca
        if (cs.alpaca.apiKey.isNotBlank() && cs.alpaca.secretKey.isNotBlank()) {
            mutable += BrokerAccount(
                id = "alpaca-legacy",
                brokerType = "Alpaca",
                accountName = "Alpaca Legacy",
                isEnabled = true,
                alpacaApiKey = cs.alpaca.apiKey,
                alpacaSecretKey = cs.alpaca.secretKey,
                alpacaBaseUrl = cs.alpaca.baseUrl,
                alpacaIsPaper = cs.alpaca.isPaper
            )
        }
        // Legacy IBKR
        if (cs.ibkr.host.isNotBlank() && cs.ibkr.port.isNotBlank()) {
            mutable += BrokerAccount(
                id = "ibkr-legacy",
                brokerType = "IBKR",
                accountName = "IBKR Legacy",
                isEnabled = true,
                ibkrHost = cs.ibkr.host,
                ibkrPort = cs.ibkr.port,
                ibkrClientId = cs.ibkr.clientId,
                ibkrAccountId = cs.ibkr.accountId,
                ibkrGatewayBaseUrl = cs.ibkr.baseUrl,
                ibkrApiKey = cs.ibkr.apiKey
            )
        }
        return if (mutable.isEmpty()) cs else cs.copy(brokerAccounts = mutable)
    }
}