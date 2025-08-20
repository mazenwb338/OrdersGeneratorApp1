package com.example.ordersgeneratorapp.data

data class AppSettings(
    val connectionSettings: ConnectionSettings = ConnectionSettings(),
    val hotkeySettings: HotkeySettings = HotkeySettings()
)

data class HotkeySettings(
    val presets: List<HotkeyPreset> = emptyList()
)

data class HotkeyPreset(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    val symbol: String = "",
    val quantity: String = "",
    val orderType: String = "market",
    val timeInForce: String = "day",
    val limitPrice: String = "",
    val stopPrice: String = "",
    val selectedBrokerIds: List<String> = emptyList(),
    val isEnabled: Boolean = true,
    val position: Int = 0,
    val buyHotkey: HotkeyConfig = HotkeyConfig(),
    val sellHotkey: HotkeyConfig = HotkeyConfig()
)

data class HotkeyConfig(
    val key: String = "",
    val ctrl: Boolean = false,
    val alt: Boolean = false,
    val shift: Boolean = false
) {
    val description: String
        get() = buildString {
            if (ctrl) append("Ctrl+")
            if (alt) append("Alt+")
            if (shift) append("Shift+")
            append(key.ifEmpty { "?" })
        }
}

data class BrokerAccount(
    val id: String = "",
    val brokerType: String = "", // "Alpaca" or "IBKR"
    val accountName: String = "", // User-friendly name like "Alpaca Main", "IBKR Retirement"
    val isEnabled: Boolean = true,
    
    // Alpaca specific
    val alpacaApiKey: String = "",
    val alpacaSecretKey: String = "",
    val alpacaBaseUrl: String = "https://paper-api.alpaca.markets",
    val alpacaIsPaper: Boolean = true,
    
    // IBKR specific  
    val ibkrHost: String = "127.0.0.1",
    val ibkrPort: String = "7497",
    val ibkrClientId: String = "1",
    val ibkrAccountId: String = "",
    val ibkrGatewayBaseUrl: String = "",
    val ibkrApiKey: String = "" // optional if you wrap custom gateway auth
)