package com.katapandroid.lazybones

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.katapandroid.lazybones.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class PostRepositoryTest {
    private lateinit var db: LazyBonesDatabase
    private lateinit var postDao: PostDao
    private lateinit var repo: PostRepository

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LazyBonesDatabase::class.java
        ).allowMainThreadQueries().build()
        postDao = db.postDao()
        repo = PostRepository(postDao)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun testInsertAndGetPost() = runBlocking {
        val post = Post(
            date = Date(),
            content = "Test post",
            checklist = listOf("a", "b"),
            voiceNotes = listOf(),
            published = false,
            goodCount = 1,
            badCount = 2
        )
        val id = repo.insert(post)
        val allPosts = repo.getAllPosts().first()
        assertEquals(1, allPosts.size)
        assertEquals("Test post", allPosts[0].content)
    }

    @Test
    fun testUpdatePost() = runBlocking {
        val post = Post(
            date = Date(),
            content = "Initial",
            checklist = listOf(),
            voiceNotes = listOf(),
            published = false,
            goodCount = 0,
            badCount = 0
        )
        val id = repo.insert(post)
        val updated = post.copy(id = id, content = "Updated")
        repo.update(updated)
        val allPosts = repo.getAllPosts().first()
        assertEquals("Updated", allPosts[0].content)
    }

    @Test
    fun testDeletePost() = runBlocking {
        val post = Post(
            date = Date(),
            content = "ToDelete",
            checklist = listOf(),
            voiceNotes = listOf(),
            published = false,
            goodCount = 0,
            badCount = 0
        )
        val id = repo.insert(post)
        val inserted = post.copy(id = id)
        repo.delete(inserted)
        val allPosts = repo.getAllPosts().first()
        assertTrue(allPosts.isEmpty())
    }
} 