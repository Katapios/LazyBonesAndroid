package com.katapandroid.lazybones.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.katapandroid.lazybones.core.database.entity.VoiceNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceNoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(voiceNote: VoiceNoteEntity): Long

    @Update
    suspend fun update(voiceNote: VoiceNoteEntity)

    @Delete
    suspend fun delete(voiceNote: VoiceNoteEntity)

    @Query("SELECT * FROM voice_notes ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<VoiceNoteEntity>>

    @Query("SELECT * FROM voice_notes WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): VoiceNoteEntity?
}
