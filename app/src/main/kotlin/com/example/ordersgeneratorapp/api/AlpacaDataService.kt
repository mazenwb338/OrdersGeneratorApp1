package com.example.ordersgeneratorapp.api

import retrofit2.Response
import retrofit2.http.*

interface AlpacaDataService {
    
    // Latest quote endpoints
    @GET("v2/stocks/{symbol}/quotes/latest")
    suspend fun getLatestQuote(@Path("symbol") symbol: String): Response<SingleQuoteResponse>
    
    @GET("v2/stocks/quotes/latest")
    suspend fun getLatestQuotes(@Query("symbols") symbols: String): Response<MultiQuoteResponse>
    
    // Latest trade endpoints
    @GET("v2/stocks/{symbol}/trades/latest")
    suspend fun getLatestTrade(@Path("symbol") symbol: String): Response<SingleTradeResponse>
    
    @GET("v2/stocks/trades/latest")
    suspend fun getLatestTrades(@Query("symbols") symbols: String): Response<MultiTradeResponse>
    
    // Historical quotes
    @GET("v2/stocks/{symbol}/quotes")
    suspend fun getHistoricalQuotes(
        @Path("symbol") symbol: String,
        @Query("start") start: String? = null,
        @Query("end") end: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("page_token") pageToken: String? = null,
        @Query("timeframe") timeframe: String? = null
    ): Response<HistoricalQuotesResponse>
    
    // Historical trades
    @GET("v2/stocks/{symbol}/trades")
    suspend fun getHistoricalTrades(
        @Path("symbol") symbol: String,
        @Query("start") start: String? = null,
        @Query("end") end: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("page_token") pageToken: String? = null,
        @Query("timeframe") timeframe: String? = null
    ): Response<HistoricalTradesResponse>
    
    // Historical bars (OHLCV)
    @GET("v2/stocks/{symbol}/bars")
    suspend fun getHistoricalBars(
        @Path("symbol") symbol: String,
        @Query("start") start: String? = null,
        @Query("end") end: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("page_token") pageToken: String? = null,
        @Query("timeframe") timeframe: String = "1Day",
        @Query("adjustment") adjustment: String = "raw"
    ): Response<HistoricalBarsResponse>
    
    // Snapshots
    @GET("v2/stocks/{symbol}/snapshot")
    suspend fun getSnapshot(@Path("symbol") symbol: String): Response<SnapshotResponse>
    
    @GET("v2/stocks/snapshots")
    suspend fun getSnapshots(@Query("symbols") symbols: String): Response<SnapshotsResponse>
    
    // News
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

// Data models for market data
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
    val snapshot: Snapshot
)

data class SnapshotsResponse(
    val snapshots: Map<String, Snapshot>
)

data class NewsArticle(
    val id: String,
    val headline: String,
    val author: String,
    val created_at: String,
    val updated_at: String,
    val summary: String,
    val content: String?,
    val images: List<NewsImage>?,
    val symbols: List<String>,
    val url: String?
)

data class NewsImage(
    val size: String,
    val url: String
)

data class NewsResponse(
    val news: List<NewsArticle>,
    val next_page_token: String?
)