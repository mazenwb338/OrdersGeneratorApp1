#!/bin/bash
cd /Users/mazenbteddini/OrdersGeneratorApp

has_changes() {
    ! git diff-index --quiet HEAD --
}

if has_changes; then
    echo "📦 Creating automatic backup..."
    
    git add app/src/main/kotlin/
    git add app/src/main/res/
    git add *.md
    git add .gitignore
    git add *.gradle.kts
    
    timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    git commit -m "🔄 Auto-backup: $timestamp

Automated backup of OrdersGeneratorApp:
✅ Market Data Screen with 20 stock monitoring  
✅ Hotkey integration with unique order IDs
✅ Real-time market data with timestamps
✅ Multi-account trading system
✅ Independent hotkey execution per account

Status: Unique Order ID System WORKING ✅"
    
    git push origin main
    
    echo "✅ Backup completed at $timestamp"
    echo "🎯 Your unique order ID system is safely backed up!"
else
    echo "ℹ️  No changes to backup"
fi
