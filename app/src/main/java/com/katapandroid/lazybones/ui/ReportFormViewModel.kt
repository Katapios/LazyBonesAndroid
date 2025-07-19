package com.katapandroid.lazybones.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katapandroid.lazybones.data.Post
import com.katapandroid.lazybones.data.PostRepository
import com.katapandroid.lazybones.data.TagRepository
import com.katapandroid.lazybones.data.TagType
import com.katapandroid.lazybones.data.Tag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import java.util.*
import androidx.compose.ui.text.input.TextFieldValue

class ReportFormViewModel(
    private val postRepository: PostRepository,
    private val tagRepository: TagRepository
) : ViewModel() {
    private val _content = MutableStateFlow("")
    val content: StateFlow<String> = _content.asStateFlow()

    private val _goodCount = MutableStateFlow(0)
    val goodCount: StateFlow<Int> = _goodCount.asStateFlow()

    private val _badCount = MutableStateFlow(0)
    val badCount: StateFlow<Int> = _badCount.asStateFlow()

    private val _goodTags = MutableStateFlow<List<Tag>>(emptyList())
    val goodTags: StateFlow<List<Tag>> = _goodTags.asStateFlow()
    private val _badTags = MutableStateFlow<List<Tag>>(emptyList())
    val badTags: StateFlow<List<Tag>> = _badTags.asStateFlow()

    // Состояния для выбранных пунктов и полей
    private val _selectedGoodTags = MutableStateFlow<List<String>>(emptyList())
    val selectedGoodTags: StateFlow<List<String>> = _selectedGoodTags.asStateFlow()
    private val _selectedBadTags = MutableStateFlow<List<String>>(emptyList())
    val selectedBadTags: StateFlow<List<String>> = _selectedBadTags.asStateFlow()
    private val _goodFields = MutableStateFlow<Map<String, TextFieldValue>>(emptyMap())
    val goodFields: StateFlow<Map<String, TextFieldValue>> = _goodFields.asStateFlow()
    private val _badFields = MutableStateFlow<Map<String, TextFieldValue>>(emptyMap())
    val badFields: StateFlow<Map<String, TextFieldValue>> = _badFields.asStateFlow()

    init {
        tagRepository.getByType(TagType.GOOD).onEach { _goodTags.value = it }.launchIn(viewModelScope)
        tagRepository.getByType(TagType.BAD).onEach { _badTags.value = it }.launchIn(viewModelScope)
    }

    fun setContent(value: String) { _content.value = value }
    fun setGoodCount(value: Int) { _goodCount.value = value }
    fun setBadCount(value: Int) { _badCount.value = value }

    // Методы для обновления состояний
    fun setSelectedGoodTags(tags: List<String>) { _selectedGoodTags.value = tags }
    fun setSelectedBadTags(tags: List<String>) { _selectedBadTags.value = tags }
    fun setGoodFields(fields: Map<String, TextFieldValue>) { _goodFields.value = fields }
    fun setBadFields(fields: Map<String, TextFieldValue>) { _badFields.value = fields }

    fun addTag(text: String, type: TagType) {
        viewModelScope.launch {
            try {
                val tag = Tag(
                    text = text,
                    type = type
                )
                val result = tagRepository.insert(tag)
                println("Tag saved successfully with id: $result")
            } catch (e: Exception) {
                println("Error saving tag: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun saveReport(goodItems: List<String>, badItems: List<String>, onSaved: () -> Unit) {
        viewModelScope.launch {
            val post = Post(
                date = Date(),
                content = _content.value,
                checklist = emptyList(),
                voiceNotes = emptyList(),
                published = false,
                goodItems = goodItems,
                badItems = badItems,
                goodCount = goodItems.size,
                badCount = badItems.size
            )
            postRepository.insert(post)
            onSaved()
        }
    }

    fun save(onSaved: () -> Unit) {
        viewModelScope.launch {
            val post = Post(
                date = Date(),
                content = _content.value,
                checklist = emptyList(),
                voiceNotes = emptyList(),
                published = false,
                goodCount = _goodCount.value,
                badCount = _badCount.value
            )
            postRepository.insert(post)
            onSaved()
        }
    }
} 