#!/bin/bash

echo "=== Проверка синхронизации данных ==="
echo ""
echo "Запустите это скрипт после открытия приложений на телефоне и часах"
echo ""

echo "Логи синхронизации с телефона (WearDataSync):"
adb logcat -d | grep "WearDataSync" | tail -20

echo ""
echo "Логи приема данных на часах (WearDataListener):"
adb -s adb-RFAX8039ZXE-bHMrjm._adb-tls-connect._tcp logcat -d | grep "WearDataListener\|MainActivity" | tail -20

echo ""
echo "Подключенные устройства:"
adb devices

