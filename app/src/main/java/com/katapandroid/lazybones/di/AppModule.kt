package com.katapandroid.lazybones.di

import com.katapandroid.lazybones.core.database.di.databaseModule
import com.katapandroid.lazybones.core.network.di.networkModule
import com.katapandroid.lazybones.core.notification.di.notificationModule
import com.katapandroid.lazybones.core.preferences.di.preferencesModule
import com.katapandroid.lazybones.feature.home.di.homeModule
import com.katapandroid.lazybones.feature.plan.di.planModule
import com.katapandroid.lazybones.feature.reports.di.reportsModule
import org.koin.dsl.module

val legacyModule = module {
    // TODO: migrate remaining ViewModels (ReportForm, Settings, VoiceNotes) to feature modules
}

val appModules = listOf(
    databaseModule,
    preferencesModule,
    networkModule,
    notificationModule,
    planModule,
    reportsModule,
    homeModule,
    legacyModule
)