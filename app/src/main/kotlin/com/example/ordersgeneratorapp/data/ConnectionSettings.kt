package com.example.ordersgeneratorapp.data

// Keep existing AlpacaSettings / IbkrSettings ONLY for backwards compatibility (legacy)
// Mark them deprecated (optional)
@Deprecated("Use BrokerAccount list instead")
data class AlpacaSettings(
    val apiKey: String = "",
    val secretKey: String = "",
    val baseUrl: String = "https://paper-api.alpaca.markets",
    val isPaper: Boolean = true
)

@Deprecated("Use BrokerAccount list instead")
data class IbkrSettings(
    val apiKey: String = "",
    val baseUrl: String = "https://example.ibkr.com",
    val host: String = "127.0.0.1",
    val port: String = "7497",
    val clientId: String = "1",
    val accountId: String = ""
)

// Unified connection settings now driven by brokerAccounts
data class ConnectionSettings(
    val brokerAccounts: List<BrokerAccount> = emptyList(),
    val selectedBrokerIds: List<String> = emptyList(),

    // Legacy (kept so old JSON still deserializes; not used going forward)
    @Deprecated("Legacy single account fields") val alpaca: AlpacaSettings = AlpacaSettings(),
    @Deprecated("Legacy single account fields") val ibkr: IbkrSettings = IbkrSettings()
)