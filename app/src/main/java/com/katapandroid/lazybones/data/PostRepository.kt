package com.katapandroid.lazybones.data

import kotlinx.coroutines.flow.Flow
import java.util.Date

class PostRepository(private val postDao: PostDao) {
    fun getAllPosts(): Flow<List<Post>> = postDao.getAllPosts()
    fun getUnpublishedPosts(): Flow<List<Post>> = postDao.getUnpublishedPosts()
    suspend fun getAllPostsSync(): List<Post> = postDao.getAllPostsSync()
    suspend fun getPostByDate(date: Date): Post? = postDao.getPostByDate(date)
    suspend fun insert(post: Post): Long = postDao.insert(post)
    suspend fun update(post: Post) = postDao.update(post)
    suspend fun delete(post: Post) = postDao.delete(post)
} 