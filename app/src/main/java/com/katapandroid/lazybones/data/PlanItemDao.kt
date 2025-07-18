package com.katapandroid.lazybones.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PlanItem): Long

    @Update
    suspend fun update(item: PlanItem)

    @Delete
    suspend fun delete(item: PlanItem)

    @Query("SELECT * FROM plan_items ORDER BY id ASC")
    fun getAll(): Flow<List<PlanItem>>
} 