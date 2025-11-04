package com.katapandroid.lazybones.core.database.di

import androidx.room.Room
import com.katapandroid.lazybones.core.database.LazyBonesDatabase
import com.katapandroid.lazybones.core.database.repository.PlanItemRepositoryImpl
import com.katapandroid.lazybones.core.database.repository.PostRepositoryImpl
import com.katapandroid.lazybones.core.database.repository.TagRepositoryImpl
import com.katapandroid.lazybones.core.database.repository.VoiceNoteRepositoryImpl
import com.katapandroid.lazybones.core.domain.repository.PlanItemRepository
import com.katapandroid.lazybones.core.domain.repository.PostRepository
import com.katapandroid.lazybones.core.domain.repository.TagRepository
import com.katapandroid.lazybones.core.domain.repository.VoiceNoteRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            LazyBonesDatabase::class.java,
            "lazybones_db"
        ).fallbackToDestructiveMigration()
            .build()
    }

    single { get<LazyBonesDatabase>().postDao() }
    single { get<LazyBonesDatabase>().voiceNoteDao() }
    single { get<LazyBonesDatabase>().planItemDao() }
    single { get<LazyBonesDatabase>().tagDao() }

    single<PostRepository> { PostRepositoryImpl(get()) }
    single<VoiceNoteRepository> { VoiceNoteRepositoryImpl(get()) }
    single<PlanItemRepository> { PlanItemRepositoryImpl(get()) }
    single<TagRepository> { TagRepositoryImpl(get()) }
}
