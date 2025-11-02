package com.katapandroid.lazybones.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katapandroid.lazybones.data.PostRepository
import com.katapandroid.lazybones.data.SettingsRepository
import com.katapandroid.lazybones.data.TimePoolManager
import com.katapandroid.lazybones.data.PoolStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.*

class MainViewModel(
    private val postRepository: PostRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val timePoolManager = TimePoolManager(settingsRepository)
    
    private val _goodCount = MutableStateFlow(0)
    val goodCount: StateFlow<Int> = _goodCount.asStateFlow()

    private val _badCount = MutableStateFlow(0)
    val badCount: StateFlow<Int> = _badCount.asStateFlow()

    private val _reportStatus = MutableStateFlow(ReportStatus.NOT_FILLED)
    val reportStatus: StateFlow<ReportStatus> = _reportStatus.asStateFlow()
    
    private val _poolStatus = MutableStateFlow<PoolStatus>(PoolStatus.ACTIVE)
    val poolStatus: StateFlow<PoolStatus> = _poolStatus.asStateFlow()
    
    private val _timerText = MutableStateFlow("")
    val timerText: StateFlow<String> = _timerText.asStateFlow()
    
    private val _canCreateReport = MutableStateFlow(false)
    val canCreateReport: StateFlow<Boolean> = _canCreateReport.asStateFlow()
    
    private val _canCreatePlan = MutableStateFlow(false)
    val canCreatePlan: StateFlow<Boolean> = _canCreatePlan.asStateFlow()

    init {
        // Отслеживаем изменения отчетов и настроек
        combine(
            postRepository.getAllPosts(),
            settingsRepository.unlockReportCreation,
            settingsRepository.unlockPlanCreation
        ) { posts, unlockReport, unlockPlan ->
            updateReportStatus(posts, unlockReport, unlockPlan)
        }.launchIn(viewModelScope)
        
        // Обновляем статус пула и таймер
        viewModelScope.launch {
            while (true) {
                updatePoolStatus()
                updateTimer()
                delay(1000) // Обновляем каждую секунду
            }
        }
    }
    
    private fun updateReportStatus(posts: List<com.katapandroid.lazybones.data.Post>, unlockReport: Boolean, unlockPlan: Boolean) {
        val (poolStart, poolEnd) = timePoolManager.getCurrentPoolRange()

        // Отчеты за текущий пул (только отчеты без checklist, с пунктами good/bad)
        val reportsInPool = posts.filter { post ->
            val postDate = post.date
            val isInPool = postDate >= poolStart && postDate <= poolEnd
            val noChecklist = post.checklist.isEmpty()
            val hasGoodOrBad = post.goodItems.isNotEmpty() || post.badItems.isNotEmpty()
            isInPool && noChecklist && hasGoodOrBad
        }

        val publishedReport = reportsInPool.firstOrNull { !it.isDraft && it.published }
        val savedReport = reportsInPool.firstOrNull { !it.isDraft && !it.published }
        val draftReport = reportsInPool.firstOrNull { it.isDraft }

        // План за текущий пул (посты с checklist)
        val todayPlan = posts.firstOrNull { post ->
            val postDate = post.date
            val isInPool = postDate >= poolStart && postDate <= poolEnd
            val hasChecklist = post.checklist.isNotEmpty()
            isInPool && hasChecklist
        }

        // Счетчики берем из приоритетного отчета: опубликованный -> сохраненный -> черновик
        val countersSource = publishedReport ?: savedReport ?: draftReport
        _goodCount.value = countersSource?.goodItems?.size ?: 0
        _badCount.value = countersSource?.badItems?.size ?: 0

        // Статус отчета по приоритету
        _reportStatus.value = when {
            publishedReport != null -> ReportStatus.PUBLISHED
            savedReport != null -> ReportStatus.SAVED
            draftReport != null -> ReportStatus.IN_PROGRESS
            else -> ReportStatus.NOT_FILLED
        }

        // Возможность создания отчета/плана
        val isInPoolTime = timePoolManager.isInPoolTime()
        val reportPublished = publishedReport != null
        val planPublished = todayPlan?.published == true

        _canCreateReport.value = (isInPoolTime && (!reportPublished || unlockReport))
        _canCreatePlan.value = (isInPoolTime && (!planPublished || unlockPlan))
    }
    
    private fun updatePoolStatus() {
        _poolStatus.value = timePoolManager.getPoolStatus()
    }
    
    private fun updateTimer() {
        val status = timePoolManager.getPoolStatus()
        val timeUntilStart = timePoolManager.getTimeUntilPoolStart()
        val timeUntilEnd = timePoolManager.getTimeUntilPoolEnd()
        
        _timerText.value = when (status) {
            PoolStatus.BEFORE_START -> {
                timeUntilStart?.let { formatTime(it) }?.let { "До начала пула: $it" } ?: ""
            }
            PoolStatus.ACTIVE -> {
                timeUntilEnd?.let { formatTime(it) }?.let { "До конца пула: $it" } ?: ""
            }
            PoolStatus.AFTER_END -> {
                timeUntilStart?.let { formatTime(it) }?.let { "До начала пула: $it" } ?: "Пул завершен"
            }
        }
    }
    
    private fun formatTime(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
}

enum class ReportStatus {
    NOT_FILLED,    // Отчет не заполнен
    IN_PROGRESS,   // Отчет заполняется (черновик)
    SAVED,         // Отчет сохранен
    PUBLISHED      // Отчет опубликован
} 