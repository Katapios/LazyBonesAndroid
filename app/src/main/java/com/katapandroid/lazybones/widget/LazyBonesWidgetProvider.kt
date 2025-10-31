package com.katapandroid.lazybones.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.katapandroid.lazybones.MainActivity
import com.katapandroid.lazybones.R
import com.katapandroid.lazybones.data.LazyBonesDatabase
import com.katapandroid.lazybones.data.Post
import com.katapandroid.lazybones.data.PlanItem
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
                    Log.d("Widget", "Manually updating ${appWidgetIds.size} widget(s)")
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
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d("Widget", "onUpdate called with ${appWidgetIds.size} widget(s)")
        for (appWidgetId in appWidgetIds) {
            Log.d("Widget", "Updating widget ID: $appWidgetId")
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        // Обработка клика на виджет для обновления
        if (intent.action == "com.katapandroid.lazybones.WIDGET_UPDATE") {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                android.content.ComponentName(context, LazyBonesWidgetProvider::class.java)
            )
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        Log.d("Widget", "updateWidget called for ID: $appWidgetId")
        
        try {
            // onUpdate должен быть быстрым, поэтому показываем виджет с дефолтными значениями
            val views = RemoteViews(context.packageName, R.layout.widget_layout)
            Log.d("Widget", "RemoteViews created successfully for package: ${context.packageName}")
            
            // Проверяем, что layout загружен
            if (views == null) {
                Log.e("Widget", "RemoteViews is null!")
                return
            }
            
            // Устанавливаем дефолтные значения - сначала пробуем установить все тексты
            try {
                views.setTextViewText(R.id.widget_motivation_text, "Не пора ли что-нибудь запланировать, LABотряс?")
                Log.d("Widget", "Set motivation text")
            } catch (e: Exception) {
                Log.e("Widget", "Error setting motivation text", e)
            }
            
            try {
                views.setTextViewText(R.id.widget_good_count, "0")
                Log.d("Widget", "Set good count")
            } catch (e: Exception) {
                Log.e("Widget", "Error setting good count", e)
            }
            
            try {
                views.setTextViewText(R.id.widget_bad_count, "0")
                Log.d("Widget", "Set bad count")
            } catch (e: Exception) {
                Log.e("Widget", "Error setting bad count", e)
            }
            
            // Скрываем прогресс-бар по умолчанию, пока нет данных
            try {
                views.setViewVisibility(R.id.widget_progress_container, android.view.View.GONE)
                Log.d("Widget", "Progress container hidden")
            } catch (e: Exception) {
                Log.e("Widget", "Error hiding progress container", e)
            }
            
            // Кнопка открытия приложения
            try {
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
                
                // Также делаем весь виджет кликабельным для обновления
                val updateIntent = Intent("com.katapandroid.lazybones.WIDGET_UPDATE").apply {
                    setComponent(android.content.ComponentName(context, LazyBonesWidgetProvider::class.java))
                }
                val updatePendingIntent = PendingIntent.getBroadcast(
                    context,
                    0,
                    updateIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                // Делаем кликабельным весь контейнер мотивационного текста
                views.setOnClickPendingIntent(R.id.widget_motivation_text, updatePendingIntent)
                
                Log.d("Widget", "PendingIntent set")
            } catch (e: Exception) {
                Log.e("Widget", "Error setting PendingIntent", e)
            }
            
            Log.d("Widget", "Updating widget with initial layout, views: $views")
            appWidgetManager.updateAppWidget(appWidgetId, views)
            Log.d("Widget", "Widget updated successfully with initial layout")
        } catch (e: Exception) {
            Log.e("Widget", "Error setting initial widget layout", e)
            e.printStackTrace()
            
            // Попытка создать простейший виджет в случае ошибки
            try {
                val simpleViews = RemoteViews(context.packageName, R.layout.widget_layout)
                simpleViews.setTextViewText(R.id.widget_motivation_text, "Ошибка загрузки виджета")
                appWidgetManager.updateAppWidget(appWidgetId, simpleViews)
            } catch (e2: Exception) {
                Log.e("Widget", "Failed to set even simple widget", e2)
            }
        }
        
        // Загружаем данные асинхронно
        scope.launch {
            try {
                Log.d("Widget", "Starting async data load")
                val db = withContext(Dispatchers.IO) {
                    try {
                        // Пытаемся получить базу через Koin
                        val koinDb = org.koin.core.context.GlobalContext.getOrNull()?.get<LazyBonesDatabase>()
                        if (koinDb != null) {
                            Log.d("Widget", "Using Koin database")
                            koinDb
                        } else {
                            Log.d("Widget", "Koin not available, creating fallback database")
                            // Fallback: создаем базу напрямую если Koin не доступен
                            androidx.room.Room.databaseBuilder(
                                context.applicationContext,
                                LazyBonesDatabase::class.java,
                                "lazybones_db"
                            ).fallbackToDestructiveMigration()
                            .build()
                        }
                    } catch (e: Exception) {
                        Log.e("Widget", "Error getting/creating database", e)
                        null
                    }
                }

                if (db == null) {
                    Log.w("Widget", "Database is null, using default values")
                    val defaultViews = RemoteViews(context.packageName, R.layout.widget_layout)
                    defaultViews.setTextViewText(R.id.widget_motivation_text, "Не пора ли что-нибудь запланировать, LABотряс?")
                    defaultViews.setTextViewText(R.id.widget_good_count, "0")
                    defaultViews.setTextViewText(R.id.widget_bad_count, "0")
                    
                    val defaultIntent = Intent(context, MainActivity::class.java)
                    val defaultPendingIntent = PendingIntent.getActivity(
                        context,
                        0,
                        defaultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    defaultViews.setOnClickPendingIntent(R.id.widget_open_app, defaultPendingIntent)
                    
                    appWidgetManager.updateAppWidget(appWidgetId, defaultViews)
                    return@launch
                }

                // Получаем посты и незавершенные пункты плана
                Log.d("Widget", "Fetching data from database")
                val (posts, currentPlanItems) = withContext(Dispatchers.IO) {
                    try {
                        val postsResult = db.postDao().getAllPostsSync()
                        val planItemsResult = db.planItemDao().getAllSync()
                        Log.d("Widget", "Fetched ${postsResult.size} posts and ${planItemsResult.size} plan items")
                        Pair(postsResult, planItemsResult)
                    } catch (e: Exception) {
                        Log.e("Widget", "Error getting data", e)
                        Pair(emptyList<Post>(), emptyList<PlanItem>())
                    }
                }

                // Фильтруем посты по сегодняшней дате
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val todayStart = calendar.time
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                calendar.add(Calendar.MILLISECOND, -1)
                val todayEnd = calendar.time

                // Ищем только отчет на сегодня (good/bad items, без checklist)
                // Good/bad всегда берутся из отчета, а не из плана
                val todayReport = posts.firstOrNull { post ->
                    val postDate = post.date
                    val isToday = postDate >= todayStart && postDate <= todayEnd
                    val noChecklist = post.checklist.isEmpty()
                    val hasGoodOrBad = post.goodItems.isNotEmpty() || post.badItems.isNotEmpty()
                    Log.d("Widget", "Checking post: date=$postDate, isToday=$isToday, checklistSize=${post.checklist.size}, goodItems=${post.goodItems.size}, badItems=${post.badItems.size}")
                    isToday && noChecklist && hasGoodOrBad
                }

                Log.d("Widget", "Found report: ${todayReport != null}, current plan items: ${currentPlanItems.size}")
                
                val updatedViews = RemoteViews(context.packageName, R.layout.widget_layout)
                
                // Используем данные только из отчета (goodItems/badItems)
                val goodCount = todayReport?.goodCount ?: 0
                val badCount = todayReport?.badCount ?: 0
                
                Log.d("Widget", "Final counts: good=$goodCount, bad=$badCount")
                
                // Мотивационный текст - используем текущие пункты плана, если они есть
                val motivationText = try {
                    // Приоритет: текущие незавершенные пункты плана > сохраненный план > дефолтное сообщение
                    when {
                        currentPlanItems.isNotEmpty() -> {
                            // Используем текущие пункты плана
                            val randomItem = currentPlanItems.random().text
                            val messages = listOf(
                                "Эй, а ты не забыл сделать «$randomItem»?",
                                "Пора взяться за «$randomItem»!",
                                "Не забудь про «$randomItem», дружище!",
                                "Как насчет «$randomItem»?",
                                "Время для «$randomItem»!",
                                "Твоя очередь: «$randomItem»!"
                            )
                            messages.random()
                        }
                        // Убрали логику с todayPlan - мотивационные сообщения только из текущих пунктов плана
                        else -> {
                            val messages = listOf(
                                "Не пора ли что-нибудь запланировать, LABотряс?",
                                "Планы не строятся сами по себе, друг мой!",
                                "Пора наметить цели на сегодня!",
                                "Планирование — это первый шаг к успеху!",
                                "Что будем делать сегодня, ленивый костяк?"
                            )
                            messages.random()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("Widget", "Error generating motivation text", e)
                    "Не пора ли что-нибудь запланировать, LABотряс?"
                }
                
                Log.d("Widget", "Motivation text: $motivationText")
                
                updatedViews.setTextViewText(R.id.widget_motivation_text, motivationText)
                updatedViews.setTextViewText(R.id.widget_good_count, goodCount.toString())
                updatedViews.setTextViewText(R.id.widget_bad_count, badCount.toString())
                
                // Прогресс-бар - всегда показываем, так как RemoteViews не поддерживает динамическое изменение layout_weight
                // В layout уже заданы фиксированные веса 50/50, которые будут корректно работать
                val total = goodCount + badCount
                if (total > 0) {
                    // Вычисляем проценты для логирования
                    val goodPercent = (goodCount.toFloat() / total * 100).coerceIn(0f, 100f)
                    val badPercent = (badCount.toFloat() / total * 100).coerceIn(0f, 100f)
                    
                    // Показываем прогресс-бар (с фиксированными весами из layout)
                    updatedViews.setViewVisibility(R.id.widget_progress_container, android.view.View.VISIBLE)
                    Log.d("Widget", "Progress: good=$goodCount ($goodPercent%), bad=$badCount ($badPercent%)")
                } else {
                    // Скрываем прогресс-бар, если нет данных
                    updatedViews.setViewVisibility(R.id.widget_progress_container, android.view.View.GONE)
                }
                
                // Кнопка открытия приложения
                val updateIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                val updatePendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    updateIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                updatedViews.setOnClickPendingIntent(R.id.widget_open_app, updatePendingIntent)
                
                // Делаем мотивационный текст кликабельным для обновления виджета
                val refreshIntent = Intent("com.katapandroid.lazybones.WIDGET_UPDATE").apply {
                    setComponent(android.content.ComponentName(context, LazyBonesWidgetProvider::class.java))
                }
                val refreshPendingIntent = PendingIntent.getBroadcast(
                    context,
                    0,
                    refreshIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                updatedViews.setOnClickPendingIntent(R.id.widget_motivation_text, refreshPendingIntent)
                
                Log.d("Widget", "Updating widget with final data: good=$goodCount, bad=$badCount")
                appWidgetManager.updateAppWidget(appWidgetId, updatedViews)
                Log.d("Widget", "Widget updated successfully with data")
            } catch (e: Exception) {
                Log.e("Widget", "Error updating widget", e)
                // В случае ошибки показываем дефолтные значения
                val errorViews = RemoteViews(context.packageName, R.layout.widget_layout)
                errorViews.setTextViewText(R.id.widget_motivation_text, "Не пора ли что-нибудь запланировать, LABотряс?")
                errorViews.setTextViewText(R.id.widget_good_count, "0")
                errorViews.setTextViewText(R.id.widget_bad_count, "0")
                
                val errorIntent = Intent(context, MainActivity::class.java)
                val errorPendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    errorIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                errorViews.setOnClickPendingIntent(R.id.widget_open_app, errorPendingIntent)
                
                appWidgetManager.updateAppWidget(appWidgetId, errorViews)
            }
        }
    }
}

