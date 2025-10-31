package com.katapandroid.lazybones.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katapandroid.lazybones.data.Post
import com.katapandroid.lazybones.data.PostRepository
import com.katapandroid.lazybones.data.SettingsRepository
import com.katapandroid.lazybones.network.TelegramService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
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

    init {
        postRepository.getAllPosts().onEach { posts ->
            // Локальные отчеты - это отчеты с goodItems или badItems, созданные через ReportFormScreen (без checklist)
            _posts.value = posts.filter { 
                (it.goodItems.isNotEmpty() || it.badItems.isNotEmpty()) && it.checklist.isEmpty() 
            }
            // Кастомные отчеты - это отчеты с checklist (созданные через PlanScreen), независимо от наличия goodItems/badItems
            _customPosts.value = posts.filter { it.checklist.isNotEmpty() }
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
} 