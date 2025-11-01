package com.katapandroid.lazybones.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.katapandroid.lazybones.MainActivity
import com.katapandroid.lazybones.R
import com.katapandroid.lazybones.data.LazyBonesDatabase
import com.katapandroid.lazybones.data.Post
import com.katapandroid.lazybones.data.PlanItem
import com.katapandroid.lazybones.data.SettingsRepository
import com.katapandroid.lazybones.data.TimePoolManager
import com.katapandroid.lazybones.data.PoolStatus
import com.katapandroid.lazybones.widget.WidgetConfigureActivity
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class LazyBonesWidgetProvider : AppWidgetProvider() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    companion object {
        /**
         * Обновляет все виджеты приложения
         */
        fun updateAllWidgets(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = android.content.ComponentName(context, LazyBonesWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                
                if (appWidgetIds.isNotEmpty()) {
                    val updateIntent = Intent(context, LazyBonesWidgetProvider::class.java).apply {
                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                    }
                    context.sendBroadcast(updateIntent)
                }
            } catch (e: Exception) {
                Log.e("Widget", "Error updating widgets", e)
            }
        }
        
        /**
         * Обновляет конкретный виджет
         */
        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val provider = LazyBonesWidgetProvider()
            provider.onUpdate(context, appWidgetManager, intArrayOf(appWidgetId))
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d("Widget", "onUpdate called with ${appWidgetIds.size} widget(s)")
        
        if (appWidgetIds.isEmpty()) {
            Log.w("Widget", "No widget IDs provided")
            return
        }
        
        for (appWidgetId in appWidgetIds) {
            try {
                Log.d("Widget", "Processing widget ID: $appWidgetId")
                // Сначала показываем дефолтный виджет синхронно
                showDefaultWidget(context, appWidgetManager, appWidgetId)
                // Затем загружаем данные асинхронно
                loadDataAndUpdate(context, appWidgetManager, appWidgetId)
            } catch (e: Exception) {
                Log.e("Widget", "Error in onUpdate for ID $appWidgetId", e)
                e.printStackTrace()
                // Пытаемся показать хотя бы минимальный виджет
                try {
                    val minimalViews = RemoteViews(context.packageName, R.layout.widget_layout)
                    minimalViews.setTextViewText(R.id.widget_motivation_text, "Ошибка")
                    appWidgetManager.updateAppWidget(appWidgetId, minimalViews)
                } catch (e2: Exception) {
                    Log.e("Widget", "Complete failure for widget $appWidgetId", e2)
                }
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        if (intent.action == "com.katapandroid.lazybones.WIDGET_UPDATE") {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                android.content.ComponentName(context, LazyBonesWidgetProvider::class.java)
            )
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }

    private fun showDefaultWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        try {
            Log.d("Widget", "Creating RemoteViews for widget $appWidgetId")
            val views = RemoteViews(context.packageName, R.layout.widget_layout)
            
            if (views == null) {
                Log.e("Widget", "RemoteViews is null!")
                return
            }
            
            // Применяем настройки темы и прозрачности
            applyWidgetTheme(context, views, appWidgetId)
            
            Log.d("Widget", "Setting default text")
            views.setTextViewText(R.id.widget_motivation_text, "Загрузка...")
            views.setTextViewText(R.id.widget_status_text, "")
            views.setTextViewText(R.id.widget_timer_text, "")
            views.setTextViewText(R.id.widget_good_count, "0")
            views.setTextViewText(R.id.widget_bad_count, "0")
            views.setViewVisibility(R.id.widget_progress_container, android.view.View.VISIBLE)
            
            Log.d("Widget", "Setting up intents")
            // Кнопка открытия приложения
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_open_app, pendingIntent)
            
            // Обновление виджета по клику
            val refreshIntent = Intent("com.katapandroid.lazybones.WIDGET_UPDATE").apply {
                setComponent(android.content.ComponentName(context, LazyBonesWidgetProvider::class.java))
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_motivation_text, refreshPendingIntent)
            
            Log.d("Widget", "Updating widget $appWidgetId")
            appWidgetManager.updateAppWidget(appWidgetId, views)
            Log.d("Widget", "Widget $appWidgetId updated successfully")
        } catch (e: Exception) {
            Log.e("Widget", "Error showing default widget for ID $appWidgetId", e)
            e.printStackTrace()
            throw e // Пробрасываем дальше для обработки в onUpdate
        }
    }

    private fun loadDataAndUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        scope.launch {
            try {
                val views = RemoteViews(context.packageName, R.layout.widget_layout)
                
                // Инициализируем базу данных (как в оригинале)
                val db = try {
                    androidx.room.Room.databaseBuilder(
                        context,
                        LazyBonesDatabase::class.java,
                        "lazybones_db"
                    ).fallbackToDestructiveMigration().build()
                } catch (e: Exception) {
                    Log.e("Widget", "Error getting database", e)
                    null
                }

                if (db == null) {
                    showDefaultWidget(context, appWidgetManager, appWidgetId)
                    return@launch
                }

                val postDao = db.postDao()
                val planItemDao = db.planItemDao()

                // Получаем посты и пункты плана
                val posts = withContext(Dispatchers.IO) { postDao.getAllPostsSync() }
                val planItems = withContext(Dispatchers.IO) { planItemDao.getAllSync() }

                // Фильтруем посты по текущему пулу времени
                val settingsRepository = SettingsRepository(context)
                val timePoolManager = TimePoolManager(settingsRepository)
                val (poolStart, poolEnd) = timePoolManager.getCurrentPoolRange()

                // Ищем отчет за текущий пул времени
                val todayReport = posts.firstOrNull { post ->
                    val postDate = post.date
                    val isInPool = postDate >= poolStart && postDate <= poolEnd
                    val noChecklist = post.checklist.isEmpty()
                    val hasGoodOrBad = post.goodItems.isNotEmpty() || post.badItems.isNotEmpty()
                    isInPool && noChecklist && hasGoodOrBad && !post.isDraft
                }

                // Получаем счетчики из отчета
                val goodCount = todayReport?.goodItems?.size ?: 0
                val badCount = todayReport?.badItems?.size ?: 0

                // Определяем статус отчета
                val reportStatus = when {
                    todayReport == null -> "Отчет не заполнен"
                    todayReport.isDraft -> "В процессе"
                    todayReport.published -> "Отчет опубликован"
                    else -> "Отчет сформирован"
                }

                // Получаем статус пула и таймер
                val poolStatusForTimer = timePoolManager.getPoolStatus()
                val timeUntilStart = timePoolManager.getTimeUntilPoolStart()
                val timeUntilEnd = timePoolManager.getTimeUntilPoolEnd()

                val timerText = when (poolStatusForTimer) {
                    PoolStatus.BEFORE_START -> {
                        timeUntilStart?.let { formatTime(it) }?.let { "До начала пула: $it" } ?: ""
                    }
                    PoolStatus.ACTIVE -> {
                        timeUntilEnd?.let { formatTime(it) }?.let { "До конца пула: $it" } ?: ""
                    }
                    PoolStatus.AFTER_END -> {
                        timeUntilStart?.let { formatTime(it) }?.let { "До начала пула: $it" } ?: "Пул завершен"
                    }
                }

                // Мотивационный текст
                val motivationText = if (poolStatusForTimer != PoolStatus.ACTIVE) {
                    "Отдыхай, LABотряс! Не пришло твое время"
                } else {
                    val currentPlanItems = planItems
                    if (currentPlanItems.isNotEmpty()) {
                        val randomItem = currentPlanItems.random().text
                        val templates = listOf(
                            "Эй, а ты не забыл сделать «$randomItem»?",
                            "Как насчет «$randomItem»?",
                            "А что там с «$randomItem»?",
                            "Не пора ли заняться «$randomItem»?",
                            "Помнишь про «$randomItem»?"
                        )
                        templates.random()
                    } else {
                        "Не пора ли что-нибудь запланировать, LABотряс?"
                    }
                }

                // Применяем настройки темы и прозрачности
                applyWidgetTheme(context, views, appWidgetId)
                
                // Обновляем виджет
                views.setTextViewText(R.id.widget_motivation_text, motivationText)
                views.setTextViewText(R.id.widget_good_count, goodCount.toString())
                views.setTextViewText(R.id.widget_bad_count, badCount.toString())
                
                // Статус отдельно (2 строки)
                views.setTextViewText(R.id.widget_status_text, reportStatus)
                // Таймер отдельно ниже
                views.setTextViewText(R.id.widget_timer_text, timerText)

                // Прогресс-бар
                val total = goodCount + badCount
                if (total > 0) {
                    views.setViewVisibility(R.id.widget_progress_container, android.view.View.VISIBLE)
                } else {
                    views.setViewVisibility(R.id.widget_progress_container, android.view.View.GONE)
                }

                // Кнопки
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_open_app, pendingIntent)

                val refreshIntent = Intent("com.katapandroid.lazybones.WIDGET_UPDATE").apply {
                    setComponent(android.content.ComponentName(context, LazyBonesWidgetProvider::class.java))
                }
                val refreshPendingIntent = PendingIntent.getBroadcast(
                    context,
                    0,
                    refreshIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_motivation_text, refreshPendingIntent)

                appWidgetManager.updateAppWidget(appWidgetId, views)
                Log.d("Widget", "Widget updated successfully")
            } catch (e: Exception) {
                Log.e("Widget", "Error updating widget", e)
                showDefaultWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }

    private fun applyWidgetTheme(context: Context, views: RemoteViews, appWidgetId: Int) {
        val settingsRepository = SettingsRepository(context)
        val theme = settingsRepository.getWidgetTheme(appWidgetId) // 0 = черный, 1 = белый
        val opacity = settingsRepository.getWidgetOpacity(appWidgetId) // 20-100
        
        // Вычисляем цвета в зависимости от темы
        val bgColor = if (theme == 0) {
            // Темная тема: черный с прозрачностью
            val alpha = (opacity * 255 / 100).coerceIn(51, 255) // 20% = 51, 100% = 255
            String.format("#%02X000000", alpha)
        } else {
            // Светлая тема: белый с прозрачностью
            val alpha = (opacity * 255 / 100).coerceIn(51, 255)
            String.format("#%02XFFFFFF", alpha)
        }
        
        val textColor = if (theme == 0) "#FFFFFF" else "#000000"
        val secondaryTextColor = if (theme == 0) "#CCCCCC" else "#666666"
        val statusTextColor = if (theme == 0) "#AAAAAA" else "#888888"
        
        // Применяем фон к корневому LinearLayout
        // В RemoteViews используем setInt для установки цвета фона
        try {
            val colorInt = android.graphics.Color.parseColor(bgColor)
            views.setInt(R.id.widget_root, "setBackgroundColor", colorInt)
        } catch (e: Exception) {
            Log.w("Widget", "Could not set background color", e)
        }
        
        // Устанавливаем цвета текста
        views.setTextColor(R.id.widget_motivation_text, android.graphics.Color.parseColor(textColor))
        views.setTextColor(R.id.widget_status_text, android.graphics.Color.parseColor(textColor))
        views.setTextColor(R.id.widget_timer_text, android.graphics.Color.parseColor(statusTextColor))
    }
    
    private fun formatTime(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
}
