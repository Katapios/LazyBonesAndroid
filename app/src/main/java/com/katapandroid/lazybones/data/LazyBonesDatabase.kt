package com.katapandroid.lazybones.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Post::class, VoiceNote::class, PlanItem::class, Tag::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class LazyBonesDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao
    abstract fun voiceNoteDao(): VoiceNoteDao
    abstract fun planItemDao(): PlanItemDao
    abstract fun tagDao(): TagDao
} 