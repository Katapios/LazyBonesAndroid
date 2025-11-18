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
import kotlinx.coroutines.isActive
import java.util.*

class MainViewModel(
    private val postRepository: PostRepository,
    private val settingsRepository: SettingsRepository,
    private val application: android.app.Application,
    private val planItemRepository: com.katapandroid.lazybones.data.PlanItemRepository
) : ViewModel() {
    private val timePoolManager = TimePoolManager(settingsRepository)
    private val wearSyncService = com.katapandroid.lazybones.sync.WearDataSyncService(application)
    
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
    
    private val _motivationalSlogan = MutableStateFlow("")
    val motivationalSlogan: StateFlow<String> = _motivationalSlogan.asStateFlow()

    init {
        // Отслеживаем изменения отчетов и настроек
        combine(
            postRepository.getAllPosts(),
            settingsRepository.unlockReportCreation,
            settingsRepository.unlockPlanCreation,
            planItemRepository.getAll()
        ) { posts, unlockReport, unlockPlan, plans ->
            updateReportStatus(posts, unlockReport, unlockPlan, plans)
        }.launchIn(viewModelScope)
        
        // Обновляем статус пула и таймер
        viewModelScope.launch {
            // Сначала инициализируем статус и таймер
            updatePoolStatus()
            updateTimer()
            // Инициализируем мотивационный лозунг
            val initialPlans = try {
                planItemRepository.getAllSync()
            } catch (e: Exception) {
                emptyList()
            }
            updateMotivationalSlogan(initialPlans)
            
            // Отправляем тестовые данные сразу при запуске
            delay(1000)
            syncDataToWear()
            
            // Отправляем еще раз через 3 секунды для надежности
            delay(2000)
            syncDataToWear()
            
            // Периодически обновляем статус, таймер и синхронизируем
            var syncCounter = 0
            while (isActive) {
                delay(1000) // Обновляем каждую секунду
                updatePoolStatus()
                updateTimer()
                
                // Синхронизируем каждые 3 секунды
                syncCounter++
                if (syncCounter >= 3) {
                    syncDataToWear()
                    syncCounter = 0
                }
            }
        }
    }
    
    private fun updateReportStatus(posts: List<com.katapandroid.lazybones.data.Post>, unlockReport: Boolean, unlockPlan: Boolean, plans: List<com.katapandroid.lazybones.data.PlanItem>) {
        val (poolStart, poolEnd) = timePoolManager.getCurrentPoolRange()

        val analysis = ReportStatusAnalyzer.analyze(posts, poolStart, poolEnd)
        val newGoodCount = analysis.goodCount
        val newBadCount = analysis.badCount
        
        _goodCount.value = newGoodCount
        _badCount.value = newBadCount
        
        // Синхронизируем все данные с часами
        val goodItemsList = (analysis.publishedReport ?: analysis.savedReport ?: analysis.draftReport)
            ?.goodItems ?: emptyList()
        val badItemsList = (analysis.publishedReport ?: analysis.savedReport ?: analysis.draftReport)
            ?.badItems ?: emptyList()
        
        // Отправляем синхронизацию сразу после обновления статуса
        viewModelScope.launch {
            // Обновляем таймер перед отправкой
            updateTimer()
            
            // Получаем планы и отчёты
            val plansForSync = try {
                planItemRepository.getAllSync()
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Error getting plans", e)
                emptyList()
            }
            
            val allReports = try {
                postRepository.getAllPostsSync().filter { !it.isDraft }.sortedByDescending { it.date.time }
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Error getting reports", e)
                emptyList()
            }
            
            android.util.Log.d("MainViewModel", "📤 Syncing plans=${plansForSync.size}, reports=${allReports.size}")
            
            val planPostsForSync = try {
                postRepository.getAllPostsSync().filter { !it.isDraft && it.checklist.isNotEmpty() }
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Error getting plan posts", e)
                emptyList()
            }
            
            wearSyncService.syncAllData(
                newGoodCount,
                newBadCount,
                _reportStatus.value.name,
                _poolStatus.value.name,
                _timerText.value,
                goodItemsList,
                badItemsList,
                plansForSync,
                allReports,
                planPostsForSync,
                _motivationalSlogan.value
            )
        }

        // Статус отчета по приоритету
        _reportStatus.value = when {
            analysis.publishedReport != null -> ReportStatus.PUBLISHED
            analysis.savedReport != null -> ReportStatus.SAVED
            analysis.draftReport != null -> ReportStatus.IN_PROGRESS
            else -> ReportStatus.NOT_FILLED
        }

        // Возможность создания отчета/плана
        val isInPoolTime = timePoolManager.isInPoolTime()
        val reportPublished = analysis.publishedReport != null
        val planPublished = analysis.planPost?.published == true

        _canCreateReport.value = (isInPoolTime && (!reportPublished || unlockReport))
        _canCreatePlan.value = (isInPoolTime && (!planPublished || unlockPlan))
        
        // Обновляем мотивационный лозунг
        updateMotivationalSlogan(plans)
    }
    
    private fun updatePoolStatus() {
        val newStatus = timePoolManager.getPoolStatus()
        if (_poolStatus.value != newStatus) {
            _poolStatus.value = newStatus
            // Обновляем мотивационный лозунг при изменении статуса пула
            viewModelScope.launch {
                val plansForSlogan = try {
                    planItemRepository.getAllSync()
                } catch (e: Exception) {
                    emptyList()
                }
                updateMotivationalSlogan(plansForSlogan)
            }
            // Синхронизируем при изменении статуса пула
            syncDataToWear()
        }
    }
    
    private fun updateMotivationalSlogan(plans: List<com.katapandroid.lazybones.data.PlanItem>) {
        val slogan = MotivationalSlogan.getSlogan(
            poolStatus = _poolStatus.value,
            plans = plans,
            goodCount = _goodCount.value,
            badCount = _badCount.value
        )
        _motivationalSlogan.value = slogan
    }
    
    private fun updateTimer() {
        val status = timePoolManager.getPoolStatus()
        val timeUntilStart = timePoolManager.getTimeUntilPoolStart()
        val timeUntilEnd = timePoolManager.getTimeUntilPoolEnd()
        
        val newTimerText = when (status) {
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
        
        _timerText.value = newTimerText
    }
    
    private fun syncDataToWear() {
        viewModelScope.launch {
            try {
                val posts = postRepository.getAllPostsSync()
                val (poolStart, poolEnd) = timePoolManager.getCurrentPoolRange()
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
                val countersSource = publishedReport ?: savedReport ?: draftReport
                
                val goodItemsList = countersSource?.goodItems ?: emptyList()
                val badItemsList = countersSource?.badItems ?: emptyList()
                val newGoodCount = goodItemsList.size
                val newBadCount = badItemsList.size
                
                // Убеждаемся, что таймер обновлен - обновляем его перед отправкой
                updateTimer()
                val currentTimerText = _timerText.value.takeIf { it.isNotEmpty() } ?: run {
                    // Если таймер пустой, вычисляем его заново
                    val status = timePoolManager.getPoolStatus()
                    val timeUntilStart = timePoolManager.getTimeUntilPoolStart()
                    val timeUntilEnd = timePoolManager.getTimeUntilPoolEnd()
                    when (status) {
                        PoolStatus.BEFORE_START -> {
                            timeUntilStart?.let { formatTime(it) }?.let { "До начала пула: $it" } ?: "Ожидание..."
                        }
                        PoolStatus.ACTIVE -> {
                            timeUntilEnd?.let { formatTime(it) }?.let { "До конца пула: $it" } ?: "Пул активен"
                        }
                        PoolStatus.AFTER_END -> {
                            timeUntilStart?.let { formatTime(it) }?.let { "До начала пула: $it" } ?: "Пул завершен"
                        }
                    }
                }
                val currentStatus = _reportStatus.value.name
                val currentPool = _poolStatus.value.name
                
                // Получаем планы - это Post с checklist (не черновики, где есть checklist)
                val plans = try {
                    val allPosts = postRepository.getAllPostsSync()
                    // Планы - это Post с непустым checklist, не черновики
                    val planPosts = allPosts.filter { !it.isDraft && it.checklist.isNotEmpty() }
                    android.util.Log.d("MainViewModel", "📋 Found ${planPosts.size} plan posts from ${allPosts.size} total posts")
                    
                    // Конвертируем Post в PlanItem для синхронизации
                    // Каждый пункт checklist становится отдельным PlanItem с датой из Post
                    val plansList = planPosts.flatMap { post ->
                        post.checklist.mapIndexed { index, checklistItem ->
                            com.katapandroid.lazybones.data.PlanItem(
                                id = post.id * 1000 + index, // Уникальный ID для каждого пункта
                                text = checklistItem
                            )
                        }
                    }
                    
                    android.util.Log.d("MainViewModel", "📋 Got ${plansList.size} plan items from ${planPosts.size} plan posts")
                    if (plansList.isNotEmpty()) {
                        android.util.Log.d("MainViewModel", "📋 Plans: ${plansList.take(3).map { "id=${it.id}, text='${it.text.take(20)}...'" }}")
                    } else {
                        android.util.Log.w("MainViewModel", "⚠️ Plans list is empty! Total posts: ${allPosts.size}")
                    }
                    plansList
                } catch (e: Exception) {
                    android.util.Log.e("MainViewModel", "❌ Error getting plans", e)
                    e.printStackTrace()
                    emptyList()
                }
                
                val allReports = try {
                    postRepository.getAllPostsSync().filter { !it.isDraft }.sortedByDescending { it.date.time }
                } catch (e: Exception) {
                    android.util.Log.e("MainViewModel", "Error getting reports", e)
                    emptyList()
                }

                android.util.Log.d("MainViewModel", "📤 Syncing to wear: good=$newGoodCount, bad=$newBadCount, status=$currentStatus, pool=$currentPool, timer=$currentTimerText, plans=${plans.size}, reports=${allReports.size}")

                // Получаем все Post для передачи дат планов
                val allPostsForPlans = try {
                    postRepository.getAllPostsSync().filter { !it.isDraft && it.checklist.isNotEmpty() }
                } catch (e: Exception) {
                    android.util.Log.e("MainViewModel", "Error getting posts for plans", e)
                    emptyList()
                }

                wearSyncService.syncAllData(
                    newGoodCount,
                    newBadCount,
                    currentStatus,
                    currentPool,
                    currentTimerText,
                    goodItemsList,
                    badItemsList,
                    plans,
                    allReports,
                    allPostsForPlans, // Передаем Post для получения дат
                    _motivationalSlogan.value
                )
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Error syncing to wear", e)
                e.printStackTrace()
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