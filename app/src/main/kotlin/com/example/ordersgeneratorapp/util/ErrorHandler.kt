package com.example.ordersgeneratorapp.util

import android.util.Log
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object ErrorHandler {
    private const val TAG = "ErrorHandler"
    
    fun handleApiError(throwable: Throwable, operation: String): String {
        Log.e(TAG, "Error during $operation", throwable)
        
        return when (throwable) {
            is HttpException -> {
                when (throwable.code()) {
                    401 -> "Authentication failed. Please check your API credentials."
                    403 -> "Access forbidden. Please verify your account permissions."
                    404 -> "Resource not found."
                    422 -> "Invalid request data. Please check your input."
                    429 -> "Rate limit exceeded. Please try again later."
                    500 -> "Server error. Please try again later."
                    503 -> "Service unavailable. Please try again later."
                    else -> "HTTP error ${throwable.code()}: ${throwable.message()}"
                }
            }
            is SocketTimeoutException -> "Connection timeout. Please check your internet connection."
            is UnknownHostException -> "Unable to connect to server. Please check your internet connection."
            is IOException -> "Network error. Please check your connection and try again."
            else -> throwable.message ?: "Unknown error occurred"
        }
    }
    
    fun handleGenericError(throwable: Throwable, operation: String): String {
        Log.e(TAG, "Generic error during $operation", throwable)
        return throwable.message ?: "An unexpected error occurred during $operation"
    }
}