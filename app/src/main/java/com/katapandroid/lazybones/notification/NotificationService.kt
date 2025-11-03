package com.katapandroid.lazybones.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.katapandroid.lazybones.data.SettingsRepository
import java.util.Calendar

class NotificationService(private val context: Context) {
    private val settings by lazy { SettingsRepository(context) }

    fun scheduleNotifications() {
        val enabled = settings.getNotificationsEnabled()
        if (!enabled) {
            cancelAllScheduled()
            return
        }

        val mode = settings.getNotificationMode() // 0 = hourly (UI shows 17-21), 1 = twice a day (12:00, 21:00)
        val times = if (mode == 0) {
            listOf(17 to 0, 18 to 0, 19 to 0, 20 to 0, 21 to 0)
        } else {
            listOf(12 to 0, 21 to 0)
        }

        // Сначала отменим уже запланированные на сегодня (по тем же requestCode)
        cancelAllScheduled()

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()

        times.forEachIndexed { index, (hour, minute) ->
            val triggerTime = Calendar.getInstance().apply {
                timeInMillis = now
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                // если время уже прошло сегодня — переносим на завтра
                if (timeInMillis <= now) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }.timeInMillis

            val pendingIntent = createAlarmIntent(requestCode = index)!!

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        }
    }

    private fun cancelAllScheduled() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // Мы используем фиксированные requestCode 0..4 для режима 0 и 0..1 для режима 1 — отменим диапазон 0..4 на всякий случай
        (0..4).forEach { code ->
            val pi = createAlarmIntent(requestCode = code, flags = PendingIntent.FLAG_NO_CREATE)
            if (pi != null) {
                alarmManager.cancel(pi)
                pi.cancel()
            }
        }
    }

    private fun createAlarmIntent(requestCode: Int, flags: Int? = null): PendingIntent? {
        val intent = Intent(context, NotificationReceiver::class.java)
        val finalFlags = (flags ?: (PendingIntent.FLAG_UPDATE_CURRENT)) or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, requestCode, intent, finalFlags)
    }
}


