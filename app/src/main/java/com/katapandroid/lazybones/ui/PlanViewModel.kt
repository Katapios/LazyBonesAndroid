package com.katapandroid.lazybones.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katapandroid.lazybones.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class PlanViewModel(
    private val planItemRepository: PlanItemRepository,
    private val tagRepository: TagRepository
) : ViewModel() {
    private val _planItems = MutableStateFlow<List<PlanItem>>(emptyList())
    val planItems: StateFlow<List<PlanItem>> = _planItems.asStateFlow()

    private val _goodTags = MutableStateFlow<List<Tag>>(emptyList())
    val goodTags: StateFlow<List<Tag>> = _goodTags.asStateFlow()

    private val _badTags = MutableStateFlow<List<Tag>>(emptyList())
    val badTags: StateFlow<List<Tag>> = _badTags.asStateFlow()

    init {
        planItemRepository.getAll().onEach { _planItems.value = it }.launchIn(viewModelScope)
        tagRepository.getByType(TagType.GOOD).onEach { _goodTags.value = it }.launchIn(viewModelScope)
        tagRepository.getByType(TagType.BAD).onEach { _badTags.value = it }.launchIn(viewModelScope)
    }

    fun addPlanItem(text: String) = viewModelScope.launch {
        planItemRepository.insert(PlanItem(text = text))
    }
    fun updatePlanItem(item: PlanItem) = viewModelScope.launch {
        planItemRepository.update(item)
    }
    fun deletePlanItem(item: PlanItem) = viewModelScope.launch {
        planItemRepository.delete(item)
    }
    
    suspend fun clearAllPlanItems() {
        try {
            planItemRepository.deleteAll()
        } catch (e: Exception) {
            android.util.Log.e("PlanViewModel", "Error clearing plan items", e)
            throw e
        }
    }

    fun addTag(text: String, type: TagType) = viewModelScope.launch {
        tagRepository.insert(Tag(text = text, type = type))
    }
    fun updateTag(tag: Tag) = viewModelScope.launch {
        tagRepository.update(tag)
    }
    fun deleteTag(tag: Tag) = viewModelScope.launch {
        tagRepository.delete(tag)
    }

    fun saveAsCustomReport(postRepository: PostRepository) = viewModelScope.launch {
        val checklist = planItems.value.map { it.text }
        val post = Post(
            date = java.util.Date(),
            content = "Кастомный отчет из планирования",
            checklist = checklist,
            voiceNotes = listOf(),
            published = false,
            goodItems = emptyList(),
            badItems = emptyList(),
            goodCount = 0,
            badCount = 0
        )
        postRepository.insert(post)
        
        // Очищаем все пункты плана после создания отчёта
        planItems.value.forEach { item ->
            planItemRepository.delete(item)
        }
    }
} 