package com.katapandroid.lazybones.core.domain.repository

import com.katapandroid.lazybones.core.domain.model.PlanItem
import kotlinx.coroutines.flow.Flow

interface PlanItemRepository {
    fun observePlanItems(): Flow<List<PlanItem>>
    suspend fun getPlanItems(): List<PlanItem>
    suspend fun insert(item: PlanItem): Long
    suspend fun update(item: PlanItem)
    suspend fun delete(item: PlanItem)
    suspend fun clear()
}
