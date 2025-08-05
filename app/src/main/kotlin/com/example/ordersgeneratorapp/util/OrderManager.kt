package com.example.ordersgeneratorapp.util

import java.io.File
import java.util.UUID
import org.json.JSONObject

object OrderManager {
    fun writeOrder(symbol: String, quantity: String, orderType: String, side: String, broker: String) {
        val order = JSONObject().apply {
            put("id", UUID.randomUUID().toString())
            put("symbol", symbol)
            put("quantity", quantity.toIntOrNull() ?: 0)
            put("type", orderType)
            put("side", side)
            put("broker", broker)
        }
        val file = File("/data/data/com.example.ordersgeneratorapp/files/orders/order_${System.currentTimeMillis()}.json")
        file.parentFile.mkdirs()
        file.writeText(order.toString())
    }
}
