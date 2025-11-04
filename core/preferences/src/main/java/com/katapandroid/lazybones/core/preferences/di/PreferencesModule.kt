package com.katapandroid.lazybones.core.preferences.di

import com.katapandroid.lazybones.core.domain.repository.SettingsRepository
import com.katapandroid.lazybones.core.preferences.SharedPreferencesSettingsRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val preferencesModule = module {
    single<SettingsRepository> { SharedPreferencesSettingsRepository(androidContext()) }
}
