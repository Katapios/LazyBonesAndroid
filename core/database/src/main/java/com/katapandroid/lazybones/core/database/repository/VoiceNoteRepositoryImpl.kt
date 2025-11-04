package com.katapandroid.lazybones.core.database.repository

import com.katapandroid.lazybones.core.database.dao.VoiceNoteDao
import com.katapandroid.lazybones.core.database.mapper.toDomain
import com.katapandroid.lazybones.core.database.mapper.toEntity
import com.katapandroid.lazybones.core.domain.model.VoiceNote
import com.katapandroid.lazybones.core.domain.repository.VoiceNoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class VoiceNoteRepositoryImpl(
    private val voiceNoteDao: VoiceNoteDao
) : VoiceNoteRepository {

    override fun observeVoiceNotes(): Flow<List<VoiceNote>> =
        voiceNoteDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getVoiceNote(id: Long): VoiceNote? =
        voiceNoteDao.findById(id)?.toDomain()

    override suspend fun insert(voiceNote: VoiceNote): Long =
        voiceNoteDao.insert(voiceNote.toEntity())

    override suspend fun update(voiceNote: VoiceNote) {
        voiceNoteDao.update(voiceNote.toEntity())
    }

    override suspend fun delete(voiceNote: VoiceNote) {
        voiceNoteDao.delete(voiceNote.toEntity())
    }
}
