package com.example.ordersgeneratorapp.repository

import android.util.Log
import kotlinx.coroutines.delay

class IbkrRepository {
    suspend fun testConnection(apiKey: String, baseUrl: String): Result<Unit> {
        return try {
            // TODO: Replace with real HTTP call
            Log.d("IbkrRepository", "Testing IBKR connection to $baseUrl")
            delay(500)
            if (apiKey.isBlank()) return Result.failure(IllegalArgumentException("Missing API Key"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}