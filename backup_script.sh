#!/bin/bash
cd /Users/mazenbteddini/OrdersGeneratorApp
git add .
git commit -m "Auto-backup: $(date '+%Y-%m-%d %H:%M:%S')"
git push origin main
