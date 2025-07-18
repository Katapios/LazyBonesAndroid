package com.katapandroid.lazybones.data

import kotlinx.coroutines.flow.Flow

class TagRepository(private val dao: TagDao) {
    fun getByType(type: TagType): Flow<List<Tag>> = dao.getByType(type)
    suspend fun insert(tag: Tag): Long = dao.insert(tag)
    suspend fun update(tag: Tag) = dao.update(tag)
    suspend fun delete(tag: Tag) = dao.delete(tag)
} 