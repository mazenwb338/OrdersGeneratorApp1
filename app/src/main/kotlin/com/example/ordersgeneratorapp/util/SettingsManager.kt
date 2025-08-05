package com.example.ordersgeneratorapp.util

import android.content.Context
import android.content.SharedPreferences
import com.example.ordersgeneratorapp.data.*
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

class SettingsManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    companion object {
        private const val KEY_CONNECTION_SETTINGS = "connection_settings"
        private const val KEY_APP_SETTINGS = "app_settings"
    }
    
    fun saveConnectionSettings(settings: ConnectionSettings) {
        val json = gson.toJson(settings)
        prefs.edit().putString(KEY_CONNECTION_SETTINGS, json).apply()
    }
    
    fun getConnectionSettings(): ConnectionSettings {
        val json = prefs.getString(KEY_CONNECTION_SETTINGS, null)
        return if (json != null) {
            try {
                gson.fromJson(json, ConnectionSettings::class.java)
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
    
    fun clearAllSettings() {
        prefs.edit().clear().apply()
    }
}