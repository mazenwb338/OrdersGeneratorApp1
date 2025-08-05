package com.example.ordersgeneratorapp.api

import retrofit2.Response
import retrofit2.http.*

interface AlpacaApiService {
    
    @GET("v2/account")
    suspend fun getAccount(): Response<AlpacaAccount>
    
    @POST("v2/orders")
    suspend fun createOrder(@Body order: CreateOrderRequest): Response<AlpacaOrder>
    
    @GET("v2/orders")
    suspend fun getOrders(
        @Query("status") status: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("direction") direction: String? = null
    ): Response<List<AlpacaOrder>>
    
    @DELETE("v2/orders/{order_id}")
    suspend fun cancelOrder(@Path("order_id") orderId: String): Response<Unit>
    
    @GET("v2/positions")
    suspend fun getPositions(): Response<List<AlpacaPosition>>
    
    @GET("v2/positions/{symbol}")
    suspend fun getPosition(@Path("symbol") symbol: String): Response<AlpacaPosition>
}