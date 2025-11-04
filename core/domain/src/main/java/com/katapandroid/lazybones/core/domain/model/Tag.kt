package com.katapandroid.lazybones.core.domain.model

data class Tag(
    val id: Long = 0,
    val text: String,
    val type: TagType
)

enum class TagType { GOOD, BAD }
