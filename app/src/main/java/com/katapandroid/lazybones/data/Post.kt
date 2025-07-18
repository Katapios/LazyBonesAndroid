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
    val goodCount: Int = 0,
    val badCount: Int = 0
) 