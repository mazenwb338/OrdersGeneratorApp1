package com.example.ordersgeneratorapp.screensng, symbol: String): MarketData? {
    return try {
import androidx.compose.foundation.*jsonResponse)
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRowonObject.getJSONObject("latestTrade").optDouble("price", 0.0)
import androidx.compose.foundation.lazy.itemssonObject.getJSONObject("prevDayBar").optDouble("c", 0.0)
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*has("latestQuote")) jsonObject.getJSONObject("latestQuote") else null
import androidx.compose.runtime.*
import androidx.compose.ui.Alignmentue with realistic fallbacks
import androidx.compose.ui.Modifierle("bidPrice", 0.0) ?: 0.0
import androidx.compose.ui.draw.clipe("askPrice", 0.0) ?: 0.0
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgbawBid else currentPrice * 0.9995
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp + (currentPrice * 0.001)
import androidx.compose.ui.viewinterop.AndroidView 1.0005
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.components.*("volume", 0L) ?: 0L
import com.github.mikephil.charting.data.*as("prevDayBar")) {
import com.github.mikephil.charting.formatter.ValueFormatter("prevDayBar").optDouble("c", currentPrice)
import com.example.ordersgeneratorapp.data.*Close) / prevClose * 100) else 0.0
import com.example.ordersgeneratorapp.hotkey.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
ce = bidPrice,
/**Price,
 * Enhanced Symbol Detail Screen with:            volume = volume,
 * - Live streaming chart with EMAs (9,20,50,200), VWAP, MACD         dayChange = dayChange,
 * - Hotkey trading buttonsTimeMillis()
 * - Current positions and open orders for the symbol
 * - Latest news feed for the symbol{
 */et data for $symbol", e)
@OptIn(ExperimentalMaterial3Api::class)
@Composable }
fun SymbolDetailScreen(    symbol: String,    onNavigateBack: () -> Unit,    viewModel: SymbolDetailViewModel = viewModel()) {    var selectedTimeframe by remember { mutableStateOf("1D") }    var showOrderDialog by remember { mutableStateOf(false) }    var orderSide by remember { mutableStateOf("buy") }        // Initialize data loading with live updates    LaunchedEffect(symbol) {        viewModel.initialize(symbol)        // Live data refresh every 5 seconds        while (true) {            delay(5000)            viewModel.refreshLiveData()        }    }        val uiState by viewModel.uiState.collectAsState()        Column(        modifier = Modifier            .fillMaxSize()            .background(MaterialTheme.colorScheme.background)    ) {        // Enhanced Top Bar        TopAppBar(            title = {                 Column {                    Text(                        text = symbol,                        fontSize = 20.sp,                        fontWeight = FontWeight.Bold                    )                    uiState.currentQuote?.let { quote ->                        Text(                            text = "$${String.format("%.2f", quote.lastPrice)} ${if (quote.change >= 0) "+" else ""}${String.format("%.2f", quote.change)} (${String.format("%.2f", quote.changePercent)}%)",                            fontSize = 12.sp,                            color = if (quote.change >= 0) Color(0xFF4CAF50) else Color(0xFFFF5722)                        )                    }                }            },            navigationIcon = {                IconButton(onClick = onNavigateBack) {                    Icon(Icons.Default.ArrowBack, "Back")                }            },            actions = {                // Live indicator                if (uiState.isLiveDataActive) {                    Row(                        verticalAlignment = Alignment.CenterVertically,                        modifier = Modifier.padding(horizontal = 8.dp)                    ) {                        Box(                            modifier = Modifier                                .size(8.dp)                                .background(Color.Green, androidx.compose.foundation.shape.CircleShape)                        )                        Spacer(modifier = Modifier.width(4.dp))                        Text("LIVE", fontSize = 10.sp, color = Color.Green)                    }                }                IconButton(onClick = { viewModel.refreshData() }) {                    Icon(                        Icons.Default.Refresh,                         "Refresh",                        tint = if (uiState.isLoading) Color.Green else Color.Gray                    )                }            }        )                LazyColumn(            modifier = Modifier.fillMaxSize(),            verticalArrangement = Arrangement.spacedBy(12.dp)        ) {            // Timeframe Selector            item {                TimeframeSelectorCard(                    selectedTimeframe = selectedTimeframe,                    onTimeframeSelected = {                         selectedTimeframe = it                        viewModel.loadChartData(symbol, it)                    }                )            }                        // Live Chart with All Indicators            item {                EnhancedLiveChart(                    symbol = symbol,                    chartData = uiState.chartData,                    technicalIndicators = uiState.technicalIndicators,                    isLoading = uiState.isLoading,                    modifier = Modifier                        .fillMaxWidth()                        .height(500.dp)                        .padding(horizontal = 16.dp)                )            }                        // Trading Hotkeys            item {                TradingHotkeysCard(                    symbol = symbol,                    currentPrice = uiState.currentQuote?.lastPrice ?: 0.0,                    onQuickTrade = { side, preset ->                         orderSide = side                        showOrderDialog = true                    }                )            }                        // Position and Orders Row            item {                Row(                    modifier = Modifier                        .fillMaxWidth()                        .padding(horizontal = 16.dp),                    horizontalArrangement = Arrangement.spacedBy(8.dp)                ) {                    // Current Position for Symbol                    SymbolPositionCard(                        symbol = symbol,                        position = uiState.symbolPosition,                        modifier = Modifier.weight(1f)                    )                                        // Open Orders for Symbol                    SymbolOrdersCard(                        symbol = symbol,                        orders = uiState.symbolOrders,                        onCancelOrder = { viewModel.cancelOrder(it) },                        modifier = Modifier.weight(1f)                    )                }            }                        // Latest News Feed            item {                SymbolNewsCard(                    symbol = symbol,                    news = uiState.symbolNews,                    isLoadingNews = uiState.isLoadingNews,                    onRefreshNews = { viewModel.refreshNews() }                )            }        }    }        // Order Dialog    if (showOrderDialog) {        OrderDialog(            symbol = symbol,            side = orderSide,            currentPrice = uiState.currentQuote?.lastPrice ?: 0.0,            onDismiss = { showOrderDialog = false },            onConfirm = { orderRequest ->                viewModel.placeOrder(orderRequest)                showOrderDialog = false            }        )    }}@Composablefun TimeframeSelectorCard(    selectedTimeframe: String,    onTimeframeSelected: (String) -> Unit) {    Card(        modifier = Modifier            .fillMaxWidth()            .padding(horizontal = 16.dp)    ) {        LazyRow(            modifier = Modifier.padding(16.dp),            horizontalArrangement = Arrangement.spacedBy(8.dp)        ) {            items(listOf("1m", "5m", "15m", "30m", "1H", "4H", "1D", "1W")) { timeframe ->                FilterChip(                    onClick = { onTimeframeSelected(timeframe) },                    label = { Text(timeframe) },                    selected = selectedTimeframe == timeframe                )            }        }    }}@Composablefun EnhancedLiveChart(    symbol: String,    chartData: ChartData,    technicalIndicators: TechnicalIndicators,    isLoading: Boolean,    modifier: Modifier = Modifier) {    Card(        modifier = modifier,        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)    ) {        Column(            modifier = Modifier.padding(16.dp)        ) {            Text(                text = "$symbol - Live Chart with Indicators",                fontSize = 18.sp,                fontWeight = FontWeight.Bold,                modifier = Modifier.padding(bottom = 12.dp)            )                        Box(modifier = Modifier.fillMaxSize()) {                if (isLoading) {                    Box(                        modifier = Modifier.fillMaxSize(),                        contentAlignment = Alignment.Center                    ) {                        Column(horizontalAlignment = Alignment.CenterHorizontally) {                            CircularProgressIndicator()                            Spacer(modifier = Modifier.height(8.dp))                            Text("Loading live chart data...")                        }                    }                } else {                    AndroidView(                        factory = { context ->                            CombinedChart(context).apply {                                description.isEnabled = false                                setTouchEnabled(true)                                setDragEnabled(true)                                setScaleEnabled(true)                                setPinchZoom(true)                                setDrawGridBackground(false)                                                                // Configure X-axis                                xAxis.apply {                                    position = XAxis.XAxisPosition.BOTTOM                                    setDrawGridLines(true)                                    gridColor = Color.Gray.toArgb()                                    textColor = Color.Gray.toArgb()                                    valueFormatter = object : ValueFormatter() {                                        private val format = SimpleDateFormat("HH:mm", Locale.getDefault())                                        override fun getFormattedValue(value: Float): String {                                            return format.format(Date(value.toLong() * 1000))                                        }                                    }                                }                                                                // Configure Y-axes                                axisLeft.apply {                                    setDrawGridLines(true)                                    gridColor = Color.Gray.toArgb()                                    textColor = Color.Gray.toArgb()                                }                                axisRight.isEnabled = false                                                                // Enhanced legend                                legend.apply {                                    isEnabled = true                                    textColor = Color.Gray.toArgb()                                    form = Legend.LegendForm.LINE                                    horizontalAlignment = Legend.LegendHorizontalAlignment.LEFT                                    verticalAlignment = Legend.LegendVerticalAlignment.TOP                                }                            }                        },                        update = { chart ->                            val combinedData = CombinedData()                                                        // Add candlestick data                            val candleEntries = chartData.candleData.mapIndexed { index, candle ->                                CandleEntry(                                    index.toFloat(),                                    candle.high.toFloat(),                                    candle.low.toFloat(),                                    candle.open.toFloat(),                                    candle.close.toFloat()                                )                            }                                                        val candleDataSet = CandleDataSet(candleEntries, "Price").apply {                                color = Color.Gray.toArgb()                                shadowColor = Color.Gray.toArgb()                                shadowWidth = 1f                                decreasingColor = Color.Red.toArgb()                                decreasingPaintStyle = android.graphics.Paint.Style.FILL                                increasingColor = Color.Green.toArgb()                                increasingPaintStyle = android.graphics.Paint.Style.FILL                                neutralColor = Color.Gray.toArgb()                            }                                                        combinedData.setData(CandleData(candleDataSet))                                                        // Add EMA and VWAP lines                            val lineData = LineData()                                                        // 9 EMA - Green                            val ema9Entries = technicalIndicators.ema9.mapIndexed { index, value ->                                Entry(index.toFloat(), value.toFloat())                            }                            val ema9DataSet = LineDataSet(ema9Entries, "EMA 9").apply {                                color = Color.Green.toArgb()                                lineWidth = 2f                                setDrawCircles(false)                                setDrawValues(false)                            }                            lineData.addDataSet(ema9DataSet)                                                        // 20 EMA - Orange                            val ema20Entries = technicalIndicators.ema20.mapIndexed { index, value ->                                Entry(index.toFloat(), value.toFloat())                            }                            val ema20DataSet = LineDataSet(ema20Entries, "EMA 20").apply {                                color = Color(0xFFFF9800).toArgb() // Orange                                lineWidth = 2f                                setDrawCircles(false)                                setDrawValues(false)                            }                            lineData.addDataSet(ema20DataSet)                                                        // 50 EMA - Red                            val ema50Entries = technicalIndicators.ema50.mapIndexed { index, value ->                                Entry(index.toFloat(), value.toFloat())                            }                            val ema50DataSet = LineDataSet(ema50Entries, "EMA 50").apply {                                color = Color.Red.toArgb()                                lineWidth = 2.5f                                setDrawCircles(false)                                setDrawValues(false)                            }                            lineData.addDataSet(ema50DataSet)                                                        // 200 EMA - Black                            val ema200Entries = technicalIndicators.ema200.mapIndexed { index, value ->                                Entry(index.toFloat(), value.toFloat())                            }                            val ema200DataSet = LineDataSet(ema200Entries, "EMA 200").apply {                                color = Color.Black.toArgb()                                lineWidth = 3f                                setDrawCircles(false)                                setDrawValues(false)                            }                            lineData.addDataSet(ema200DataSet)                                                        // VWAP - Cyan dashed                            val vwapEntries = technicalIndicators.vwap.mapIndexed { index, value ->                                Entry(index.toFloat(), value.toFloat())                            }                            val vwapDataSet = LineDataSet(vwapEntries, "VWAP").apply {                                color = Color.Cyan.toArgb()                                lineWidth = 2f                                setDrawCircles(false)                                setDrawValues(false)                                enableDashedLine(10f, 5f, 0f)                            }                            lineData.addDataSet(vwapDataSet)                                                        combinedData.setData(lineData)                                                        chart.data = combinedData                            chart.invalidate()                        },                        modifier = Modifier.fillMaxSize()                    )                }            }        }    }}@Composablefun TradingHotkeysCard(    symbol: String,    currentPrice: Double,    onQuickTrade: (String, String) -> Unit) {    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Quick Trading - $${String.format("%.2f", currentPrice)}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            // Buy buttons row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(listOf("25", "50", "100", "200", "500")) { quantity ->
                    Button(
                        onClick = { onQuickTrade("buy", quantity) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        modifier = Modifier.width(80.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("BUY", fontSize = 10.sp)
                            Text(quantity, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Sell buttons row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(listOf("25", "50", "100", "200", "500")) { quantity ->
                    Button(
                        onClick = { onQuickTrade("sell", quantity) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
                        modifier = Modifier.width(80.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("SELL", fontSize = 10.sp)
                            Text(quantity, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SymbolPositionCard(
    symbol: String,
    position: Position?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = position?.let { 
                if (it.unrealizedPL >= 0) Color(0xFF4CAF50).copy(alpha = 0.1f) 
                else Color(0xFFF44336).copy(alpha = 0.1f) 
            } ?: MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Position",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            if (position != null) {
                Text("Qty: ${position.quantity}")
                Text("Avg: $${String.format("%.2f", position.avgCost)}")
                Text(
                    text = "P&L: $${String.format("%.2f", position.unrealizedPL)}",
                    color = if (position.unrealizedPL >= 0) Color(0xFF4CAF50) else Color(0xFFF44336),
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = "No position",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun SymbolOrdersCard(
    symbol: String,
    orders: List<Order>,
    onCancelOrder: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Open Orders",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            if (orders.isNotEmpty()) {
                orders.forEach { order ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("${order.side.uppercase()} ${order.quantity}")
                            Text("@ $${String.format("%.2f", order.price)}")
                        }
                        IconButton(
                            onClick = { onCancelOrder(order.id) },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Cancel",
                                tint = Color.Red,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    if (order != orders.last()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            } else {
                Text(
                    text = "No open orders",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun SymbolNewsCard(
    symbol: String,
    news: List<NewsItem>,
    isLoadingNews: Boolean,
    onRefreshNews: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Latest News - $symbol",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onRefreshNews) {
                    Icon(Icons.Default.Refresh, "Refresh News")
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (isLoadingNews) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                news.take(5).forEach { newsItem ->
                    NewsItemCard(newsItem = newsItem)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun NewsItemCard(newsItem: NewsItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Navigate to full article */ },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = newsItem.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = newsItem.source,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = newsItem.timeAgo,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}