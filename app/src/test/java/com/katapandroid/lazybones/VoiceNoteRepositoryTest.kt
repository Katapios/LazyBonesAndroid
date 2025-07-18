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
class VoiceNoteRepositoryTest {
    private lateinit var db: LazyBonesDatabase
    private lateinit var voiceNoteDao: VoiceNoteDao
    private lateinit var repo: VoiceNoteRepository

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LazyBonesDatabase::class.java
        ).allowMainThreadQueries().build()
        voiceNoteDao = db.voiceNoteDao()
        repo = VoiceNoteRepository(voiceNoteDao)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun testInsertAndGetVoiceNote() = runBlocking {
        val note = VoiceNote(
            filePath = "/tmp/test.3gp",
            createdAt = Date(),
            duration = 12345L
        )
        val id = repo.insert(note)
        val allNotes = repo.getAllVoiceNotes().first()
        assertEquals(1, allNotes.size)
        assertEquals("/tmp/test.3gp", allNotes[0].filePath)
    }

    @Test
    fun testUpdateVoiceNote() = runBlocking {
        val note = VoiceNote(
            filePath = "/tmp/old.3gp",
            createdAt = Date(),
            duration = 1000L
        )
        val id = repo.insert(note)
        val updated = note.copy(id = id, filePath = "/tmp/new.3gp")
        repo.update(updated)
        val allNotes = repo.getAllVoiceNotes().first()
        assertEquals("/tmp/new.3gp", allNotes[0].filePath)
    }

    @Test
    fun testDeleteVoiceNote() = runBlocking {
        val note = VoiceNote(
            filePath = "/tmp/delete.3gp",
            createdAt = Date(),
            duration = 500L
        )
        val id = repo.insert(note)
        val inserted = note.copy(id = id)
        repo.delete(inserted)
        val allNotes = repo.getAllVoiceNotes().first()
        assertTrue(allNotes.isEmpty())
    }
} 