package com.example.ordersgeneratorapp.components

import androidx.compose.runtime.Composable

// This component is disabled - hotkeys only
@Composable
fun QuickTradeButtons(
    alpacaRepository: Any,
    presets: List<Any>,
    accounts: List<Any>,
    enableDirectButtons: Boolean = false,
    onMessage: (String) -> Unit = {}
) {
    // Buttons completely disabled - only hotkeys should work
}