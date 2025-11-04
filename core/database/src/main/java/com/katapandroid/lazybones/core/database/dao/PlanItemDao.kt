package com.katapandroid.lazybones.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.katapandroid.lazybones.core.database.entity.PlanItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PlanItemEntity): Long

    @Update
    suspend fun update(item: PlanItemEntity)

    @Delete
    suspend fun delete(item: PlanItemEntity)

    @Query("SELECT * FROM plan_items ORDER BY id ASC")
    fun observeAll(): Flow<List<PlanItemEntity>>

    @Query("SELECT * FROM plan_items ORDER BY id ASC")
    suspend fun getAll(): List<PlanItemEntity>

    @Query("DELETE FROM plan_items")
    suspend fun clear()
}
