#!/bin/bash

# Определяем ID часов (первый подключенный)
WATCH_ID=$(adb devices | grep -E "adb-RFAX8039ZXE" | awk '{print $1}' | head -1)
PHONE_ID=$(adb devices | grep -v "adb-RFAX8039ZXE" | grep "device" | grep -v "List" | awk '{print $1}' | head -1)

echo "========================================="
echo "Проверка синхронизации LazyBones"
echo "========================================="
echo "Часы: $WATCH_ID"
echo "Телефон: $PHONE_ID"
echo "========================================="
echo ""

if [ -z "$WATCH_ID" ]; then
    echo "❌ Часы не найдены"
    exit 1
fi

echo "📱 Логи отправки с телефона (нажмите Ctrl+C для остановки):"
echo ""

# Если есть ID телефона, логируем с него, иначе со всех устройств
if [ -n "$PHONE_ID" ]; then
    adb -s "$PHONE_ID" logcat -c
    adb -s "$PHONE_ID" logcat | grep --line-buffered -E "(WearDataSync|MainViewModel.*sync)"
else
    echo "⚠️ Телефон не определен, показываем все логи..."
    adb logcat -c
    adb logcat | grep --line-buffered -E "(WearDataSync|MainViewModel.*sync)"
fi

