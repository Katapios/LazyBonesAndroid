package com.katapandroid.lazybones

import android.app.Application
import com.katapandroid.lazybones.di.appModule
import com.katapandroid.lazybones.notification.NotificationService
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class LazyBonesApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@LazyBonesApp)
            modules(appModule)
        }
        
        // Инициализируем расписание уведомлений при запуске приложения
        // Обертываем в try-catch, чтобы приложение не крашилось при отсутствии разрешений
        try {
            val settingsRepository = com.katapandroid.lazybones.data.SettingsRepository(this)
            if (settingsRepository.getNotificationsEnabled()) {
                val notificationService = NotificationService(this)
                notificationService.scheduleNotifications()
            }
        } catch (e: Exception) {
            android.util.Log.e("LazyBonesApp", "Error scheduling notifications on startup: ${e.message}", e)
            // Не крашим приложение, просто логируем ошибку
        }
    }
} 