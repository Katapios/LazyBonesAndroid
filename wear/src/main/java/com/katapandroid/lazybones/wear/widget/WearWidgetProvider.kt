package com.katapandroid.lazybones.wear.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews

class WearWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
    
    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, com.katapandroid.lazybones.wear.R.layout.wear_widget_layout)
        
        // Загружаем данные из SharedPreferences (синхронизируются с телефоном)
        val prefs = context.getSharedPreferences("wear_data", android.content.Context.MODE_PRIVATE)
        val goodCount = prefs.getInt("goodCount", 0)
        val badCount = prefs.getInt("badCount", 0)
        
        views.setTextViewText(com.katapandroid.lazybones.wear.R.id.widget_title, "LazyBones")
        views.setTextViewText(com.katapandroid.lazybones.wear.R.id.widget_good, "Good: $goodCount")
        views.setTextViewText(com.katapandroid.lazybones.wear.R.id.widget_bad, "Bad: $badCount")
        
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}

