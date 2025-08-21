package com.example.ordersgeneratorapp.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.components.*
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.example.ordersgeneratorapp.R
import com.example.ordersgeneratorapp.data.*
import com.example.ordersgeneratorapp.viewmodel.SymbolDetailViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SymbolDetailScreen(
    symbol: String,
    onNavigateBack: () -> Unit,
    viewModel: SymbolDetailViewModel = viewModel()
) {
    var selectedTimeframe by remember { mutableStateOf("1D") }
    var showOrderDialog by remember { mutableStateOf(false) }
    var orderSide by remember { mutableStateOf("buy") }
    
    // Dark theme colors
    val backgroundColor = colorResource(R.color.backgroundColor)
    val surfaceColor = colorResource(R.color.surfaceColor)
    val cardColor = colorResource(R.color.cardColor)
    val bullishGreen = colorResource(R.color.bullish_green)
    val bearishRed = colorResource(R.color.bearish_red)
    
    // Initialize data loading with live updates
    LaunchedEffect(symbol) {
        viewModel.initialize(symbol)
    }
    
    val uiState by viewModel.uiState.collectAsState()
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Enhanced Top Bar with dark theme
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            text = symbol,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        uiState.currentQuote?.let { quote ->
                            Text(
                                text = "$${String.format("%.2f", quote.lastPrice)} ${if (quote.change >= 0) "+" else ""}${String.format("%.2f", quote.change)} (${String.format("%.2f", quote.changePercent)}%)",
                                fontSize = 12.sp,
                                color = if (quote.change >= 0) bullishGreen else bearishRed
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack, 
                            "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Live indicator
                    if (uiState.isLiveDataActive) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(bullishGreen, androidx.compose.foundation.shape.CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("LIVE", fontSize = 10.sp, color = bullishGreen)
                        }
                    }
                    IconButton(onClick = { viewModel.refreshData() }) {
                        Icon(
                            Icons.Default.Refresh, 
                            "Refresh",
                            tint = if (uiState.isLoading) bullishGreen else Color.Gray
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = surfaceColor
                )
            )
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Timeframe Selector
                item {
                    TimeframeSelectorCard(
                        selectedTimeframe = selectedTimeframe,
                        onTimeframeSelected = { 
                            selectedTimeframe = it
                            viewModel.loadChartData(symbol, it)
                        },
                        cardColor = cardColor
                    )
                }
                
                // Live Chart with All Indicators
                item {
                    EnhancedLiveChart(
                        symbol = symbol,
                        chartData = uiState.chartData,
                        technicalIndicators = uiState.technicalIndicators,
                        isLoading = uiState.isLoading,
                        cardColor = cardColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(500.dp)
                            .padding(horizontal = 16.dp)
                    )
                }
                
                // Trading Hotkeys
                item {
                    TradingHotkeysCard(
                        symbol = symbol,
                        currentPrice = uiState.currentQuote?.lastPrice ?: 0.0,
                        onQuickTrade = { side, preset -> 
                            orderSide = side
                            showOrderDialog = true
                        },
                        cardColor = cardColor,
                        bullishGreen = bullishGreen,
                        bearishRed = bearishRed
                    )
                }
                
                // Position and Orders Row
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Current Position for Symbol
                        SymbolPositionCard(
                            symbol = symbol,
                            position = uiState.symbolPosition,
                            cardColor = cardColor,
                            bullishGreen = bullishGreen,
                            bearishRed = bearishRed,
                            modifier = Modifier.weight(1f)
                        )
                        
                        // Open Orders for Symbol
                        SymbolOrdersCard(
                            symbol = symbol,
                            orders = uiState.symbolOrders,
                            onCancelOrder = { viewModel.cancelOrder(it) },
                            cardColor = cardColor,
                            bearishRed = bearishRed,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                // Latest News Feed
                item {
                    SymbolNewsCard(
                        symbol = symbol,
                        news = uiState.symbolNews,
                        isLoadingNews = uiState.isLoadingNews,
                        onRefreshNews = { viewModel.refreshNews() },
                        cardColor = cardColor,
                        surfaceColor = surfaceColor
                    )
                }
            }
        }
    }
    
    // Order Dialog
    if (showOrderDialog) {
        OrderDialog(
            symbol = symbol,
            side = orderSide,
            currentPrice = uiState.currentQuote?.lastPrice ?: 0.0,
            onDismiss = { showOrderDialog = false },
            onConfirm = { orderRequest ->
                viewModel.placeOrder(orderRequest)
                showOrderDialog = false
            }
        )
    }
}

@Composable
fun TimeframeSelectorCard(
    selectedTimeframe: String,
    onTimeframeSelected: (String) -> Unit,
    cardColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        LazyRow(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(listOf("1m", "5m", "15m", "30m", "1H", "4H", "1D", "1W")) { timeframe ->
                FilterChip(
                    onClick = { onTimeframeSelected(timeframe) },
                    label = { Text(timeframe, color = Color.White) },
                    selected = selectedTimeframe == timeframe,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color.Gray.copy(alpha = 0.3f),
                        selectedContainerColor = colorResource(R.color.colorAccent)
                    )
                )
            }
        }
    }
}

@Composable
fun EnhancedLiveChart(
    symbol: String,
    chartData: ChartData,
    technicalIndicators: TechnicalIndicators,
    isLoading: Boolean,
    cardColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "$symbol - Live Chart with Indicators",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = colorResource(R.color.colorAccent))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Loading live chart data...", color = Color.White)
                        }
                    }
                } else {
                    AndroidView(
                        factory = { context ->
                            CombinedChart(context).apply {
                                description.isEnabled = false
                                setTouchEnabled(true)
                                setDragEnabled(true)
                                setScaleEnabled(true)
                                setPinchZoom(true)
                                setDrawGridBackground(false)
                                setBackgroundColor(cardColor.toArgb())
                                
                                // Configure X-axis for dark theme
                                xAxis.apply {
                                    position = XAxis.XAxisPosition.BOTTOM
                                    setDrawGridLines(true)
                                    gridColor = Color.Gray.copy(alpha = 0.3f).toArgb()
                                    textColor = Color.White.toArgb()
                                    valueFormatter = object : ValueFormatter() {
                                        private val format = SimpleDateFormat("HH:mm", Locale.getDefault())
                                        override fun getFormattedValue(value: Float): String {
                                            return format.format(Date(value.toLong() * 1000))
                                        }
                                    }
                                }
                                
                                // Configure Y-axes for dark theme
                                axisLeft.apply {
                                    setDrawGridLines(true)
                                    gridColor = Color.Gray.copy(alpha = 0.3f).toArgb()
                                    textColor = Color.White.toArgb()
                                }
                                axisRight.isEnabled = false
                                
                                // Enhanced legend for dark theme
                                legend.apply {
                                    isEnabled = true
                                    textColor = Color.White.toArgb()
                                    form = Legend.LegendForm.LINE
                                    horizontalAlignment = Legend.LegendHorizontalAlignment.LEFT
                                    verticalAlignment = Legend.LegendVerticalAlignment.TOP
                                }
                            }
                        },
                        update = { chart ->
                            val combinedData = CombinedData()
                            
                            // Add candlestick data
                            if (chartData.candleData.isNotEmpty()) {
                                val candleEntries = chartData.candleData.mapIndexed { index, candle ->
                                    CandleEntry(
                                        index.toFloat(),
                                        candle.high.toFloat(),
                                        candle.low.toFloat(),
                                        candle.open.toFloat(),
                                        candle.close.toFloat()
                                    )
                                }
                                
                                val candleDataSet = CandleDataSet(candleEntries, "Price").apply {
                                    color = Color.Gray.toArgb()
                                    shadowColor = Color.Gray.toArgb()
                                    shadowWidth = 1f
                                    decreasingColor = colorResource(R.color.bearish_red).toArgb()
                                    decreasingPaintStyle = android.graphics.Paint.Style.FILL
                                    increasingColor = colorResource(R.color.bullish_green).toArgb()
                                    increasingPaintStyle = android.graphics.Paint.Style.FILL
                                    neutralColor = Color.Gray.toArgb()
                                }
                                
                                combinedData.setData(CandleData(candleDataSet))
                            }
                            
                            // Add EMA and VWAP lines with specified colors
                            val lineData = LineData()
                            
                            // 9 EMA - Green
                            if (technicalIndicators.ema9.isNotEmpty()) {
                                val ema9Entries = technicalIndicators.ema9.mapIndexed { index, value ->
                                    Entry(index.toFloat(), value.toFloat())
                                }
                                val ema9DataSet = LineDataSet(ema9Entries, "EMA 9").apply {
                                    color = colorResource(R.color.ema9_green).toArgb()
                                    lineWidth = 2f
                                    setDrawCircles(false)
                                    setDrawValues(false)
                                }
                                lineData.addDataSet(ema9DataSet)
                            }
                            
                            // 20 EMA - Orange
                            if (technicalIndicators.ema20.isNotEmpty()) {
                                val ema20Entries = technicalIndicators.ema20.mapIndexed { index, value ->
                                    Entry(index.toFloat(), value.toFloat())
                                }
                                val ema20DataSet = LineDataSet(ema20Entries, "EMA 20").apply {
                                    color = colorResource(R.color.ema20_orange).toArgb()
                                    lineWidth = 2f
                                    setDrawCircles(false)
                                    setDrawValues(false)
                                }
                                lineData.addDataSet(ema20DataSet)
                            }
                            
                            // 50 EMA - Red
                            if (technicalIndicators.ema50.isNotEmpty()) {
                                val ema50Entries = technicalIndicators.ema50.mapIndexed { index, value ->
                                    Entry(index.toFloat(), value.toFloat())
                                }
                                val ema// filepath: /Users/mazenbteddini/OrdersGeneratorApp/app/src/main/kotlin/com/example/ordersgeneratorapp/screens/SymbolDetailScreen.kt
package com.example.ordersgeneratorapp.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.components.*
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.example.ordersgeneratorapp.R
import com.example.ordersgeneratorapp.data.*
import com.example.ordersgeneratorapp.viewmodel.SymbolDetailViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SymbolDetailScreen(
    symbol: String,
    onNavigateBack: () -> Unit,
    viewModel: SymbolDetailViewModel = viewModel()
) {
    var selectedTimeframe by remember { mutableStateOf("1D") }
    var showOrderDialog by remember { mutableStateOf(false) }
    var orderSide by remember { mutableStateOf("buy") }
    
    // Dark theme colors
    val backgroundColor = colorResource(R.color.backgroundColor)
    val surfaceColor = colorResource(R.color.surfaceColor)
    val cardColor = colorResource(R.color.cardColor)
    val bullishGreen = colorResource(R.color.bullish_green)
    val bearishRed = colorResource(R.color.bearish_red)
    
    // Initialize data loading with live updates
    LaunchedEffect(symbol) {
        viewModel.initialize(symbol)
    }
    
    val uiState by viewModel.uiState.collectAsState()
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Enhanced Top Bar with dark theme
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            text = symbol,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        uiState.currentQuote?.let { quote ->
                            Text(
                                text = "$${String.format("%.2f", quote.lastPrice)} ${if (quote.change >= 0) "+" else ""}${String.format("%.2f", quote.change)} (${String.format("%.2f", quote.changePercent)}%)",
                                fontSize = 12.sp,
                                color = if (quote.change >= 0) bullishGreen else bearishRed
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack, 
                            "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Live indicator
                    if (uiState.isLiveDataActive) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(bullishGreen, androidx.compose.foundation.shape.CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("LIVE", fontSize = 10.sp, color = bullishGreen)
                        }
                    }
                    IconButton(onClick = { viewModel.refreshData() }) {
                        Icon(
                            Icons.Default.Refresh, 
                            "Refresh",
                            tint = if (uiState.isLoading) bullishGreen else Color.Gray
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = surfaceColor
                )
            )
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Timeframe Selector
                item {
                    TimeframeSelectorCard(
                        selectedTimeframe = selectedTimeframe,
                        onTimeframeSelected = { 
                            selectedTimeframe = it
                            viewModel.loadChartData(symbol, it)
                        },
                        cardColor = cardColor
                    )
                }
                
                // Live Chart with All Indicators
                item {
                    EnhancedLiveChart(
                        symbol = symbol,
                        chartData = uiState.chartData,
                        technicalIndicators = uiState.technicalIndicators,
                        isLoading = uiState.isLoading,
                        cardColor = cardColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(500.dp)
                            .padding(horizontal = 16.dp)
                    )
                }
                
                // Trading Hotkeys
                item {
                    TradingHotkeysCard(
                        symbol = symbol,
                        currentPrice = uiState.currentQuote?.lastPrice ?: 0.0,
                        onQuickTrade = { side, preset -> 
                            orderSide = side
                            showOrderDialog = true
                        },
                        cardColor = cardColor,
                        bullishGreen = bullishGreen,
                        bearishRed = bearishRed
                    )
                }
                
                // Position and Orders Row
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Current Position for Symbol
                        SymbolPositionCard(
                            symbol = symbol,
                            position = uiState.symbolPosition,
                            cardColor = cardColor,
                            bullishGreen = bullishGreen,
                            bearishRed = bearishRed,
                            modifier = Modifier.weight(1f)
                        )
                        
                        // Open Orders for Symbol
                        SymbolOrdersCard(
                            symbol = symbol,
                            orders = uiState.symbolOrders,
                            onCancelOrder = { viewModel.cancelOrder(it) },
                            cardColor = cardColor,
                            bearishRed = bearishRed,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                // Latest News Feed
                item {
                    SymbolNewsCard(
                        symbol = symbol,
                        news = uiState.symbolNews,
                        isLoadingNews = uiState.isLoadingNews,
                        onRefreshNews = { viewModel.refreshNews() },
                        cardColor = cardColor,
                        surfaceColor = surfaceColor
                    )
                }
            }
        }
    }
    
    // Order Dialog
    if (showOrderDialog) {
        OrderDialog(
            symbol = symbol,
            side = orderSide,
            currentPrice = uiState.currentQuote?.lastPrice ?: 0.0,
            onDismiss = { showOrderDialog = false },
            onConfirm = { orderRequest ->
                viewModel.placeOrder(orderRequest)
                showOrderDialog = false
            }
        )
    }
}

@Composable
fun TimeframeSelectorCard(
    selectedTimeframe: String,
    onTimeframeSelected: (String) -> Unit,
    cardColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        LazyRow(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(listOf("1m", "5m", "15m", "30m", "1H", "4H", "1D", "1W")) { timeframe ->
                FilterChip(
                    onClick = { onTimeframeSelected(timeframe) },
                    label = { Text(timeframe, color = Color.White) },
                    selected = selectedTimeframe == timeframe,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color.Gray.copy(alpha = 0.3f),
                        selectedContainerColor = colorResource(R.color.colorAccent)
                    )
                )
            }
        }
    }
}

@Composable
fun EnhancedLiveChart(
    symbol: String,
    chartData: ChartData,
    technicalIndicators: TechnicalIndicators,
    isLoading: Boolean,
    cardColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "$symbol - Live Chart with Indicators",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = colorResource(R.color.colorAccent))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Loading live chart data...", color = Color.White)
                        }
                    }
                } else {
                    AndroidView(
                        factory = { context ->
                            CombinedChart(context).apply {
                                description.isEnabled = false
                                setTouchEnabled(true)
                                setDragEnabled(true)
                                setScaleEnabled(true)
                                setPinchZoom(true)
                                setDrawGridBackground(false)
                                setBackgroundColor(cardColor.toArgb())
                                
                                // Configure X-axis for dark theme
                                xAxis.apply {
                                    position = XAxis.XAxisPosition.BOTTOM
                                    setDrawGridLines(true)
                                    gridColor = Color.Gray.copy(alpha = 0.3f).toArgb()
                                    textColor = Color.White.toArgb()
                                    valueFormatter = object : ValueFormatter() {
                                        private val format = SimpleDateFormat("HH:mm", Locale.getDefault())
                                        override fun getFormattedValue(value: Float): String {
                                            return format.format(Date(value.toLong() * 1000))
                                        }
                                    }
                                }
                                
                                // Configure Y-axes for dark theme
                                axisLeft.apply {
                                    setDrawGridLines(true)
                                    gridColor = Color.Gray.copy(alpha = 0.3f).toArgb()
                                    textColor = Color.White.toArgb()
                                }
                                axisRight.isEnabled = false
                                
                                // Enhanced legend for dark theme
                                legend.apply {
                                    isEnabled = true
                                    textColor = Color.White.toArgb()
                                    form = Legend.LegendForm.LINE
                                    horizontalAlignment = Legend.LegendHorizontalAlignment.LEFT
                                    verticalAlignment = Legend.LegendVerticalAlignment.TOP
                                }
                            }
                        },
                        update = { chart ->
                            val combinedData = CombinedData()
                            
                            // Add candlestick data
                            if (chartData.candleData.isNotEmpty()) {
                                val candleEntries = chartData.candleData.mapIndexed { index, candle ->
                                    CandleEntry(
                                        index.toFloat(),
                                        candle.high.toFloat(),
                                        candle.low.toFloat(),
                                        candle.open.toFloat(),
                                        candle.close.toFloat()
                                    )
                                }
                                
                                val candleDataSet = CandleDataSet(candleEntries, "Price").apply {
                                    color = Color.Gray.toArgb()
                                    shadowColor = Color.Gray.toArgb()
                                    shadowWidth = 1f
                                    decreasingColor = colorResource(R.color.bearish_red).toArgb()
                                    decreasingPaintStyle = android.graphics.Paint.Style.FILL
                                    increasingColor = colorResource(R.color.bullish_green).toArgb()
                                    increasingPaintStyle = android.graphics.Paint.Style.FILL
                                    neutralColor = Color.Gray.toArgb()
                                }
                                
                                combinedData.setData(CandleData(candleDataSet))
                            }
                            
                            // Add EMA and VWAP lines with specified colors
                            val lineData = LineData()
                            
                            // 9 EMA - Green
                            if (technicalIndicators.ema9.isNotEmpty()) {
                                val ema9Entries = technicalIndicators.ema9.mapIndexed { index, value ->
                                    Entry(index.toFloat(), value.toFloat())
                                }
                                val ema9DataSet = LineDataSet(ema9Entries, "EMA 9").apply {
                                    color = colorResource(R.color.ema9_green).toArgb()
                                    lineWidth = 2f
                                    setDrawCircles(false)
                                    setDrawValues(false)
                                }
                                lineData.addDataSet(ema9DataSet)
                            }
                            
                            // 20 EMA - Orange
                            if (technicalIndicators.ema20.isNotEmpty()) {
                                val ema20Entries = technicalIndicators.ema20.mapIndexed { index, value ->
                                    Entry(index.toFloat(), value.toFloat())
                                }
                                val ema20DataSet = LineDataSet(ema20Entries, "EMA 20").apply {
                                    color = colorResource(R.color.ema20_orange).toArgb()
                                    lineWidth = 2f
                                    setDrawCircles(false)
                                    setDrawValues(false)
                                }
                                lineData.addDataSet(ema20DataSet)
                            }
                            
                            // 50 EMA - Red
                            if (technicalIndicators.ema50.isNotEmpty()) {
                                val ema50Entries = technicalIndicators.ema50.mapIndexed { index, value ->
                                    Entry(index.toFloat(), value.toFloat())
                                }
                                val ema