package com.katapandroid.lazybones.core.domain.model

import java.util.Date

data class VoiceNote(
    val id: Long = 0,
    val filePath: String,
    val createdAt: Date,
    val duration: Long
)
