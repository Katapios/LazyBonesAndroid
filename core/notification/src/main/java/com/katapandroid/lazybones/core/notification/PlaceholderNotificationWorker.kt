package com.katapandroid.lazybones.core.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class PlaceholderNotificationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = Result.success()
}
