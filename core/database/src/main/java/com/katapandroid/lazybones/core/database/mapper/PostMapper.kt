package com.katapandroid.lazybones.core.database.mapper

import com.katapandroid.lazybones.core.database.entity.PostEntity
import com.katapandroid.lazybones.core.domain.model.Post

fun PostEntity.toDomain(): Post = Post(
    id = id,
    date = date,
    content = content,
    checklist = checklist,
    voiceNotes = voiceNotes,
    published = published,
    isDraft = isDraft,
    goodItems = goodItems,
    badItems = badItems,
    goodCount = goodCount,
    badCount = badCount
)

fun Post.toEntity(): PostEntity = PostEntity(
    id = id,
    date = date,
    content = content,
    checklist = checklist,
    voiceNotes = voiceNotes,
    published = published,
    isDraft = isDraft,
    goodItems = goodItems,
    badItems = badItems,
    goodCount = goodCount,
    badCount = badCount
)
