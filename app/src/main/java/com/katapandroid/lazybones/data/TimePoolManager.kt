package com.katapandroid.lazybones.data

import java.util.Calendar
import java.util.Date

class TimePoolManager(private val settingsRepository: SettingsRepository) {

    fun getCurrentPoolRange(): Pair<Date, Date> {
        val startMinutes = settingsRepository.getPoolStartMinutes()
        val endMinutes = settingsRepository.getPoolEndMinutes()

        val calendar = Calendar.getInstance()
        val start = calendar.clone() as Calendar
        start.set(Calendar.HOUR_OF_DAY, startMinutes / 60)
        start.set(Calendar.MINUTE, startMinutes % 60)
        start.set(Calendar.SECOND, 0)
        start.set(Calendar.MILLISECOND, 0)

        val end = calendar.clone() as Calendar
        end.set(Calendar.HOUR_OF_DAY, endMinutes / 60)
        end.set(Calendar.MINUTE, endMinutes % 60)
        end.set(Calendar.SECOND, 0)
        end.set(Calendar.MILLISECOND, 0)

        // Защитимся от некорректной конфигурации: если конец <= начала, сдвинем конец на следующий день
        if (end.timeInMillis <= start.timeInMillis) {
            end.add(Calendar.DAY_OF_YEAR, 1)
        }

        return start.time to end.time
    }

    fun getPoolStatus(): PoolStatus {
        val now = System.currentTimeMillis()
        val (start, end) = getCurrentPoolRange()
        return when {
            now < start.time -> PoolStatus.BEFORE_START
            now in start.time..end.time -> PoolStatus.ACTIVE
            else -> PoolStatus.AFTER_END
        }
    }

    fun isInPoolTime(): Boolean = getPoolStatus() == PoolStatus.ACTIVE

    fun getTimeUntilPoolStart(): Long? {
        val now = System.currentTimeMillis()
        val (start, end) = getCurrentPoolRange()
        return when {
            now < start.time -> start.time - now
            now <= end.time -> 0L
            else -> {
                val cal = Calendar.getInstance()
                cal.time = start
                cal.add(Calendar.DAY_OF_YEAR, 1)
                cal.timeInMillis - now
            }
        }
    }

    fun getTimeUntilPoolEnd(): Long? {
        val now = System.currentTimeMillis()
        val (_, end) = getCurrentPoolRange()
        return if (now <= end.time) end.time - now else null
    }

    fun validatePoolSettings(startMinutes: Int, endMinutes: Int): Pair<Boolean, String?> {
        // Ограничения из UI: 06:00 (360) - 23:00 (1380), длительность ≤ 12 часов
        if (startMinutes < 360) return false to "Начало не раньше 06:00"
        if (endMinutes > 1380) return false to "Конец не позже 23:00"
        if (endMinutes <= startMinutes) return false to "Конец должен быть позже начала"
        val duration = endMinutes - startMinutes
        if (duration > 720) return false to "Длительность пула не более 12 часов"
        return true to null
    }
}


