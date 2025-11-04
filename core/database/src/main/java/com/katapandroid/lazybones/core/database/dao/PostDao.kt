package com.katapandroid.lazybones.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.katapandroid.lazybones.core.database.entity.PostEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface PostDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(post: PostEntity): Long

    @Update
    suspend fun update(post: PostEntity)

    @Delete
    suspend fun delete(post: PostEntity)

    @Query("SELECT * FROM posts ORDER BY date DESC")
    fun observeAll(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE date = :date LIMIT 1")
    suspend fun findByDate(date: Date): PostEntity?

    @Query("SELECT * FROM posts WHERE published = 0 ORDER BY date DESC")
    fun observeUnpublished(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts ORDER BY date DESC")
    suspend fun getAll(): List<PostEntity>
}
