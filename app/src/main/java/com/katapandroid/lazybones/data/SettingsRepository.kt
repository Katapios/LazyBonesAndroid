package com.katapandroid.lazybones.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "lazybones_settings",
        Context.MODE_PRIVATE
    )
    
    private val _phoneName = MutableStateFlow(getPhoneName())
    val phoneName: Flow<String> = _phoneName.asStateFlow()
    
    private val _telegramToken = MutableStateFlow(getTelegramToken())
    val telegramToken: Flow<String> = _telegramToken.asStateFlow()
    
    private val _telegramChatId = MutableStateFlow(getTelegramChatId())
    val telegramChatId: Flow<String> = _telegramChatId.asStateFlow()
    
    private val _telegramBotId = MutableStateFlow(getTelegramBotId())
    val telegramBotId: Flow<String> = _telegramBotId.asStateFlow()
    
    private val _notificationsEnabled = MutableStateFlow(getNotificationsEnabled())
    val notificationsEnabled: Flow<Boolean> = _notificationsEnabled.asStateFlow()
    
    private val _notificationMode = MutableStateFlow(getNotificationMode())
    val notificationMode: Flow<Int> = _notificationMode.asStateFlow()
    
    // Getters
    fun getPhoneName(): String = prefs.getString(KEY_PHONE_NAME, "") ?: ""
    fun getTelegramToken(): String = prefs.getString(KEY_TELEGRAM_TOKEN, "") ?: ""
    fun getTelegramChatId(): String = prefs.getString(KEY_TELEGRAM_CHAT_ID, "") ?: ""
    fun getTelegramBotId(): String = prefs.getString(KEY_TELEGRAM_BOT_ID, "") ?: ""
    fun getNotificationsEnabled(): Boolean = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, false)
    fun getNotificationMode(): Int = prefs.getInt(KEY_NOTIFICATION_MODE, 0)
    
    // Setters
    fun setPhoneName(name: String) {
        prefs.edit().putString(KEY_PHONE_NAME, name).apply()
        _phoneName.value = name
    }
    
    fun setTelegramToken(token: String) {
        prefs.edit().putString(KEY_TELEGRAM_TOKEN, token).apply()
        _telegramToken.value = token
    }
    
    fun setTelegramChatId(chatId: String) {
        prefs.edit().putString(KEY_TELEGRAM_CHAT_ID, chatId).apply()
        _telegramChatId.value = chatId
    }
    
    fun setTelegramBotId(botId: String) {
        prefs.edit().putString(KEY_TELEGRAM_BOT_ID, botId).apply()
        _telegramBotId.value = botId
    }
    
    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
        _notificationsEnabled.value = enabled
    }
    
    fun setNotificationMode(mode: Int) {
        prefs.edit().putInt(KEY_NOTIFICATION_MODE, mode).apply()
        _notificationMode.value = mode
    }
    
    companion object {
        private const val KEY_PHONE_NAME = "phone_name"
        private const val KEY_TELEGRAM_TOKEN = "telegram_token"
        private const val KEY_TELEGRAM_CHAT_ID = "telegram_chat_id"
        private const val KEY_TELEGRAM_BOT_ID = "telegram_bot_id"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_NOTIFICATION_MODE = "notification_mode"
    }
} 