package com.katapandroid.lazybones.di

import android.app.Application
import androidx.room.Room
import com.katapandroid.lazybones.data.*
import com.katapandroid.lazybones.ui.MainViewModel
import com.katapandroid.lazybones.ui.ReportsViewModel
import com.katapandroid.lazybones.ui.ReportFormViewModel
import com.katapandroid.lazybones.ui.VoiceNotesViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single {
        Room.databaseBuilder(
            get<Application>(), // Исправлено: используем Application
            LazyBonesDatabase::class.java,
            "lazybones_db"
        ).build()
    }
    single { get<LazyBonesDatabase>().postDao() }
    single { get<LazyBonesDatabase>().voiceNoteDao() }
    single { PostRepository(get()) }
    single { VoiceNoteRepository(get()) }
    viewModel { MainViewModel(get()) }
    viewModel { ReportsViewModel(get()) }
    viewModel { ReportFormViewModel(get()) }
    viewModel { VoiceNotesViewModel(get()) }
} 