package com.katapandroid.lazybones.core.database.repository

import com.katapandroid.lazybones.core.database.dao.TagDao
import com.katapandroid.lazybones.core.database.mapper.toDomain
import com.katapandroid.lazybones.core.database.mapper.toEntity
import com.katapandroid.lazybones.core.domain.model.Tag
import com.katapandroid.lazybones.core.domain.model.TagType
import com.katapandroid.lazybones.core.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TagRepositoryImpl(
    private val tagDao: TagDao
) : TagRepository {

    override fun observeTags(): Flow<List<Tag>> =
        tagDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeTagsByType(type: TagType): Flow<List<Tag>> =
        tagDao.observeByType(type.name).map { entities -> entities.map { it.toDomain() } }

    override suspend fun insert(tag: Tag): Long =
        tagDao.insert(tag.toEntity())

    override suspend fun update(tag: Tag) {
        tagDao.update(tag.toEntity())
    }

    override suspend fun delete(tag: Tag) {
        tagDao.delete(tag.toEntity())
    }
}
