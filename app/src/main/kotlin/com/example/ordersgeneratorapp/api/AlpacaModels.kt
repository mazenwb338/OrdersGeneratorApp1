// filepath: /Users/mazenbteddini/OrdersGeneratorApp/app/src/main/kotlin/com/example/ordersgeneratorapp/api/AlpacaModels.kt
package com.example.ordersgeneratorapp.api

import com.google.gson.annotations.SerializedName

// Account models
data class AlpacaAccount(
    val id: String,
    @SerializedName("account_number")
    val accountNumber: String,
    val status: String,
    val currency: String,
    @SerializedName("buying_power")
    val buyingPower: String,
    val regtBuyingPower: String,
    val daytradingBuyingPower: String,
    val cash: String,
    @SerializedName("portfolio_value")
    val portfolioValue: String,
    @SerializedName("equity")
    val equity: String,
    @SerializedName("last_equity")
    val lastEquity: String,
    val multiplier: String,
    val initialMargin: String,
    val maintenanceMargin: String,
    val sma: String,
    val daytradeCount: Int,
    val shortoableCash: String? = null,
    val createdAt: String
)

// Order models
data class AlpacaOrder(
    val id: String,
    val clientOrderId: String,
    @SerializedName("created_at")
    val createdAt: String,
    val updatedAt: String,
    val submittedAt: String,
    @SerializedName("filled_at")
    val filledAt: String?,
    val expiredAt: String?,
    val canceledAt: String?,
    val failedAt: String?,
    val replacedAt: String?,
    val replacedBy: String?,
    val replaces: String?,
    val assetId: String,
    val symbol: String,
    val assetClass: String,
    val notional: String?,
    val qty: String,
    val filledQty: String?,
    val filledAvgPrice: String?,
    val orderClass: String,
    @SerializedName("order_type")
    val orderType: String,
    val type: String,
    val side: String,
    @SerializedName("time_in_force")
    val timeInForce: String,
    @SerializedName("limit_price")
    val limitPrice: String?,
    @SerializedName("stop_price")
    val stopPrice: String?,
    val status: String,
    val extendedHours: Boolean,
    val legs: List<AlpacaOrder>?,
    val trailPercent: String?,
    val trailPrice: String?,
    val hwm: String?,
    val commission: String?
)

data class CreateOrderRequest(
    val symbol: String,
    val qty: String,
    val side: String,
    val type: String,
    @SerializedName("time_in_force")
    val timeInForce: String,
    @SerializedName("limit_price")
    val limitPrice: String? = null,
    @SerializedName("stop_price")
    val stopPrice: String? = null,
    val clientOrderId: String? = null,
    val extendedHours: Boolean? = null,
    val orderClass: String? = null,
    val takeProfitLimitPrice: String? = null,
    val stopLossStopPrice: String? = null,
    val stopLossLimitPrice: String? = null,
    val trailPrice: String? = null,
    val trailPercent: String? = null
)

// Position models
data class AlpacaPosition(
    val assetId: String,
    val symbol: String,
    val exchange: String,
    val assetClass: String,
    @SerializedName("avg_entry_price")
    val avgEntryPrice: String,
    val qty: String,
    val side: String,
    @SerializedName("market_value")
    val marketValue: String,
    val costBasis: String,
    @SerializedName("unrealized_pl")
    val unrealizedPl: String,
    @SerializedName("unrealized_plpc")
    val unrealizedPlpc: String,
    val unrealizedIntradayPl: String,
    val unrealizedIntradayPlpc: String,
    val currentPrice: String,
    val lastdayPrice: String,
    val changeToday: String,
    val swapRate: String? = null,
    val avgEntrySwapRate: String? = null,
    val usdValue: String? = null,
    val qtdPl: String? = null
)

// Market data models - using different names to avoid conflicts
data class AlpacaQuote(
    @SerializedName("t") val timestamp: String,
    @SerializedName("ax") val askExchange: String,
    @SerializedName("ap") val askPrice: Double,
    @SerializedName("as") val askSize: Int,
    @SerializedName("bx") val bidExchange: String,
    @SerializedName("bp") val bidPrice: Double,
    @SerializedName("bs") val bidSize: Int,
    @SerializedName("c") val conditions: List<String>?
)

data class AlpacaTrade(
    @SerializedName("t") val timestamp: String,
    @SerializedName("x") val exchange: String,
    @SerializedName("p") val price: Double,
    @SerializedName("s") val size: Int,
    @SerializedName("c") val conditions: List<String>?,
    @SerializedName("i") val id: Long,
    @SerializedName("z") val tape: String
)

data class AlpacaMultiQuoteResponse(
    @SerializedName("quotes") val quotes: Map<String, AlpacaQuote>,
    @SerializedName("next_page_token") val nextPageToken: String?
)

data class AlpacaMultiTradeResponse(
    @SerializedName("trades") val trades: Map<String, AlpacaTrade>,
    @SerializedName("next_page_token") val nextPageToken: String?
)

data class AlpacaSingleQuoteResponse(
    @SerializedName("quote") val quote: AlpacaQuote,
    @SerializedName("symbol") val symbol: String
)

data class AlpacaSingleTradeResponse(
    @SerializedName("trade") val trade: AlpacaTrade,
    @SerializedName("symbol") val symbol: String
)

data class AlpacaErrorResponse(
    val code: Int,
    val message: String
)