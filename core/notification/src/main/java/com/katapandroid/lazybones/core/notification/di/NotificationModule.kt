package com.katapandroid.lazybones.core.notification.di

import com.katapandroid.lazybones.core.domain.service.NotificationScheduler
import com.katapandroid.lazybones.core.notification.NotificationSchedulerImpl
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val notificationModule = module {
    single<NotificationScheduler> { NotificationSchedulerImpl(androidContext()) }
}
