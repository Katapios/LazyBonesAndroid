package com.katapandroid.lazybones.core.domain.repository

import com.katapandroid.lazybones.core.domain.model.VoiceNote
import kotlinx.coroutines.flow.Flow

interface VoiceNoteRepository {
    fun observeVoiceNotes(): Flow<List<VoiceNote>>
    suspend fun getVoiceNote(id: Long): VoiceNote?
    suspend fun insert(voiceNote: VoiceNote): Long
    suspend fun update(voiceNote: VoiceNote)
    suspend fun delete(voiceNote: VoiceNote)
}
