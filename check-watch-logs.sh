#!/bin/bash

WATCH_ID="adb-RFAX8039ZXE-bHMrjm._adb-tls-connect._tcp"

echo "📱 Логи приема на часах (нажмите Ctrl+C для остановки):"
echo ""

adb -s "$WATCH_ID" logcat -c
adb -s "$WATCH_ID" logcat | grep --line-buffered -E "(WearDataReceiver|MainActivity.*data)"

