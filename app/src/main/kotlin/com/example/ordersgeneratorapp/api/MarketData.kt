package com.example.ordersgeneratorapp.api

data class MarketData(
    val symbol: String,
    val quote: Quote?,
    val trade: Trade?,
    val timestamp: String
)

data class Quote(
    val bidPrice: String,
    val bidSize: String,
    val askPrice: String,
    val askSize: String,
    val timestamp: String
)

data class Trade(
    val price: String,
    val size: String,
    val timestamp: String
)

// Response wrappers
data class QuoteResponse(
    val quote: Quote
)

data class TradeResponse(
    val trade: Trade
)

data class QuotesResponse(
    val quotes: Map<String, Quote>
)

data class TradesResponse(
    val trades: Map<String, Trade>
)

// ✅ Extension functions to convert new API responses to legacy format (ONLY HERE!)
fun QuoteData.toLegacyQuote(): Quote = Quote(
    bidPrice = this.bp.toString(),
    bidSize = this.bs.toString(),
    askPrice = this.ap.toString(),
    askSize = this.`as`.toString(),
    timestamp = this.t
)

fun TradeData.toLegacyTrade(): Trade = Trade(
    price = this.p.toString(),
    size = this.s.toString(),
    timestamp = this.t
)