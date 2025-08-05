package com.example.ordersgeneratorapp.processes

import java.io.File

object AlpacaProcessor {
    fun watchOrders() {
        val orderDir = File("/data/data/com.example.ordersgeneratorapp/files/orders")
        orderDir.mkdirs()
        while (true) {
            orderDir.listFiles()?.forEach { file ->
                val text = file.readText()
                println("Alpaca Order: $text")
                file.delete()
            }
            Thread.sleep(2000)
        }
    }
}
