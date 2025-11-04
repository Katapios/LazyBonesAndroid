package com.katapandroid.lazybones.core.notification

import android.content.Context
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.katapandroid.lazybones.core.domain.service.NotificationScheduler
import java.util.concurrent.TimeUnit

class NotificationSchedulerImpl(
    private val context: Context
) : NotificationScheduler {

    override fun scheduleNotifications() {
        try {
            // Placeholder worker; real implementation should schedule actual reminder worker
            val workRequest = PeriodicWorkRequestBuilder<PlaceholderNotificationWorker>(
                12, TimeUnit.HOURS
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        } catch (exception: Exception) {
            Log.e("NotificationScheduler", "Failed to schedule notifications", exception)
        }
    }

    companion object {
        private const val WORK_NAME = "lazybones-notification-work"
    }
}
