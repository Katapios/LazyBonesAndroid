package com.katapandroid.lazybones.data

import kotlinx.coroutines.flow.Flow

class PlanItemRepository(private val dao: PlanItemDao) {
    fun getAll(): Flow<List<PlanItem>> = dao.getAll()
    suspend fun insert(item: PlanItem): Long = dao.insert(item)
    suspend fun update(item: PlanItem) = dao.update(item)
    suspend fun delete(item: PlanItem) = dao.delete(item)
} 