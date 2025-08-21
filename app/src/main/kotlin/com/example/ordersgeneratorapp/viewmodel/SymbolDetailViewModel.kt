package com.example.ordersgeneratorapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ordersgeneratorapp.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject

@HiltViewModel
class SymbolDetailViewModel @Inject constructor(
    private val marketDataRepository: MarketDataRepository,
    private val tradingRepository: TradingRepository,
    private val newsRepository: NewsRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SymbolDetailUiState())
    val uiState: StateFlow<SymbolDetailUiState> = _uiState.asStateFlow()
    
    private var currentSymbol: String = ""
    private var refreshJob: kotlinx.coroutines.Job? = null
    
    fun initialize(symbol: String) {
        currentSymbol = symbol
        loadInitialData()
        startLiveDataStream()
    }
    
    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                // Load all initial data
                val quote = marketDataRepository.getQuote(currentSymbol)
                val chartData = marketDataRepository.getChartData(currentSymbol, "1D")
                val indicators = marketDataRepository.getTechnicalIndicators(currentSymbol, "1D")
                val position = tradingRepository.getPosition(currentSymbol)
                val orders = tradingRepository.getOrdersForSymbol(currentSymbol)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    currentQuote = quote,
                    chartData = chartData,
                    technicalIndicators = indicators,
                    symbolPosition = position,
                    symbolOrders = orders
                )
                
                // Load news separately
                loadNews()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message
                )
            }
        }
    }
    
    fun loadChartData(symbol: String, timeframe: String) {
        viewModelScope.launch {
            try {
                val chartData = marketDataRepository.getChartData(symbol, timeframe)
                val indicators = marketDataRepository.getTechnicalIndicators(symbol, timeframe)
                
                _uiState.value = _uiState.value.copy(
                    chartData = chartData,
                    technicalIndicators = indicators
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }
    
    private fun startLiveDataStream() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLiveDataActive = true)
            
            while (true) {
                try {
                    refreshLiveData()
                    delay(5000) // Refresh every 5 seconds
                } catch (e: Exception) {
                    // Log error but continue streaming
                    delay(10000) // Wait longer on error
                }
            }
        }
    }
    
    fun refreshLiveData() {
        viewModelScope.launch {
            try {
                val quote = marketDataRepository.getQuote(currentSymbol)
                val position = tradingRepository.getPosition(currentSymbol)
                val orders = tradingRepository.getOrdersForSymbol(currentSymbol)
                
                _uiState.value = _uiState.value.copy(
                    currentQuote = quote,
                    symbolPosition = position,
                    symbolOrders = orders
                )
            } catch (e: Exception) {
                // Handle silently for live updates
            }
        }
    }
    
    fun refreshData() {
        loadInitialData()
    }
    
    private fun loadNews() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingNews = true)
            
            try {
                val news = newsRepository.getNewsForSymbol(currentSymbol)
                _uiState.value = _uiState.value.copy(
                    isLoadingNews = false,
                    symbolNews = news
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingNews = false,
                    errorMessage = e.message
                )
            }
        }
    }
    
    fun refreshNews() {
        loadNews()
    }
    
    fun placeOrder(orderRequest: OrderRequest) {
        viewModelScope.launch {
            try {
                tradingRepository.placeOrder(orderRequest)
                // Refresh orders after placing
                val orders = tradingRepository.getOrdersForSymbol(currentSymbol)
                _uiState.value = _uiState.value.copy(symbolOrders = orders)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }
    
    fun cancelOrder(orderId: String) {
        viewModelScope.launch {
            try {
                tradingRepository.cancelOrder(orderId)
                // Refresh orders after canceling
                val orders = tradingRepository.getOrdersForSymbol(currentSymbol)
                _uiState.value = _uiState.value.copy(symbolOrders = orders)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        refreshJob?.cancel()
        _uiState.value = _uiState.value.copy(isLiveDataActive = false)
    }
}