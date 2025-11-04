package com.katapandroid.lazybones.core.preferences

import android.content.Context
import android.content.SharedPreferences
import com.katapandroid.lazybones.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SharedPreferencesSettingsRepository(
    context: Context
) : SettingsRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _phoneName = MutableStateFlow(getPhoneName())
    override val phoneName: Flow<String> = _phoneName.asStateFlow()

    private val _telegramToken = MutableStateFlow(getTelegramToken())
    override val telegramToken: Flow<String> = _telegramToken.asStateFlow()

    private val _telegramChatId = MutableStateFlow(getTelegramChatId())
    override val telegramChatId: Flow<String> = _telegramChatId.asStateFlow()

    private val _telegramBotId = MutableStateFlow(getTelegramBotId())
    override val telegramBotId: Flow<String> = _telegramBotId.asStateFlow()

    private val _telegramLastUpdateId = MutableStateFlow(getTelegramLastUpdateId())
    override val telegramLastUpdateId: Flow<Long> = _telegramLastUpdateId.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(getNotificationsEnabled())
    override val notificationsEnabled: Flow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _notificationMode = MutableStateFlow(getNotificationMode())
    override val notificationMode: Flow<Int> = _notificationMode.asStateFlow()

    private val _poolStartMinutes = MutableStateFlow(getPoolStartMinutes())
    override val poolStartMinutes: Flow<Int> = _poolStartMinutes.asStateFlow()

    private val _poolEndMinutes = MutableStateFlow(getPoolEndMinutes())
    override val poolEndMinutes: Flow<Int> = _poolEndMinutes.asStateFlow()

    private val _unlockReportCreation = MutableStateFlow(getUnlockReportCreation())
    override val unlockReportCreation: Flow<Boolean> = _unlockReportCreation.asStateFlow()

    private val _unlockPlanCreation = MutableStateFlow(getUnlockPlanCreation())
    override val unlockPlanCreation: Flow<Boolean> = _unlockPlanCreation.asStateFlow()

    override fun getPhoneName(): String = prefs.getString(KEY_PHONE_NAME, "") ?: ""

    override fun setPhoneName(name: String) {
        prefs.edit().putString(KEY_PHONE_NAME, name).apply()
        _phoneName.value = name
    }

    override fun getTelegramToken(): String = prefs.getString(KEY_TELEGRAM_TOKEN, "") ?: ""

    override fun setTelegramToken(token: String) {
        prefs.edit().putString(KEY_TELEGRAM_TOKEN, token).apply()
        _telegramToken.value = token
    }

    override fun getTelegramChatId(): String = prefs.getString(KEY_TELEGRAM_CHAT_ID, "") ?: ""

    override fun setTelegramChatId(chatId: String) {
        prefs.edit().putString(KEY_TELEGRAM_CHAT_ID, chatId).apply()
        _telegramChatId.value = chatId
    }

    override fun getTelegramBotId(): String = prefs.getString(KEY_TELEGRAM_BOT_ID, "") ?: ""

    override fun setTelegramBotId(botId: String) {
        prefs.edit().putString(KEY_TELEGRAM_BOT_ID, botId).apply()
        _telegramBotId.value = botId
    }

    override fun getTelegramLastUpdateId(): Long = prefs.getLong(KEY_TELEGRAM_LAST_UPDATE_ID, 0L)

    override fun setTelegramLastUpdateId(updateId: Long) {
        prefs.edit().putLong(KEY_TELEGRAM_LAST_UPDATE_ID, updateId).apply()
        _telegramLastUpdateId.value = updateId
    }

    override fun getNotificationsEnabled(): Boolean = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, false)

    override fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
        _notificationsEnabled.value = enabled
    }

    override fun getNotificationMode(): Int = prefs.getInt(KEY_NOTIFICATION_MODE, 0)

    override fun setNotificationMode(mode: Int) {
        prefs.edit().putInt(KEY_NOTIFICATION_MODE, mode).apply()
        _notificationMode.value = mode
    }

    override fun getPoolStartMinutes(): Int = prefs.getInt(KEY_POOL_START_MINUTES, DEFAULT_POOL_START)

    override fun setPoolStartMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_POOL_START_MINUTES, minutes).apply()
        _poolStartMinutes.value = minutes
    }

    override fun getPoolEndMinutes(): Int = prefs.getInt(KEY_POOL_END_MINUTES, DEFAULT_POOL_END)

    override fun setPoolEndMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_POOL_END_MINUTES, minutes).apply()
        _poolEndMinutes.value = minutes
    }

    override fun getUnlockReportCreation(): Boolean = prefs.getBoolean(KEY_UNLOCK_REPORT_CREATION, false)

    override fun setUnlockReportCreation(unlock: Boolean) {
        prefs.edit().putBoolean(KEY_UNLOCK_REPORT_CREATION, unlock).apply()
        _unlockReportCreation.value = unlock
    }

    override fun getUnlockPlanCreation(): Boolean = prefs.getBoolean(KEY_UNLOCK_PLAN_CREATION, false)

    override fun setUnlockPlanCreation(unlock: Boolean) {
        prefs.edit().putBoolean(KEY_UNLOCK_PLAN_CREATION, unlock).apply()
        _unlockPlanCreation.value = unlock
    }

    override fun getWidgetTheme(widgetId: Int): Int = prefs.getInt("${KEY_WIDGET_THEME}_$widgetId", 0)

    override fun setWidgetTheme(widgetId: Int, theme: Int) {
        prefs.edit().putInt("${KEY_WIDGET_THEME}_$widgetId", theme).apply()
    }

    override fun getWidgetOpacity(widgetId: Int): Int = prefs.getInt("${KEY_WIDGET_OPACITY}_$widgetId", 100)

    override fun setWidgetOpacity(widgetId: Int, opacity: Int) {
        prefs.edit().putInt("${KEY_WIDGET_OPACITY}_$widgetId", opacity.coerceIn(20, 100)).apply()
    }

    companion object {
        private const val PREFS_NAME = "lazybones_settings"

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

        private const val DEFAULT_POOL_START = 360
        private const val DEFAULT_POOL_END = 1080
    }
}
