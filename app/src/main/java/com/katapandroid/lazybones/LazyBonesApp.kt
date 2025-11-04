package com.katapandroid.lazybones

import android.app.Application
import com.katapandroid.lazybones.core.domain.repository.SettingsRepository
import com.katapandroid.lazybones.core.domain.service.NotificationScheduler
import com.katapandroid.lazybones.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.android.get
import org.koin.core.context.startKoin

class LazyBonesApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@LazyBonesApp)
            modules(appModules)
        }

        // Инициализируем расписание уведомлений при запуске приложения
        // Обертываем в try-catch, чтобы приложение не крашилось при отсутствии разрешений
        try {
            val settingsRepository: SettingsRepository = get()
            val notificationScheduler: NotificationScheduler = get()
            if (settingsRepository.getNotificationsEnabled()) {
                notificationScheduler.scheduleNotifications()
            }
        } catch (e: Exception) {
            android.util.Log.e("LazyBonesApp", "Error scheduling notifications on startup: ${e.message}", e)
            // Не крашим приложение, просто логируем ошибку
        }
    }
} 