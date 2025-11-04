package com.katapandroid.lazybones.data

import java.util.Calendar
import java.util.Date

enum class PoolStatus {
    BEFORE_START,
    ACTIVE,
    AFTER_END
}

data class PoolRange(val start: Date, val end: Date)

data class PoolReports(
    val published: Post?,
    val saved: Post?,
    val draft: Post?
) {
    val prioritized: Post?
        get() = published ?: saved ?: draft

    val isEmpty: Boolean
        get() = published == null && saved == null && draft == null
}

class TimePoolManager(
    private val settingsRepository: SettingsRepository,
    private val nowProvider: () -> Date = { Date() }
) {

    fun getCurrentPoolRange(referenceDate: Date = nowProvider()): PoolRange {
        val start = resolveCalendar(referenceDate, settingsRepository.getPoolStartMinutes())
        val end = resolveCalendar(referenceDate, settingsRepository.getPoolEndMinutes())

        if (!end.after(start)) {
            end.add(Calendar.DAY_OF_YEAR, 1)
        }

        return PoolRange(start.time, end.time)
    }

    fun getPoolStatus(referenceDate: Date = nowProvider()): PoolStatus {
        val range = getCurrentPoolRange(referenceDate)
        val now = referenceDate.time

        return when {
            now < range.start.time -> PoolStatus.BEFORE_START
            now <= range.end.time -> PoolStatus.ACTIVE
            else -> PoolStatus.AFTER_END
        }
    }

    fun isInPoolTime(referenceDate: Date = nowProvider()): Boolean =
        getPoolStatus(referenceDate) == PoolStatus.ACTIVE

    fun getTimeUntilPoolStart(referenceDate: Date = nowProvider()): Long? {
        val range = getCurrentPoolRange(referenceDate)
        val now = referenceDate.time

        if (now < range.start.time) {
            return range.start.time - now
        }

        val nextStart = Calendar.getInstance().apply {
            time = range.start
            add(Calendar.DAY_OF_YEAR, 1)
        }

        return nextStart.timeInMillis - now
    }

    fun getTimeUntilPoolEnd(referenceDate: Date = nowProvider()): Long? {
        val range = getCurrentPoolRange(referenceDate)
        val now = referenceDate.time

        return if (now in range.start.time..range.end.time) {
            range.end.time - now
        } else {
            null
        }
    }

    fun validatePoolSettings(startMinutes: Int, endMinutes: Int): Pair<Boolean, String?> {
        if (startMinutes !in 0 until MINUTES_IN_DAY || endMinutes !in 0 until MINUTES_IN_DAY) {
            return false to "Время должно быть в пределах суток"
        }

        if (endMinutes <= startMinutes) {
            return false to "Окончание должно быть позже начала"
        }

        val duration = endMinutes - startMinutes
        if (duration < MIN_POOL_DURATION_MINUTES) {
            return false to "Длительность пула должна быть не менее 1 часа"
        }

        return true to null
    }

    fun classifyReportsInCurrentPool(
        posts: List<Post>,
        referenceDate: Date = nowProvider()
    ): PoolReports {
        val range = getCurrentPoolRange(referenceDate)
        val reportsInRange = posts.filter { post ->
            post.isLocalReport() && post.isWithin(range)
        }

        return PoolReports(
            published = reportsInRange.firstOrNull { !it.isDraft && it.published },
            saved = reportsInRange.firstOrNull { !it.isDraft && !it.published },
            draft = reportsInRange.firstOrNull { it.isDraft }
        )
    }

    fun findPlanForCurrentPool(
        posts: List<Post>,
        referenceDate: Date = nowProvider()
    ): Post? {
        val range = getCurrentPoolRange(referenceDate)
        return posts.firstOrNull { post ->
            post.checklist.isNotEmpty() && post.isWithin(range)
        }
    }

    private fun Post.isWithin(range: PoolRange): Boolean =
        !date.before(range.start) && !date.after(range.end)

    private fun Post.isLocalReport(): Boolean =
        checklist.isEmpty() && (goodItems.isNotEmpty() || badItems.isNotEmpty())

    private fun resolveCalendar(referenceDate: Date, minutesFromMidnight: Int): Calendar {
        val calendar = Calendar.getInstance().apply {
            time = referenceDate
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.HOUR_OF_DAY, minutesFromMidnight / 60)
            set(Calendar.MINUTE, minutesFromMidnight % 60)
        }
        return calendar
    }

    companion object {
        private const val MINUTES_IN_DAY = 24 * 60
        private const val MIN_POOL_DURATION_MINUTES = 60
    }
}
