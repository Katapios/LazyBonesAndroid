package com.katapandroid.lazybones.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katapandroid.lazybones.network.TelegramService
import com.katapandroid.lazybones.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.skip
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
    
    private val _phoneNameSaveStatus = MutableStateFlow<String?>(null)
    val phoneNameSaveStatus: StateFlow<String?> = _phoneNameSaveStatus.asStateFlow()
    
    init {
        // Загружаем сохраненные настройки при первой инициализации
        viewModelScope.launch {
            // Загружаем начальное значение один раз при старте
            val initialName = settingsRepository.getPhoneName()
            _phoneName.value = initialName
            
            // Слушаем изменения из репозитория (когда сохраняется извне)
            // Используем skip(1), чтобы пропустить начальное значение, которое уже загружено
            settingsRepository.phoneName
                .distinctUntilChanged()
                .collect { newValue ->
                    // Обновляем только если значение действительно изменилось
                    if (_phoneName.value != newValue) {
                        android.util.Log.d("SettingsViewModel", "Phone name updated from repository: '$newValue'")
                        _phoneName.value = newValue
                    }
                }
        }
        viewModelScope.launch {
            settingsRepository.telegramToken
                .distinctUntilChanged()
                .collect { _telegramToken.value = it }
        }
        viewModelScope.launch {
            settingsRepository.telegramChatId
                .distinctUntilChanged()
                .collect { _telegramChatId.value = it }
        }
        viewModelScope.launch {
            settingsRepository.telegramBotId
                .distinctUntilChanged()
                .collect { _telegramBotId.value = it }
        }
        viewModelScope.launch {
            settingsRepository.notificationsEnabled
                .distinctUntilChanged()
                .collect { _notificationsEnabled.value = it }
        }
        viewModelScope.launch {
            settingsRepository.notificationMode
                .distinctUntilChanged()
                .collect { _notificationMode.value = it }
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
        val nameToSave = _phoneName.value.trim()
        android.util.Log.d("SettingsViewModel", "Saving phone name: '$nameToSave' (from _phoneName.value: '${_phoneName.value}')")
        settingsRepository.setPhoneName(nameToSave)
        val savedName = settingsRepository.getPhoneName()
        android.util.Log.d("SettingsViewModel", "Phone name saved, verifying: repository.getPhoneName() = '$savedName'")
        
        // Показываем подтверждение
        _phoneNameSaveStatus.value = "✅ Имя сохранено: $savedName"
        
        // Автоматически скрываем сообщение через 3 секунды
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
            _phoneNameSaveStatus.value = null
        }
    }
    
    fun clearPhoneNameSaveStatus() {
        _phoneNameSaveStatus.value = null
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