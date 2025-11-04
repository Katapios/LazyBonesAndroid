package com.katapandroid.lazybones.core.database.repository

import com.katapandroid.lazybones.core.database.dao.PlanItemDao
import com.katapandroid.lazybones.core.database.mapper.toDomain
import com.katapandroid.lazybones.core.database.mapper.toEntity
import com.katapandroid.lazybones.core.domain.model.PlanItem
import com.katapandroid.lazybones.core.domain.repository.PlanItemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PlanItemRepositoryImpl(
    private val planItemDao: PlanItemDao
) : PlanItemRepository {

    override fun observePlanItems(): Flow<List<PlanItem>> =
        planItemDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getPlanItems(): List<PlanItem> =
        planItemDao.getAll().map { it.toDomain() }

    override suspend fun insert(item: PlanItem): Long =
        planItemDao.insert(item.toEntity())

    override suspend fun update(item: PlanItem) {
        planItemDao.update(item.toEntity())
    }

    override suspend fun delete(item: PlanItem) {
        planItemDao.delete(item.toEntity())
    }

    override suspend fun clear() {
        planItemDao.clear()
    }
}
