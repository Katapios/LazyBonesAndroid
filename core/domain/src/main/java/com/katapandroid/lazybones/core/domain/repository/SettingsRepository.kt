package com.katapandroid.lazybones.core.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val phoneName: Flow<String>
    val telegramToken: Flow<String>
    val telegramChatId: Flow<String>
    val telegramBotId: Flow<String>
    val telegramLastUpdateId: Flow<Long>
    val notificationsEnabled: Flow<Boolean>
    val notificationMode: Flow<Int>
    val poolStartMinutes: Flow<Int>
    val poolEndMinutes: Flow<Int>
    val unlockReportCreation: Flow<Boolean>
    val unlockPlanCreation: Flow<Boolean>

    fun getPhoneName(): String
    fun setPhoneName(name: String)

    fun getTelegramToken(): String
    fun setTelegramToken(token: String)

    fun getTelegramChatId(): String
    fun setTelegramChatId(chatId: String)

    fun getTelegramBotId(): String
    fun setTelegramBotId(botId: String)

    fun getTelegramLastUpdateId(): Long
    fun setTelegramLastUpdateId(updateId: Long)

    fun getNotificationsEnabled(): Boolean
    fun setNotificationsEnabled(enabled: Boolean)

    fun getNotificationMode(): Int
    fun setNotificationMode(mode: Int)

    fun getPoolStartMinutes(): Int
    fun setPoolStartMinutes(minutes: Int)

    fun getPoolEndMinutes(): Int
    fun setPoolEndMinutes(minutes: Int)

    fun getUnlockReportCreation(): Boolean
    fun setUnlockReportCreation(unlock: Boolean)

    fun getUnlockPlanCreation(): Boolean
    fun setUnlockPlanCreation(unlock: Boolean)

    fun getWidgetTheme(widgetId: Int): Int
    fun setWidgetTheme(widgetId: Int, theme: Int)

    fun getWidgetOpacity(widgetId: Int): Int
    fun setWidgetOpacity(widgetId: Int, opacity: Int)
}
