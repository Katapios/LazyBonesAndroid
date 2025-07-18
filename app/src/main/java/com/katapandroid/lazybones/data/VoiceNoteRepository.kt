package com.katapandroid.lazybones.data

import kotlinx.coroutines.flow.Flow

class VoiceNoteRepository(private val voiceNoteDao: VoiceNoteDao) {
    fun getAllVoiceNotes(): Flow<List<VoiceNote>> = voiceNoteDao.getAllVoiceNotes()
    suspend fun getVoiceNoteById(id: Long): VoiceNote? = voiceNoteDao.getVoiceNoteById(id)
    suspend fun insert(voiceNote: VoiceNote): Long = voiceNoteDao.insert(voiceNote)
    suspend fun update(voiceNote: VoiceNote) = voiceNoteDao.update(voiceNote)
    suspend fun delete(voiceNote: VoiceNote) = voiceNoteDao.delete(voiceNote)
} 