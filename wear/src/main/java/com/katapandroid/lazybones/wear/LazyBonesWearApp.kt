package com.katapandroid.lazybones.wear

import android.app.Application

class LazyBonesWearApp : Application() {
    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("LazyBonesWearApp", "✅ Wear app initialized with applicationId: ${packageName}")
    }
}
