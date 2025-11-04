package com.katapandroid.lazybones.core.database.mapper

import com.katapandroid.lazybones.core.database.entity.VoiceNoteEntity
import com.katapandroid.lazybones.core.domain.model.VoiceNote

fun VoiceNoteEntity.toDomain(): VoiceNote = VoiceNote(
    id = id,
    filePath = filePath,
    createdAt = createdAt,
    duration = duration
)

fun VoiceNote.toEntity(): VoiceNoteEntity = VoiceNoteEntity(
    id = id,
    filePath = filePath,
    createdAt = createdAt,
    duration = duration
)
