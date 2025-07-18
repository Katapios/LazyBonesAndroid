package com.katapandroid.lazybones

import android.app.Application
import com.katapandroid.lazybones.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class LazyBonesApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@LazyBonesApp)
            modules(appModule)
        }
    }
} 