#!/bin/bash
echo "📊 Monitoring OrdersGeneratorApp Order Execution..."
echo "   Watch for unique order IDs and hotkey execution"
echo "   Press Ctrl+C to stop monitoring"
echo ""

adb logcat | grep -E "(HotkeyOrderProcessor|MarketDataScreen|ALPACA_ORDER_ID|ACCOUNT_ORDER_SUCCESS|executeQuickTrade|Quick trade)"
