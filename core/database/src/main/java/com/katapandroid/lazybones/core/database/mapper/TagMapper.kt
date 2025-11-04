package com.katapandroid.lazybones.core.database.mapper

import com.katapandroid.lazybones.core.database.entity.TagEntity
import com.katapandroid.lazybones.core.domain.model.Tag
import com.katapandroid.lazybones.core.domain.model.TagType

private fun String.toTagType(): TagType = try {
    TagType.valueOf(this)
} catch (_: IllegalArgumentException) {
    TagType.GOOD
}

fun TagEntity.toDomain(): Tag = Tag(
    id = id,
    text = text,
    type = type.toTagType()
)

fun Tag.toEntity(): TagEntity = TagEntity(
    id = id,
    text = text,
    type = type.name
)
