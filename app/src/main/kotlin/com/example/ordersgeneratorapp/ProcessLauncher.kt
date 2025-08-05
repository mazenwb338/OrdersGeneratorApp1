package com.example.ordersgeneratorapp

import com.example.ordersgeneratorapp.processes.AlpacaProcessor
import kotlin.concurrent.thread

object ProcessLauncher {
    fun startAll() {
        thread { AlpacaProcessor.watchOrders() }
    }
}
