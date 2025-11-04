package com.katapandroid.lazybones.core.database.repository

import com.katapandroid.lazybones.core.database.dao.PostDao
import com.katapandroid.lazybones.core.database.mapper.toDomain
import com.katapandroid.lazybones.core.database.mapper.toEntity
import com.katapandroid.lazybones.core.domain.model.Post
import com.katapandroid.lazybones.core.domain.repository.PostRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date

class PostRepositoryImpl(
    private val postDao: PostDao
) : PostRepository {

    override fun observePosts(): Flow<List<Post>> =
        postDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeUnpublishedPosts(): Flow<List<Post>> =
        postDao.observeUnpublished().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getPosts(): List<Post> =
        postDao.getAll().map { it.toDomain() }

    override suspend fun getPostByDate(date: Date): Post? =
        postDao.findByDate(date)?.toDomain()

    override suspend fun insert(post: Post): Long =
        postDao.insert(post.toEntity())

    override suspend fun update(post: Post) {
        postDao.update(post.toEntity())
    }

    override suspend fun delete(post: Post) {
        postDao.delete(post.toEntity())
    }
}
