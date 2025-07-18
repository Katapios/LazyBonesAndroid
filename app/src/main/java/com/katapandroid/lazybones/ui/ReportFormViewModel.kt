package com.katapandroid.lazybones.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katapandroid.lazybones.data.Post
import com.katapandroid.lazybones.data.PostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

class ReportFormViewModel(
    private val postRepository: PostRepository
) : ViewModel() {
    private val _content = MutableStateFlow("")
    val content: StateFlow<String> = _content.asStateFlow()

    private val _goodCount = MutableStateFlow(0)
    val goodCount: StateFlow<Int> = _goodCount.asStateFlow()

    private val _badCount = MutableStateFlow(0)
    val badCount: StateFlow<Int> = _badCount.asStateFlow()

    fun setContent(value: String) { _content.value = value }
    fun setGoodCount(value: Int) { _goodCount.value = value }
    fun setBadCount(value: Int) { _badCount.value = value }

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