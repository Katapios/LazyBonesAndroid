package com.katapandroid.lazybones.feature.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katapandroid.lazybones.core.domain.model.Post
import com.katapandroid.lazybones.core.domain.model.TelegramMessage
import com.katapandroid.lazybones.core.domain.repository.PostRepository
import com.katapandroid.lazybones.core.domain.repository.SettingsRepository
import com.katapandroid.lazybones.core.domain.service.TelegramGateway
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.onEach

class ReportsViewModel(
    private val postRepository: PostRepository,
    private val telegramGateway: TelegramGateway,
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
        postRepository.observePosts().onEach { posts ->
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
            val result = telegramGateway.publishReport(
                token = token,
                chatId = chatId,
                post = post,
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

                val trimmedChatId = chatIdStr.trim()
                val targetChatId = trimmedChatId.toLongOrNull() ?: run {
                    val resolvedId = telegramGateway.resolveChatNumericId(token, trimmedChatId)
                    if (resolvedId.isFailure) {
                        _telegramError.value = resolvedId.exceptionOrNull()?.message
                            ?: "Не удалось определить ID группы. Проверьте chat_id в настройках"
                        return@launch
                    }
                    resolvedId.getOrThrow()
                }

                val allMessages = mutableListOf<TelegramMessage>()
                var currentOffset: Long? = 0L
                var maxUpdateId: Long? = null
                var iterations = 0
                var hasMore = true

                while (hasMore && iterations < 20) {
                    iterations++
                    val fetchResult = telegramGateway.fetchRecentMessages(token, currentOffset)
                    if (fetchResult.isFailure) {
                        _telegramError.value = fetchResult.exceptionOrNull()?.message
                        break
                    }

                    val (messages, updateId) = fetchResult.getOrThrow()
                    if (messages.isEmpty()) {
                        hasMore = false
                    } else {
                        allMessages.addAll(messages)
                        maxUpdateId = updateId
                        currentOffset = if (updateId != null && updateId > 0) updateId + 1 else null
                        if (messages.size < 100) {
                            hasMore = false
                        }
                    }
                }

                val filtered = allMessages.filter { it.chatId == targetChatId }
                    .sortedByDescending { it.dateSeconds }
                    .take(30)
                _telegramMessages.value = filtered

                maxUpdateId?.let { updateId ->
                    if (updateId > 0) {
                        settingsRepository.setTelegramLastUpdateId(updateId + 1)
                    }
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
              telegramGateway.resolveChatOpenLink(token, chatId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 