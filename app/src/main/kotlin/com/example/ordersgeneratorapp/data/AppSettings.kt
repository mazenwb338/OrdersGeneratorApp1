package com.example.ordersgeneratorapp.data

data class AppSettings(
    val connectionSettings: ConnectionSettings = ConnectionSettings(),
    val hotkeySettings: HotkeySettings = HotkeySettings()
)

data class HotkeySettings(
    val hotkeyPresets: List<HotkeyPreset> = emptyList()
)

data class HotkeyPreset(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val symbol: String,
    val quantity: String,
    val orderType: String = "market",
    val timeInForce: String = "day",
    val limitPrice: String = "",
    val stopPrice: String = ""
)

// Remove the AlpacaSettings class from here - it's already defined in ConnectionSettings.kt