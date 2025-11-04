package com.katapandroid.lazybones.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.katapandroid.lazybones.core.database.dao.PlanItemDao
import com.katapandroid.lazybones.core.database.dao.PostDao
import com.katapandroid.lazybones.core.database.dao.TagDao
import com.katapandroid.lazybones.core.database.dao.VoiceNoteDao
import com.katapandroid.lazybones.core.database.entity.PlanItemEntity
import com.katapandroid.lazybones.core.database.entity.PostEntity
import com.katapandroid.lazybones.core.database.entity.TagEntity
import com.katapandroid.lazybones.core.database.entity.VoiceNoteEntity

@Database(
    entities = [
        PostEntity::class,
        VoiceNoteEntity::class,
        PlanItemEntity::class,
        TagEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class LazyBonesDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao
    abstract fun voiceNoteDao(): VoiceNoteDao
    abstract fun planItemDao(): PlanItemDao
    abstract fun tagDao(): TagDao
}
