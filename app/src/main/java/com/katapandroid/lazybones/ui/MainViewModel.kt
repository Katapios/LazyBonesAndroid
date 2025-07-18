package com.katapandroid.lazybones.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katapandroid.lazybones.data.PostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MainViewModel(
    private val postRepository: PostRepository
) : ViewModel() {
    private val _goodCount = MutableStateFlow(0)
    val goodCount: StateFlow<Int> = _goodCount.asStateFlow()

    private val _badCount = MutableStateFlow(0)
    val badCount: StateFlow<Int> = _badCount.asStateFlow()

    private val _reportStatus = MutableStateFlow(ReportStatus.NOT_STARTED)
    val reportStatus: StateFlow<ReportStatus> = _reportStatus.asStateFlow()

    init {
        postRepository.getAllPosts().onEach { posts ->
            val today = posts.firstOrNull { it.published.not() }
            _goodCount.value = today?.goodCount ?: 0
            _badCount.value = today?.badCount ?: 0
            _reportStatus.value = when {
                today == null -> ReportStatus.NOT_STARTED
                today.published -> ReportStatus.DONE
                else -> ReportStatus.IN_PROGRESS
            }
        }.launchIn(viewModelScope)
    }

    // Заглушка для таймера
    val timerProgress = MutableStateFlow(0.5f)
    val timerTimeText = MutableStateFlow("12:34:56")
}

enum class ReportStatus {
    NOT_STARTED, IN_PROGRESS, DONE
} 