package com.katapandroid.lazybones.core.domain.repository

import com.katapandroid.lazybones.core.domain.model.Tag
import com.katapandroid.lazybones.core.domain.model.TagType
import kotlinx.coroutines.flow.Flow

interface TagRepository {
    fun observeTags(): Flow<List<Tag>>
    fun observeTagsByType(type: TagType): Flow<List<Tag>>
    suspend fun insert(tag: Tag): Long
    suspend fun update(tag: Tag)
    suspend fun delete(tag: Tag)
}
