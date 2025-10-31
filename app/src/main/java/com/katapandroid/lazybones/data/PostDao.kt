package com.katapandroid.lazybones.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface PostDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(post: Post): Long

    @Update
    suspend fun update(post: Post)

    @Delete
    suspend fun delete(post: Post)

    @Query("SELECT * FROM posts ORDER BY date DESC")
    fun getAllPosts(): Flow<List<Post>>

    @Query("SELECT * FROM posts WHERE date = :date LIMIT 1")
    suspend fun getPostByDate(date: Date): Post?

    @Query("SELECT * FROM posts WHERE published = 0 ORDER BY date DESC")
    fun getUnpublishedPosts(): Flow<List<Post>>
    
    @Query("SELECT * FROM posts ORDER BY date DESC")
    suspend fun getAllPostsSync(): List<Post>
} 