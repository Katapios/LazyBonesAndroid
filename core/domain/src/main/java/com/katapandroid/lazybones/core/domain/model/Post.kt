package com.katapandroid.lazybones.core.domain.model

import java.util.Date

data class Post(
    val id: Long = 0,
    val date: Date,
    val content: String,
    val checklist: List<String>,
    val voiceNotes: List<Long>,
    val published: Boolean = false,
    val isDraft: Boolean = true,
    val goodItems: List<String> = emptyList(),
    val badItems: List<String> = emptyList(),
    val goodCount: Int = 0,
    val badCount: Int = 0
)
