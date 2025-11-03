# Wear OS модуль - LazyBones Watch

## Сборка и тестирование

### Вариант 1: Через Android Studio
1. Откройте проект в Android Studio
2. Выберите конфигурацию запуска **"Wear OS Debug"**
3. Подключите часы или запустите эмулятор Wear OS
4. Нажмите Run (▶️)

### Вариант 2: Через Gradle (командная строка)

#### Сборка APK:
```bash
./gradlew :wear:assembleDebug
```

#### Установка на устройство:
```bash
./gradlew :wear:installDebug
```

#### Быстрая сборка и установка:
```bash
chmod +x wear/build-watch.sh
./wear/build-watch.sh
```

### Вариант 3: Через ADB напрямую

#### Список устройств:
```bash
adb devices
```

#### Установка APK:
```bash
adb install -r wear/build/outputs/apk/debug/wear-debug.apk
```

#### Запуск приложения:
```bash
adb shell am start -n com.katapandroid.lazybones.wear/.MainActivity
```

## Создание эмулятора Wear OS

1. **Android Studio** → **Tools** → **Device Manager**
2. **Create Device**
3. Выберите категорию **Wear OS**
4. Выберите устройство (например, **Pixel Watch**)
5. Выберите системный образ (рекомендуется **Wear OS 4.0** или новее)
6. Настройте параметры и завершите создание

## Компоненты модуля

- ✅ **MainActivity** - Главное приложение для часов
- ✅ **WearWidgetProvider** - Виджет для часов
- ⏸️ **Watch Face** - Циферблат (временно отключен)

## Тестовые данные

Приложение показывает:
- **Good: 5** (зеленый цвет)
- **Bad: 2** (красный цвет)

## Проблемы и решения

### Устройство не обнаружено
- Проверьте подключение через USB
- Убедитесь, что включена отладка по USB на часах
- Попробуйте: `adb kill-server && adb start-server`

### Ошибки сборки
- Очистите проект: `./gradlew clean`
- Пересоберите: `./gradlew :wear:assembleDebug`

### Эмулятор не запускается
- Проверьте, что HAXM или Hypervisor включен
- Увеличьте память для эмулятора в настройках AVD

