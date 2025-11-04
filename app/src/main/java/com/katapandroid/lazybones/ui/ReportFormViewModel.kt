package com.katapandroid.lazybones.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katapandroid.lazybones.data.Post
import com.katapandroid.lazybones.data.PostRepository
import com.katapandroid.lazybones.data.TagRepository
import com.katapandroid.lazybones.data.TagType
import com.katapandroid.lazybones.data.Tag
import com.katapandroid.lazybones.data.PoolReports
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import java.util.*
import androidx.compose.ui.text.input.TextFieldValue
import com.katapandroid.lazybones.data.SettingsRepository
import com.katapandroid.lazybones.data.TimePoolManager

class ReportFormViewModel(
    private val postRepository: PostRepository,
    private val tagRepository: TagRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val timePoolManager = TimePoolManager(settingsRepository)
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
    
    // Флаг, чтобы загружать отчет только один раз при инициализации
    private var hasLoadedTodayReport = false

    init {
        tagRepository.getByType(TagType.GOOD).onEach { _goodTags.value = it }.launchIn(viewModelScope)
        tagRepository.getByType(TagType.BAD).onEach { _badTags.value = it }.launchIn(viewModelScope)
        
        // Загружаем существующий отчет за сегодня при инициализации (только один раз)
        viewModelScope.launch {
            if (!hasLoadedTodayReport) {
                loadTodayReport()
                hasLoadedTodayReport = true
            }
        }
    }
    
    private suspend fun loadTodayReport() {
        val todayReport = getCurrentPoolReports().prioritized

        if (todayReport != null && _selectedGoodTags.value.isEmpty() && _selectedBadTags.value.isEmpty()) {
            _selectedGoodTags.value = todayReport.goodItems
            _selectedBadTags.value = todayReport.badItems

            val goodFieldsMap = todayReport.goodItems.associateWith { TextFieldValue(it) }
            val badFieldsMap = todayReport.badItems.associateWith { TextFieldValue(it) }
            _goodFields.value = goodFieldsMap
            _badFields.value = badFieldsMap
        }
    }
    
    // Публичный метод для загрузки отчета, если списки пустые (вызывается при входе на экран)
    suspend fun loadTodayReportIfEmpty() {
        // Загружаем только если списки пустые (значит пользователь еще ничего не добавил или ViewModel пересоздался)
        if (_selectedGoodTags.value.isEmpty() && _selectedBadTags.value.isEmpty()) {
            loadTodayReport()
        }
    }
    
    // Автоматически сохраняет черновик отчета при добавлении пункта
    suspend fun saveDraftReport(goodItems: List<String>, badItems: List<String>) {
        val todayReport = getCurrentPoolReports().prioritized

        if (todayReport != null) {
            // Обновляем существующий отчет за сегодня (остается черновиком)
            val updatedPost = todayReport.copy(
                goodItems = goodItems,
                badItems = badItems,
                goodCount = goodItems.size,
                badCount = badItems.size,
                isDraft = true // Остается черновиком
            )
            postRepository.update(updatedPost)
        } else {
            // Создаем новый отчет за сегодня (черновик)
            val post = Post(
                date = Date(),
                content = _content.value,
                checklist = emptyList(),
                voiceNotes = emptyList(),
                published = false,
                isDraft = true, // Черновик - не показывается в отчетах
                goodItems = goodItems,
                badItems = badItems,
                goodCount = goodItems.size,
                badCount = badItems.size
            )
            postRepository.insert(post)
        }
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

    suspend fun saveReport(goodItems: List<String>, badItems: List<String>, onSaved: () -> Unit) {
        val todayReport = getCurrentPoolReports().prioritized

        if (todayReport != null) {
            // Обновляем существующий отчет за сегодня и помечаем как НЕ черновик
            val updatedPost = todayReport.copy(
                goodItems = goodItems,
                badItems = badItems,
                goodCount = goodItems.size,
                badCount = badItems.size,
                isDraft = false // При явном сохранении помечаем как не черновик
            )
            postRepository.update(updatedPost)
        } else {
            // Создаем новый отчет за сегодня (не черновик, так как явно сохранен)
            val post = Post(
                date = Date(),
                content = _content.value,
                checklist = emptyList(),
                voiceNotes = emptyList(),
                published = false,
                isDraft = false, // Явно сохранен - не черновик
                goodItems = goodItems,
                badItems = badItems,
                goodCount = goodItems.size,
                badCount = badItems.size
            )
            postRepository.insert(post)
        }
        
        // Пункты остаются на экране - НЕ очищаем их
        // НЕ вызываем loadTodayReport() - локальное состояние не трогаем
        
        onSaved()
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

    private suspend fun getCurrentPoolReports(): PoolReports =
        timePoolManager.classifyReportsInCurrentPool(postRepository.getAllPostsSync())
} 