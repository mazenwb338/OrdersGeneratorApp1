package com.example.ordersgeneratorapp.data

data class AlpacaSettings(
    val apiKey: String = "",
    val secretKey: String = "",
    val baseUrl: String = "https://paper-api.alpaca.markets",
    val isPaper: Boolean = true
)

data class IBKRSettings(
    val host: String = "127.0.0.1",
    val port: String = "7497",
    val clientId: String = "1",
    val isPaper: Boolean = true
)

data class ConnectionSettings(
    val alpaca: AlpacaSettings = AlpacaSettings(),
    val ibkr: IBKRSettings = IBKRSettings()
)