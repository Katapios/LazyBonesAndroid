package com.katapandroid.lazybones.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katapandroid.lazybones.data.PostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.*

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
            // Фильтруем по сегодняшней дате (как в виджете)
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val todayStart = calendar.time
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            calendar.add(Calendar.MILLISECOND, -1)
            val todayEnd = calendar.time

            // Ищем только отчет на сегодня (good/bad items, без checklist)
            // Good/bad всегда берутся из отчета, а не из плана
            val todayReport = posts.firstOrNull { post ->
                val postDate = post.date
                val isToday = postDate >= todayStart && postDate <= todayEnd
                val noChecklist = post.checklist.isEmpty()
                val hasGoodOrBad = post.goodItems.isNotEmpty() || post.badItems.isNotEmpty()
                isToday && noChecklist && hasGoodOrBad
            }

            // Используем данные только из отчета (goodItems/badItems)
            _goodCount.value = todayReport?.goodCount ?: 0
            _badCount.value = todayReport?.badCount ?: 0
            
            // Статус отчета: если есть отчет на сегодня, значит IN_PROGRESS
            _reportStatus.value = when {
                todayReport != null -> ReportStatus.IN_PROGRESS
                else -> ReportStatus.NOT_STARTED
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