package com.katapandroid.lazybones.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.util.Date

@Entity(tableName = "posts")
data class Post(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Date,
    val content: String,
    val checklist: List<String>,
    val voiceNotes: List<Long>, // id-шники VoiceNote
    val published: Boolean = false,
    val isDraft: Boolean = true, // Черновик (не показывается в отчетах до сохранения)
    val goodItems: List<String> = emptyList(), // Список good пунктов
    val badItems: List<String> = emptyList(),  // Список bad пунктов
    val goodCount: Int = 0,
    val badCount: Int = 0
) 