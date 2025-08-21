data class TechnicalIndicators(
    val ema9: List<Double> = emptyList(),
    val ema20: List<Double> = emptyList(),
    val ema50: List<Double> = emptyList(),    // New
    val ema200: List<Double> = emptyList(),
    val vwap: List<Double> = emptyList(),
    val macd: MacdData = MacdData(),
    val rsi: List<Double> = emptyList(),
    val bollingerBands: BollingerBands = BollingerBands()
)

data class MacdData(
    val macdLine: List<Double> = emptyList(),
    val signalLine: List<Double> = emptyList(),
    val histogram: List<Double> = emptyList()
)

data class BollingerBands(
    val upperBand: List<Double> = emptyList(),
    val middleBand: List<Double> = emptyList(),
    val lowerBand: List<Double> = emptyList()
)

data class NewsItem(
    val id: String,
    val title: String,
    val summary: String,
    val source: String,
    val url: String,
    val publishedAt: Long,
    val timeAgo: String,
    val sentiment: String? = null
)

data class SymbolDetailUiState(
    val isLoading: Boolean = false,
    val isLiveDataActive: Boolean = false,
    val isLoadingNews: Boolean = false,
    val currentQuote: Quote? = null,
    val chartData: ChartData = ChartData(),
    val technicalIndicators: TechnicalIndicators = TechnicalIndicators(),
    val symbolPosition: Position? = null,
    val symbolOrders: List<Order> = emptyList(),
    val symbolNews: List<NewsItem> = emptyList(),
    val errorMessage: String? = null
)