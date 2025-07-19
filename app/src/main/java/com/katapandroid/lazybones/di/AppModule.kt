package com.katapandroid.lazybones.di

import android.app.Application
import androidx.room.Room
import com.katapandroid.lazybones.data.*
import com.katapandroid.lazybones.ui.MainViewModel
import com.katapandroid.lazybones.ui.ReportsViewModel
import com.katapandroid.lazybones.ui.ReportFormViewModel
import com.katapandroid.lazybones.ui.VoiceNotesViewModel
import com.katapandroid.lazybones.ui.PlanViewModel
import com.katapandroid.lazybones.ui.SettingsViewModel
import com.katapandroid.lazybones.network.TelegramService
import com.katapandroid.lazybones.data.SettingsRepository
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single {
        Room.databaseBuilder(
            get<Application>(), // Исправлено: используем Application
            LazyBonesDatabase::class.java,
            "lazybones_db"
        ).fallbackToDestructiveMigration()
         .build()
    }
    single { get<LazyBonesDatabase>().postDao() }
    single { get<LazyBonesDatabase>().voiceNoteDao() }
    single { get<LazyBonesDatabase>().planItemDao() }
    single { get<LazyBonesDatabase>().tagDao() }
    single { PostRepository(get()) }
    single { VoiceNoteRepository(get()) }
    single { PlanItemRepository(get()) }
    single { TagRepository(get()) }
    single { TelegramService() }
    single { SettingsRepository(get()) }
    viewModel { MainViewModel(get()) }
    viewModel { ReportsViewModel(get<PostRepository>(), get<TelegramService>()) }
    viewModel { ReportFormViewModel(get<PostRepository>(), get<TagRepository>()) }
    viewModel { VoiceNotesViewModel(get()) }
    viewModel { PlanViewModel(get<PlanItemRepository>(), get<TagRepository>()) }
    viewModel { SettingsViewModel(get<TelegramService>(), get<SettingsRepository>()) }
} 