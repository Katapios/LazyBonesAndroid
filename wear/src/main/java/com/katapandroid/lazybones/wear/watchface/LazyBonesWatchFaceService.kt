package com.katapandroid.lazybones.wear.watchface

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.text.format.DateFormat
import androidx.wear.watchface.*
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyleSchema
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import java.time.ZonedDateTime
import java.util.*

/**
 * Watch Face Service для отображения данных LazyBones на циферблате
 * Реализовано согласно документации androidx.wear.watchface API
 */
class LazyBonesWatchFaceService : WatchFaceService() {
    
    override fun createUserStyleSchema(): UserStyleSchema {
        return UserStyleSchema(emptyList())
    }
    
    override suspend fun createWatchFace(
        surfaceHolder: android.view.SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): WatchFace {
        val renderer = LazyBonesWatchFaceRenderer(
            this,
            surfaceHolder,
            watchState,
            currentUserStyleRepository
        )
        
        // WatchFace конструктор принимает только: watchFaceType (Int) и renderer (Renderer)
        return WatchFace(
            WatchFaceType.DIGITAL,
            renderer
        )
    }
    
    inner class LazyBonesWatchFaceRenderer(
        private val context: Context,
        surfaceHolder: android.view.SurfaceHolder,
        watchState: WatchState,
        currentUserStyleRepository: CurrentUserStyleRepository
    ) : Renderer.CanvasRenderer(
        surfaceHolder,
        currentUserStyleRepository,
        watchState,
        WatchFaceType.DIGITAL,
        interactiveDrawModeUpdateDelayMillis = 16L
    ) {
        
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        
        // Данные из SharedPreferences
        private var goodCount = 0
        private var badCount = 0
        private var reportStatus: String? = null
        private var timerText: String? = null
        private var reportsCount = 0
        private var plansCount = 0
        
        // Paints для рисования
        private val timePaint = Paint().apply {
            color = Color.WHITE
            textSize = 48f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        
        private val datePaint = Paint().apply {
            color = Color.GRAY
            textSize = 20f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        
        private val dataPaint = Paint().apply {
            color = Color.WHITE
            textSize = 16f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        
        private val goodPaint = Paint().apply {
            color = Color.parseColor("#4CAF50")
            textSize = 18f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        
        private val badPaint = Paint().apply {
            color = Color.parseColor("#F44336")
            textSize = 18f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        
        private val statusPaint = Paint().apply {
            color = Color.parseColor("#2196F3")
            textSize = 14f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        
        init {
            scope.launch {
                while (true) {
                    loadDataFromPrefs()
                    invalidate()
                    kotlinx.coroutines.delay(3000)
                }
            }
        }
        
        private fun loadDataFromPrefs() {
            val prefs = context.getSharedPreferences("wear_data", Context.MODE_PRIVATE)
            goodCount = prefs.getInt("goodCount", 0)
            badCount = prefs.getInt("badCount", 0)
            reportStatus = prefs.getString("reportStatus", null)
            timerText = prefs.getString("timerText", null)
            
            try {
                val reportsJson = prefs.getString("reportsJson", "[]") ?: "[]"
                val plansJson = prefs.getString("plansJson", "[]") ?: "[]"
                
                reportsCount = if (reportsJson.isNotEmpty() && reportsJson != "[]") {
                    org.json.JSONArray(reportsJson).length()
                } else {
                    0
                }
                
                plansCount = if (plansJson.isNotEmpty() && plansJson != "[]") {
                    org.json.JSONArray(plansJson).length()
                } else {
                    0
                }
            } catch (e: Exception) {
                android.util.Log.e("LazyBonesWatchFace", "Error parsing counts", e)
            }
        }
        
        override fun render(
            canvas: Canvas,
            bounds: Rect,
            zonedDateTime: ZonedDateTime
        ) {
            val centerX = bounds.width() / 2f
            var currentY = bounds.height() / 2f - 60f
            
            // Время
            val time = DateFormat.format("HH:mm", Date.from(zonedDateTime.toInstant()))
            canvas.drawText(time.toString(), centerX, currentY, timePaint)
            
            currentY += 30f
            
            // Дата
            val date = DateFormat.format("dd.MM", Date.from(zonedDateTime.toInstant()))
            canvas.drawText(date.toString(), centerX, currentY, datePaint)
            
            currentY += 40f
            
            // Good / Bad
            canvas.drawText("✓ $goodCount", centerX - 40f, currentY, goodPaint)
            canvas.drawText("✗ $badCount", centerX + 40f, currentY, badPaint)
            
            currentY += 30f
            
            // Статус отчёта
            val statusText = translateStatus(reportStatus)
            if (statusText.isNotEmpty() && statusText != "Нет данных") {
                canvas.drawText(statusText, centerX, currentY, statusPaint)
                currentY += 25f
            }
            
            // Таймер
            if (timerText != null && timerText!!.isNotEmpty()) {
                canvas.drawText(timerText!!.take(20), centerX, currentY, dataPaint)
                currentY += 25f
            }
            
            // Статистика
            currentY += 10f
            canvas.drawText("Отчётов: $reportsCount", centerX, currentY, dataPaint)
            currentY += 20f
            canvas.drawText("Планов: $plansCount", centerX, currentY, dataPaint)
        }
        
        override fun renderHighlightLayer(
            canvas: Canvas,
            bounds: Rect,
            zonedDateTime: ZonedDateTime
        ) {
            // Пустая реализация для highlight layer
        }
        
        private fun translateStatus(status: String?): String {
            return when (status?.uppercase()) {
                "PUBLISHED" -> "Опубликован"
                "SAVED" -> "Сохранён"
                "DRAFT" -> "Черновик"
                "IN_PROGRESS" -> "Заполняется"
                "NOT_FILLED" -> "Не заполнен"
                "NONE" -> "Нет отчёта"
                null -> ""
                else -> status
            }
        }
        
        override fun onDestroy() {
            super.onDestroy()
            scope.cancel()
        }
    }
}
