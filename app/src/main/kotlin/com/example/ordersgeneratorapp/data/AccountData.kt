package com.example.ordersgeneratorapp.data

data class AccountBalance(
    val totalValue: Double = 0.0,
    val cashBalance: Double = 0.0,
    val buyingPower: Double = 0.0,
    val dayChange: Double = 0.0,
    val dayChangePercent: Double = 0.0
)

data class Position(
    val symbol: String,
    val quantity: Int,
    val averagePrice: Double,
    val currentPrice: Double,
    val unrealizedPL: Double,
    val unrealizedPLPercent: Double,
    val marketValue: Double
)

data class AccountStatus(
    val accountId: String,
    val accountType: String, // "Alpaca" or "IBKR"
    val isConnected: Boolean,
    val balance: AccountBalance,
    val positions: List<Position> = emptyList(),
    val lastUpdated: Long = System.currentTimeMillis()
)

data class DashboardData(
    val alpacaAccount: AccountStatus? = null,
    val ibkrAccount: AccountStatus? = null,
    val isLoading: Boolean = false
)