package com.katapandroid.lazybones.core.domain.repository

import com.katapandroid.lazybones.core.domain.model.Post
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface PostRepository {
    fun observePosts(): Flow<List<Post>>
    fun observeUnpublishedPosts(): Flow<List<Post>>
    suspend fun getPosts(): List<Post>
    suspend fun getPostByDate(date: Date): Post?
    suspend fun insert(post: Post): Long
    suspend fun update(post: Post)
    suspend fun delete(post: Post)
}
