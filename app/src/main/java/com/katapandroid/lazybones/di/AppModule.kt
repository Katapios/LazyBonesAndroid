package com.katapandroid.lazybones.di

import android.app.Application
import androidx.room.Room
import com.katapandroid.lazybones.data.*
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            LazyBonesDatabase::class.java,
            "lazybones_db"
        ).build()
    }
    single { get<LazyBonesDatabase>().postDao() }
    single { get<LazyBonesDatabase>().voiceNoteDao() }
    single { PostRepository(get()) }
    single { VoiceNoteRepository(get()) }
} 