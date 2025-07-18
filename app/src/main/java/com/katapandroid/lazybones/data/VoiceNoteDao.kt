package com.katapandroid.lazybones.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceNoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(voiceNote: VoiceNote): Long

    @Update
    suspend fun update(voiceNote: VoiceNote)

    @Delete
    suspend fun delete(voiceNote: VoiceNote)

    @Query("SELECT * FROM voice_notes ORDER BY createdAt DESC")
    fun getAllVoiceNotes(): Flow<List<VoiceNote>>

    @Query("SELECT * FROM voice_notes WHERE id = :id LIMIT 1")
    suspend fun getVoiceNoteById(id: Long): VoiceNote?
} 