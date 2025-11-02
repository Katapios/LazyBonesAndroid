package com.katapandroid.lazybones.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katapandroid.lazybones.data.Post
import com.katapandroid.lazybones.data.PostRepository
import com.katapandroid.lazybones.data.SettingsRepository
import com.katapandroid.lazybones.network.TelegramService
import com.katapandroid.lazybones.network.TelegramMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.onEach

class ReportsViewModel(
    private val postRepository: PostRepository,
    private val telegramService: TelegramService,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()
    
    private val _customPosts = MutableStateFlow<List<Post>>(emptyList())
    val customPosts: StateFlow<List<Post>> = _customPosts.asStateFlow()

    private val _telegramMessages = MutableStateFlow<List<TelegramMessage>>(emptyList())
    val telegramMessages: StateFlow<List<TelegramMessage>> = _telegramMessages.asStateFlow()

    private val _telegramLoading = MutableStateFlow(false)
    val telegramLoading: StateFlow<Boolean> = _telegramLoading.asStateFlow()

    private val _telegramError = MutableStateFlow<String?>(null)
    val telegramError: StateFlow<String?> = _telegramError.asStateFlow()

    init {
        postRepository.getAllPosts().onEach { posts ->
            // Локальные отчеты - это отчеты с goodItems или badItems, созданные через ReportFormScreen (без checklist)
            // Черновики не показываются (isDraft = false)
            _posts.value = posts.filter { 
                (it.goodItems.isNotEmpty() || it.badItems.isNotEmpty()) && 
                it.checklist.isEmpty() && 
                !it.isDraft
            }
            // Кастомные отчеты - это отчеты с checklist (созданные через PlanScreen), независимо от наличия goodItems/badItems
            // Черновики не показываются
            _customPosts.value = posts.filter { it.checklist.isNotEmpty() && !it.isDraft }
        }.launchIn(viewModelScope)
    }

    suspend fun addPost(post: Post) = postRepository.insert(post)
    suspend fun deletePost(post: Post) {
        postRepository.delete(post)
    }
    
    suspend fun deleteCustomPost(post: Post) {
        postRepository.delete(post)
    }
    
    suspend fun updatePost(post: Post) {
        postRepository.update(post)
    }
    
    suspend fun publishCustomReportToTelegram(
        post: Post,
        token: String,
        chatId: String
    ): Result<String> {
        return try {
            // Получаем сохраненное имя устройства
            val deviceName = settingsRepository.getPhoneName()
            val result = telegramService.sendCustomReport(
                token = token,
                chatId = chatId,
                date = post.date,
                checklist = post.checklist,
                goodItems = post.goodItems,
                badItems = post.badItems,
                deviceName = deviceName
            )
            
            result.fold(
                onSuccess = { 
                    // Обновляем статус публикации
                    val updatedPost = post.copy(published = true)
                    postRepository.update(updatedPost)
                    Result.success("Отчет успешно опубликован в Telegram")
                },
                onFailure = { exception ->
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun clearTelegramError() { _telegramError.value = null }
    fun setTelegramError(message: String) { _telegramError.value = message }

    fun refreshTelegramMessages() {
        viewModelScope.launch {
            _telegramLoading.value = true
            _telegramError.value = null
            try {
                val token = settingsRepository.getTelegramToken()
                val chatIdStr = settingsRepository.getTelegramChatId()
                if (token.isBlank() || chatIdStr.isBlank()) {
                    _telegramError.value = "Укажите токен и chat_id в настройках"
                    return@launch
                }
                // Используем тот же chatId, что и для отправки - резолвим в numeric ID для фильтрации
                val trimmedChatId = chatIdStr.trim()
                val targetChatId: Long = trimmedChatId.toLongOrNull() ?: run {
                    // Нужно резолвить username в numeric ID
                    val res = telegramService.resolveChatNumericId(token, trimmedChatId)
                    when {
                        res.isSuccess -> res.getOrNull()!!
                        else -> {
                            val errorMsg = res.exceptionOrNull()?.message 
                                ?: "Не удалось определить ID группы. Проверьте chat_id в настройках"
                            _telegramError.value = errorMsg
                            return@launch
                        }
                    }
                }
                // При явном обновлении получаем все доступные сообщения, начиная с самого начала
                // Telegram хранит обновления в очереди до 24 часов, поэтому можем получить историю за этот период
                val allMessages = mutableListOf<com.katapandroid.lazybones.network.TelegramMessage>()
                var currentOffset: Long? = 0L // Начинаем с начала, чтобы получить всю доступную историю
                var maxUpdateId: Long? = null
                var hasMore = true
                var iterations = 0
                
                // Делаем запросы пока получаем обновления (увеличиваем до 20 итераций для получения больше истории)
                while (hasMore && iterations < 20) {
                    iterations++
                    val result = telegramService.fetchRecentMessages(token, currentOffset)
                    result.fold(
                        onSuccess = { (messages, updateId) ->
                            if (messages.isEmpty()) {
                                hasMore = false
                                return@fold
                            }
                            allMessages.addAll(messages)
                            maxUpdateId = updateId
                            // Для следующего запроса используем offset = maxUpdateId + 1, чтобы получить следующие обновления
                            currentOffset = if (updateId != null && updateId > 0) updateId + 1 else null
                            // Если получено меньше лимита - больше обновлений нет
                            if (messages.size < 100) {
                                hasMore = false
                            }
                        },
                        onFailure = { 
                            // При ошибке прерываем цикл
                            hasMore = false
                        }
                    )
                }
                
                // Фильтруем все накопленные сообщения по нужному chatId
                val filtered = allMessages.filter { it.chatId == targetChatId }
                _telegramMessages.value = filtered.sortedByDescending { it.dateSeconds }.take(30)
                
                // Сохраняем offset для следующего запроса
                val finalUpdateId = maxUpdateId
                if (finalUpdateId != null && finalUpdateId > 0) {
                    settingsRepository.setTelegramLastUpdateId(finalUpdateId + 1)
                }
            } catch (e: Exception) {
                _telegramError.value = e.message ?: "Неизвестная ошибка"
            } finally {
                _telegramLoading.value = false
            }
        }
    }

    suspend fun resolveGroupLink(): Result<String> {
        return try {
            val token = settingsRepository.getTelegramToken()
            val chatId = settingsRepository.getTelegramChatId()
            if (token.isBlank() || chatId.isBlank()) {
                return Result.failure(Exception("Укажите токен и chat_id в настройках"))
            }
            telegramService.resolveChatOpenLink(token, chatId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 