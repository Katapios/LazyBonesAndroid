package com.katapandroid.lazybones.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katapandroid.lazybones.network.TelegramService
import com.katapandroid.lazybones.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val telegramService: TelegramService,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    private val _phoneName = MutableStateFlow("")
    val phoneName: StateFlow<String> = _phoneName.asStateFlow()
    
    private val _telegramToken = MutableStateFlow("")
    val telegramToken: StateFlow<String> = _telegramToken.asStateFlow()
    
    private val _telegramChatId = MutableStateFlow("")
    val telegramChatId: StateFlow<String> = _telegramChatId.asStateFlow()
    
    private val _telegramBotId = MutableStateFlow("")
    val telegramBotId: StateFlow<String> = _telegramBotId.asStateFlow()
    
    private val _notificationsEnabled = MutableStateFlow(false)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()
    
    private val _notificationMode = MutableStateFlow(0)
    val notificationMode: StateFlow<Int> = _notificationMode.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _testMessageResult = MutableStateFlow<String?>(null)
    val testMessageResult: StateFlow<String?> = _testMessageResult.asStateFlow()
    
    init {
        // Загружаем сохраненные настройки
        viewModelScope.launch {
            settingsRepository.phoneName.collect { _phoneName.value = it }
        }
        viewModelScope.launch {
            settingsRepository.telegramToken.collect { _telegramToken.value = it }
        }
        viewModelScope.launch {
            settingsRepository.telegramChatId.collect { _telegramChatId.value = it }
        }
        viewModelScope.launch {
            settingsRepository.telegramBotId.collect { _telegramBotId.value = it }
        }
        viewModelScope.launch {
            settingsRepository.notificationsEnabled.collect { _notificationsEnabled.value = it }
        }
        viewModelScope.launch {
            settingsRepository.notificationMode.collect { _notificationMode.value = it }
        }
    }
    
    fun setPhoneName(name: String) {
        _phoneName.value = name
    }
    
    fun setTelegramToken(token: String) {
        _telegramToken.value = token
    }
    
    fun setTelegramChatId(chatId: String) {
        _telegramChatId.value = chatId
    }
    
    fun setTelegramBotId(botId: String) {
        _telegramBotId.value = botId
    }
    
    fun setNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
    }
    
    fun setNotificationMode(mode: Int) {
        _notificationMode.value = mode
    }
    
    fun savePhoneName() {
        settingsRepository.setPhoneName(_phoneName.value)
    }
    
    fun saveTelegramSettings() {
        settingsRepository.setTelegramToken(_telegramToken.value)
        settingsRepository.setTelegramChatId(_telegramChatId.value)
        settingsRepository.setTelegramBotId(_telegramBotId.value)
    }
    
    fun testTelegramConnection() {
        viewModelScope.launch {
            _isLoading.value = true
            _testMessageResult.value = null
            
            try {
                val result = telegramService.sendTestMessage(_telegramToken.value, _telegramChatId.value)
                result.fold(
                    onSuccess = { 
                        _testMessageResult.value = "✅ Связь установлена успешно!"
                    },
                    onFailure = { exception ->
                        _testMessageResult.value = "❌ Ошибка: ${exception.message}"
                    }
                )
            } catch (e: Exception) {
                _testMessageResult.value = "❌ Ошибка: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun clearTestMessageResult() {
        _testMessageResult.value = null
    }
} 