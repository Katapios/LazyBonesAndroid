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
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class LazyBonesWidgetProviderNarrow : AppWidgetProvider() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    companion object {
        /**
         * Обновляет все узкие виджеты приложения
         */
        fun updateAllWidgets(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = android.content.ComponentName(context, LazyBonesWidgetProviderNarrow::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                
                if (appWidgetIds.isNotEmpty()) {
                    val updateIntent = Intent(context, LazyBonesWidgetProviderNarrow::class.java).apply {
                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                    }
                    context.sendBroadcast(updateIntent)
                }
            } catch (e: Exception) {
                Log.e("WidgetNarrow", "Error updating widgets", e)
            }
        }
        
        /**
         * Обновляет конкретный узкий виджет
         */
        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val provider = LazyBonesWidgetProviderNarrow()
            provider.onUpdate(context, appWidgetManager, intArrayOf(appWidgetId))
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d("WidgetNarrow", "onUpdate called with ${appWidgetIds.size} widget(s)")
        
        if (appWidgetIds.isEmpty()) {
            Log.w("WidgetNarrow", "No widget IDs provided")
            return
        }
        
        for (appWidgetId in appWidgetIds) {
            try {
                Log.d("WidgetNarrow", "Processing widget ID: $appWidgetId")
                // Сначала показываем дефолтный виджет синхронно
                showDefaultWidget(context, appWidgetManager, appWidgetId)
                // Затем загружаем данные асинхронно
                loadDataAndUpdate(context, appWidgetManager, appWidgetId)
            } catch (e: Exception) {
                Log.e("WidgetNarrow", "Error in onUpdate for ID $appWidgetId", e)
                e.printStackTrace()
                // Пытаемся показать хотя бы минимальный виджет
                try {
                    val minimalViews = RemoteViews(context.packageName, R.layout.widget_layout_narrow)
                    minimalViews.setTextViewText(R.id.widget_motivation_text, "Ошибка")
                    appWidgetManager.updateAppWidget(appWidgetId, minimalViews)
                } catch (e2: Exception) {
                    Log.e("WidgetNarrow", "Complete failure for widget $appWidgetId", e2)
                }
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        if (intent.action == "com.katapandroid.lazybones.WIDGET_NARROW_UPDATE") {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                android.content.ComponentName(context, LazyBonesWidgetProviderNarrow::class.java)
            )
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }
    
    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        Log.d("WidgetNarrow", "onAppWidgetOptionsChanged for widget $appWidgetId")
        updateWidget(context, appWidgetManager, appWidgetId)
    }
    
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        Log.d("WidgetNarrow", "Widgets deleted: ${appWidgetIds.contentToString()}")
    }
    
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        Log.d("WidgetNarrow", "Widget provider disabled")
        scope.cancel()
    }
    
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Log.d("WidgetNarrow", "Widget provider enabled")
    }

    private fun showDefaultWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        try {
            Log.d("WidgetNarrow", "Creating RemoteViews for widget $appWidgetId")
            val views = RemoteViews(context.packageName, R.layout.widget_layout_narrow)
            
            // Применяем настройки темы и прозрачности
            applyWidgetTheme(context, views, appWidgetId)
            
            views.setTextViewText(R.id.widget_motivation_text, "Загрузка...")
            views.setTextViewText(R.id.widget_good_count, "0")
            views.setTextViewText(R.id.widget_bad_count, "0")
            views.setTextViewText(R.id.widget_timer_text, "")
            
            // Кнопка открытия приложения - при тапе по всему виджету
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            // Устанавливаем onClick на корневой элемент
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            
            // Кнопка настроек виджета (для Samsung)
            val settingsIntent = Intent(context, com.katapandroid.lazybones.widget.WidgetConfigureActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val settingsPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                settingsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_settings_button, settingsPendingIntent)
            
            appWidgetManager.updateAppWidget(appWidgetId, views)
            Log.d("WidgetNarrow", "Widget $appWidgetId updated successfully")
        } catch (e: Exception) {
            Log.e("WidgetNarrow", "Error showing default widget for ID $appWidgetId", e)
            e.printStackTrace()
            throw e
        }
    }

    private fun loadDataAndUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        scope.launch {
            try {
                val views = RemoteViews(context.packageName, R.layout.widget_layout_narrow)
                
                // Инициализируем базу данных (как в основном виджете)
                val db = try {
                    androidx.room.Room.databaseBuilder(
                        context,
                        LazyBonesDatabase::class.java,
                        "lazybones_db"
                    ).fallbackToDestructiveMigration().build()
                } catch (e: Exception) {
                    Log.e("WidgetNarrow", "Error getting database", e)
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

                // Отчеты за текущий пул (без checklist, но с пунктами good/bad), учитываем и черновики
                val reportsInPool = posts.filter { post ->
                    val postDate = post.date
                    val isInPool = postDate >= poolStart && postDate <= poolEnd
                    val noChecklist = post.checklist.isEmpty()
                    val hasGoodOrBad = post.goodItems.isNotEmpty() || post.badItems.isNotEmpty()
                    isInPool && noChecklist && hasGoodOrBad
                }
                val publishedReport = reportsInPool.firstOrNull { !it.isDraft && it.published }
                val savedReport = reportsInPool.firstOrNull { !it.isDraft && !it.published }
                val draftReport = reportsInPool.firstOrNull { it.isDraft }

                // Счетчики берем с приоритетом: опубликованный → сохраненный → черновик
                val countersSource = publishedReport ?: savedReport ?: draftReport
                val goodCount = countersSource?.goodItems?.size ?: 0
                val badCount = countersSource?.badItems?.size ?: 0

                // Получаем статус пула и таймер
                val poolStatusForTimer = timePoolManager.getPoolStatus()
                val timeUntilStart = timePoolManager.getTimeUntilPoolStart()
                val timeUntilEnd = timePoolManager.getTimeUntilPoolEnd()

                val timerText = when (poolStatusForTimer) {
                    PoolStatus.BEFORE_START -> {
                        timeUntilStart?.let { formatTime(it) }?.let { "До начала: $it" } ?: ""
                    }
                    PoolStatus.ACTIVE -> {
                        timeUntilEnd?.let { formatTime(it) }?.let { "До конца: $it" } ?: ""
                    }
                    PoolStatus.AFTER_END -> {
                        timeUntilStart?.let { formatTime(it) }?.let { "До начала: $it" } ?: "Пул завершен"
                    }
                }

                // Мотивационный текст - автоматически выбираем случайный из плана (меняется при каждом обновлении)
                val motivationText = if (poolStatusForTimer != PoolStatus.ACTIVE) {
                    "Отдыхай, LABотряс!"
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
                        "Не пора ли что-нибудь запланировать?"
                    }
                }

                // Применяем настройки темы и прозрачности
                applyWidgetTheme(context, views, appWidgetId)
                
                // Обновляем виджет
                views.setTextViewText(R.id.widget_motivation_text, motivationText)
                views.setTextViewText(R.id.widget_good_count, goodCount.toString())
                views.setTextViewText(R.id.widget_bad_count, badCount.toString())
                views.setTextViewText(R.id.widget_timer_text, timerText)

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
                // При тапе по виджету - переход в приложение
                views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

                // Кнопка настроек виджета (для Samsung)
                val settingsIntent = Intent(context, com.katapandroid.lazybones.widget.WidgetConfigureActivity::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                val settingsPendingIntent = PendingIntent.getActivity(
                    context,
                    appWidgetId + 2000, // Уникальный request code для узкого виджета
                    settingsIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_settings_button, settingsPendingIntent)

                appWidgetManager.updateAppWidget(appWidgetId, views)
                Log.d("WidgetNarrow", "Widget updated successfully")
            } catch (e: Exception) {
                Log.e("WidgetNarrow", "Error updating widget", e)
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
        try {
            val colorInt = android.graphics.Color.parseColor(bgColor)
            views.setInt(R.id.widget_root, "setBackgroundColor", colorInt)
        } catch (e: Exception) {
            Log.w("WidgetNarrow", "Could not set background color", e)
        }
        
        // Устанавливаем цвета текста
        // Лозунг и Good/Bad - фиксированные цвета (не зависят от темы)
        views.setTextColor(R.id.widget_motivation_text, android.graphics.Color.parseColor("#4CAF50")) // Зеленый
        views.setTextColor(R.id.widget_good_count, android.graphics.Color.parseColor("#4CAF50")) // Зеленый
        views.setTextColor(R.id.widget_bad_count, android.graphics.Color.parseColor("#F44336")) // Красный
        // Good и Bad лейблы - фиксированные цвета
        views.setTextColor(R.id.widget_good_label, android.graphics.Color.parseColor("#4CAF50")) // Зеленый
        views.setTextColor(R.id.widget_bad_label, android.graphics.Color.parseColor("#F44336")) // Красный
        // Таймер и кнопка настроек зависят от темы
        views.setTextColor(R.id.widget_timer_text, android.graphics.Color.parseColor(statusTextColor))
        views.setTextColor(R.id.widget_settings_button, android.graphics.Color.parseColor(secondaryTextColor))
    }
    
    private fun formatTime(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
}

