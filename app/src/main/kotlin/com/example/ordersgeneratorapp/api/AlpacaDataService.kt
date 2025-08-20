package com.example.ordersgeneratorapp.api

import retrofit2.Response
import retrofit2.http.*

interface AlpacaDataService {
    @GET("v2/stocks/{symbol}/snapshot")
    suspend fun getSnapshot(@Path("symbol") symbol: String): Response<SnapshotResponse>
    
    @GET("v2/stocks/snapshots")
    suspend fun getSnapshots(@Query("symbols") symbols: String): Response<SnapshotsResponse>
    
    @GET("v2/stocks/{symbol}/quotes/latest")
    suspend fun getLatestQuote(@Path("symbol") symbol: String): Response<SingleQuoteResponse>

    @GET("v2/stocks/{symbol}/trades/latest")
    suspend fun getLatestTrade(@Path("symbol") symbol: String): Response<SingleTradeResponse>

    @GET("v2/stocks/quotes")
    suspend fun getQuotes(@Query("symbols") symbols: String): Response<MultiQuoteResponse>

    @GET("v2/stocks/trades")
    suspend fun getTrades(@Query("symbols") symbols: String): Response<MultiTradeResponse>

    @GET("v2/stocks/{symbol}/quotes")
    suspend fun getHistoricalQuotes(
        @Path("symbol") symbol: String,
        @Query("start") start: String? = null,
        @Query("end") end: String? = null,
        @Query("limit") limit: Int = 1000,
        @Query("page_token") pageToken: String? = null
    ): Response<HistoricalQuotesResponse>

    @GET("v2/stocks/{symbol}/trades")
    suspend fun getHistoricalTrades(
        @Path("symbol") symbol: String,
        @Query("start") start: String? = null,
        @Query("end") end: String? = null,
        @Query("limit") limit: Int = 1000,
        @Query("page_token") pageToken: String? = null
    ): Response<HistoricalTradesResponse>

    @GET("v2/stocks/{symbol}/bars")
    suspend fun getHistoricalBars(
        @Path("symbol") symbol: String,
        @Query("start") start: String,
        @Query("end") end: String,
        @Query("timeframe") timeframe: String = "1Day",
        @Query("limit") limit: Int = 1000,
        @Query("page_token") pageToken: String? = null
    ): Response<HistoricalBarsResponse>

    @GET("v1beta1/news")
    suspend fun getNews(
        @Query("symbols") symbols: String? = null,
        @Query("start") start: String? = null,
        @Query("end") end: String? = null,
        @Query("sort") sort: String = "desc",
        @Query("include_content") includeContent: Boolean = false,
        @Query("exclude_contentless") excludeContentless: Boolean = false,
        @Query("limit") limit: Int = 10,
        @Query("page_token") pageToken: String? = null
    ): Response<NewsResponse>
}

// ✅ API Data models - keep only these, NO duplicates
data class QuoteData(
    val t: String, // timestamp
    val ax: String, // ask exchange
    val ap: Double, // ask price
    val `as`: Int, // ask size
    val bx: String, // bid exchange
    val bp: Double, // bid price
    val bs: Int, // bid size
    val c: List<String>? = null // conditions
)

data class TradeData(
    val t: String, // timestamp
    val x: String, // exchange
    val p: Double, // price
    val s: Int, // size
    val c: List<String>? = null, // conditions
    val i: Long = 0, // trade ID
    val z: String? = null // tape
)

data class MultiQuoteResponse(
    val quotes: Map<String, QuoteData>,
    val next_page_token: String? = null
)

data class SingleQuoteResponse(
    val quote: QuoteData
)

data class MultiTradeResponse(
    val trades: Map<String, TradeData>,
    val next_page_token: String? = null
)

data class SingleTradeResponse(
    val trade: TradeData
)

data class HistoricalQuotesResponse(
    val quotes: List<QuoteData>,
    val symbol: String,
    val next_page_token: String?
)

data class HistoricalTradesResponse(
    val trades: List<TradeData>,
    val symbol: String,
    val next_page_token: String?
)

data class Bar(
    val t: String, // timestamp
    val o: Double, // open
    val h: Double, // high
    val l: Double, // low
    val c: Double, // close
    val v: Long, // volume
    val n: Int?, // trade count
    val vw: Double? // volume weighted average price
)

data class HistoricalBarsResponse(
    val bars: List<Bar>,
    val symbol: String,
    val next_page_token: String?
)

data class Snapshot(
    val latestTrade: TradeData?,
    val latestQuote: QuoteData?,
    val minuteBar: Bar?,
    val dailyBar: Bar?,
    val prevDailyBar: Bar?
)

data class SnapshotResponse(
    val trade: TradeData?,
    val quote: QuoteData?,
    val dailyBar: BarData?,
    val minuteBar: BarData?,
    val prevDailyBar: BarData?
)

data class SnapshotsResponse(
    val snapshots: Map<String, SnapshotResponse>
)

data class BarData(
    val t: String, // timestamp
    val o: Double, // open
    val h: Double, // high
    val l: Double, // low
    val c: Double, // close
    val v: Long,   // volume
    val n: Int     // number of trades
)

data class NewsResponse(
    val news: List<NewsItem>,
    val next_page_token: String?
)

data class NewsItem(
    val id: String,
    val headline: String,
    val author: String,
    val created_at: String,
    val updated_at: String,
    val summary: String,
    val url: String,
    val symbols: List<String>
)

// ✅ NO extension functions here - they are ONLY in MarketData.kt