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
    
    private val _telegramLastUpdateId = MutableStateFlow(getTelegramLastUpdateId())
    val telegramLastUpdateId: Flow<Long> = _telegramLastUpdateId.asStateFlow()
    
    
    private val _notificationsEnabled = MutableStateFlow(getNotificationsEnabled())
    val notificationsEnabled: Flow<Boolean> = _notificationsEnabled.asStateFlow()
    
    private val _notificationMode = MutableStateFlow(getNotificationMode())
    val notificationMode: Flow<Int> = _notificationMode.asStateFlow()
    
    private val _poolStartMinutes = MutableStateFlow(getPoolStartMinutes())
    val poolStartMinutes: Flow<Int> = _poolStartMinutes.asStateFlow()
    
    private val _poolEndMinutes = MutableStateFlow(getPoolEndMinutes())
    val poolEndMinutes: Flow<Int> = _poolEndMinutes.asStateFlow()
    
    private val _unlockReportCreation = MutableStateFlow(getUnlockReportCreation())
    val unlockReportCreation: Flow<Boolean> = _unlockReportCreation.asStateFlow()
    
    private val _unlockPlanCreation = MutableStateFlow(getUnlockPlanCreation())
    val unlockPlanCreation: Flow<Boolean> = _unlockPlanCreation.asStateFlow()
    
    // Getters
    fun getPhoneName(): String = prefs.getString(KEY_PHONE_NAME, "") ?: ""
    fun getTelegramToken(): String = prefs.getString(KEY_TELEGRAM_TOKEN, "") ?: ""
    fun getTelegramChatId(): String = prefs.getString(KEY_TELEGRAM_CHAT_ID, "") ?: ""
    fun getTelegramBotId(): String = prefs.getString(KEY_TELEGRAM_BOT_ID, "") ?: ""
    fun getTelegramLastUpdateId(): Long = prefs.getLong(KEY_TELEGRAM_LAST_UPDATE_ID, 0L)
    fun getNotificationsEnabled(): Boolean = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, false)
    fun getNotificationMode(): Int = prefs.getInt(KEY_NOTIFICATION_MODE, 0)
    fun getPoolStartMinutes(): Int = prefs.getInt(KEY_POOL_START_MINUTES, 360) // По умолчанию 06:00 (360 минут)
    fun getPoolEndMinutes(): Int = prefs.getInt(KEY_POOL_END_MINUTES, 1080) // По умолчанию 18:00 (1080 минут, 12 часов от начала)
    fun getUnlockReportCreation(): Boolean = prefs.getBoolean(KEY_UNLOCK_REPORT_CREATION, false)
    fun getUnlockPlanCreation(): Boolean = prefs.getBoolean(KEY_UNLOCK_PLAN_CREATION, false)
    
    // Настройки виджета
    fun getWidgetTheme(widgetId: Int): Int = prefs.getInt("${KEY_WIDGET_THEME}_$widgetId", 0) // 0 = черный, 1 = белый
    fun getWidgetOpacity(widgetId: Int): Int = prefs.getInt("${KEY_WIDGET_OPACITY}_$widgetId", 100) // 0-100
    
    // Setters
    fun setPhoneName(name: String) {
        android.util.Log.d("SettingsRepository", "Setting phone name to: '$name' (current: '${_phoneName.value}')")
        val result = prefs.edit().putString(KEY_PHONE_NAME, name).commit()
        val savedValue = prefs.getString(KEY_PHONE_NAME, null)
        android.util.Log.d("SettingsRepository", "SharedPreferences commit result: $result, saved value: '$savedValue'")
        
        // Всегда обновляем StateFlow, чтобы уведомить подписчиков
        _phoneName.value = name
        android.util.Log.d("SettingsRepository", "Updated _phoneName.value to: '${_phoneName.value}'")
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
    
    fun setTelegramLastUpdateId(updateId: Long) {
        prefs.edit().putLong(KEY_TELEGRAM_LAST_UPDATE_ID, updateId).apply()
        _telegramLastUpdateId.value = updateId
    }
    
    
    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
        _notificationsEnabled.value = enabled
    }
    
    fun setNotificationMode(mode: Int) {
        prefs.edit().putInt(KEY_NOTIFICATION_MODE, mode).apply()
        _notificationMode.value = mode
    }
    
    fun setPoolStartMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_POOL_START_MINUTES, minutes).apply()
        _poolStartMinutes.value = minutes
    }
    
    fun setPoolEndMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_POOL_END_MINUTES, minutes).apply()
        _poolEndMinutes.value = minutes
    }
    
    fun setUnlockReportCreation(unlock: Boolean) {
        prefs.edit().putBoolean(KEY_UNLOCK_REPORT_CREATION, unlock).apply()
        _unlockReportCreation.value = unlock
    }
    
    fun setUnlockPlanCreation(unlock: Boolean) {
        prefs.edit().putBoolean(KEY_UNLOCK_PLAN_CREATION, unlock).apply()
        _unlockPlanCreation.value = unlock
    }
    
    // Настройки виджета
    fun setWidgetTheme(widgetId: Int, theme: Int) {
        prefs.edit().putInt("${KEY_WIDGET_THEME}_$widgetId", theme).apply()
    }
    
    fun setWidgetOpacity(widgetId: Int, opacity: Int) {
        prefs.edit().putInt("${KEY_WIDGET_OPACITY}_$widgetId", opacity.coerceIn(20, 100)).apply()
    }
    
    companion object {
        private const val KEY_PHONE_NAME = "phone_name"
        private const val KEY_TELEGRAM_TOKEN = "telegram_token"
        private const val KEY_TELEGRAM_CHAT_ID = "telegram_chat_id"
        private const val KEY_TELEGRAM_BOT_ID = "telegram_bot_id"
        private const val KEY_TELEGRAM_LAST_UPDATE_ID = "telegram_last_update_id"
        
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_NOTIFICATION_MODE = "notification_mode"
        private const val KEY_POOL_START_MINUTES = "pool_start_minutes"
        private const val KEY_POOL_END_MINUTES = "pool_end_minutes"
        private const val KEY_UNLOCK_REPORT_CREATION = "unlock_report_creation"
        private const val KEY_UNLOCK_PLAN_CREATION = "unlock_plan_creation"
        private const val KEY_WIDGET_THEME = "widget_theme"
        private const val KEY_WIDGET_OPACITY = "widget_opacity"
    }
}
