package com.katapandroid.lazybones.core.domain.service

import com.katapandroid.lazybones.core.domain.model.PoolStatus
import com.katapandroid.lazybones.core.domain.repository.SettingsRepository
import java.util.Calendar
import java.util.Date

class TimePoolManager(
    private val settingsRepository: SettingsRepository
) {

    fun validatePoolSettings(startMinutes: Int, endMinutes: Int): Pair<Boolean, String?> {
        if (startMinutes >= endMinutes) {
            return false to "Время окончания должно быть позже времени начала"
        }
        if (endMinutes - startMinutes < MIN_POOL_LENGTH_MINUTES) {
            return false to "Минимальная длина пула — ${MIN_POOL_LENGTH_MINUTES / 60} час"
        }
        return true to null
    }

    fun getCurrentPoolRange(reference: Date = Date()): Pair<Date, Date> {
        val calendar = Calendar.getInstance().apply { time = reference }
        val start = calculateDateForMinutes(calendar, settingsRepository.getPoolStartMinutes())
        val endMinutes = settingsRepository.getPoolEndMinutes()
        val endCalendar = Calendar.getInstance().apply { time = start }
        if (endMinutes < settingsRepository.getPoolStartMinutes()) {
            endCalendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        endCalendar.set(Calendar.HOUR_OF_DAY, endMinutes / 60)
        endCalendar.set(Calendar.MINUTE, endMinutes % 60)
        endCalendar.set(Calendar.SECOND, 0)
        endCalendar.set(Calendar.MILLISECOND, 0)
        return start to endCalendar.time
    }

    fun getPoolStatus(reference: Date = Date()): PoolStatus {
        val (start, end) = getCurrentPoolRange(reference)
        return when {
            reference.before(start) -> PoolStatus.BEFORE_START
            reference.after(end) -> PoolStatus.AFTER_END
            else -> PoolStatus.ACTIVE
        }
    }

    fun getTimeUntilPoolStart(reference: Date = Date()): Long? {
        val (start, _) = getCurrentPoolRange(reference)
        val diff = start.time - reference.time
        return if (diff > 0) diff else null
    }

    fun getTimeUntilPoolEnd(reference: Date = Date()): Long? {
        val (_, end) = getCurrentPoolRange(reference)
        val diff = end.time - reference.time
        return if (diff > 0) diff else null
    }

    fun getPoolStartMinutes(): Int = settingsRepository.getPoolStartMinutes()

    fun getPoolEndMinutes(): Int = settingsRepository.getPoolEndMinutes()

    private fun calculateDateForMinutes(base: Calendar, minutes: Int): Date = Calendar.getInstance().apply {
        time = base.time
        set(Calendar.HOUR_OF_DAY, minutes / 60)
        set(Calendar.MINUTE, minutes % 60)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.time

    fun isInPoolTime(reference: Date = Date()): Boolean {
        val (start, end) = getCurrentPoolRange(reference)
        return reference >= start && reference <= end
    }

    companion object {
        private const val MIN_POOL_LENGTH_MINUTES = 60
    }
}
