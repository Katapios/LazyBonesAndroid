package com.katapandroid.lazybones.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tags")
data class Tag(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val type: TagType
)

enum class TagType { GOOD, BAD } 