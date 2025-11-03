package com.katapandroid.lazybones.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.katapandroid.lazybones.data.SettingsRepository
import com.katapandroid.lazybones.ui.theme.LazyBonesTheme

class WidgetConfigureActivity : ComponentActivity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    private fun updateCurrentWidget() {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        
        // Проверяем, какой это виджет - обычный или узкий
        val normalWidgetIds = appWidgetManager.getAppWidgetIds(
            android.content.ComponentName(this, LazyBonesWidgetProvider::class.java)
        )
        val narrowWidgetIds = appWidgetManager.getAppWidgetIds(
            android.content.ComponentName(this, LazyBonesWidgetProviderNarrow::class.java)
        )
        
        when {
            appWidgetId in normalWidgetIds -> {
                LazyBonesWidgetProvider.updateWidget(this, appWidgetManager, appWidgetId)
            }
            appWidgetId in narrowWidgetIds -> {
                LazyBonesWidgetProviderNarrow.updateWidget(this, appWidgetManager, appWidgetId)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Проверяем переданный widget ID из разных источников
        // Стандартный способ (при добавлении виджета)
        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }
        
        // Альтернативный способ (для Samsung и других лаунчеров)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        }
        
        // Если ID все еще невалидный - пробуем получить из data
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID && intent.data != null) {
            val dataString = intent.data?.toString()
            dataString?.substringAfterLast("/")?.toIntOrNull()?.let {
                appWidgetId = it
            }
        }

        // Если ID невалидный - закрываем
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            android.util.Log.e("WidgetConfigure", "Invalid widget ID, finishing activity")
            finish()
            return
        }
        
        android.util.Log.d("WidgetConfigure", "Opening settings for widget ID: $appWidgetId")

        val settingsRepository = SettingsRepository(this)
        val initialTheme = settingsRepository.getWidgetTheme(appWidgetId)
        val initialOpacity = settingsRepository.getWidgetOpacity(appWidgetId)

        setContent {
            var theme by remember { mutableStateOf(initialTheme) }
            var opacity by remember { mutableStateOf(initialOpacity) }

            LazyBonesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Text(
                            text = "Настройки виджета",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )

                        // Настройка темы
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "Тема",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Темная тема
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(120.dp),
                                        onClick = {
                                            theme = 0
                                            settingsRepository.setWidgetTheme(appWidgetId, 0)
                                            // Обновляем виджет сразу при изменении темы
                                            updateCurrentWidget()
                                        },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (theme == 0) 
                                                MaterialTheme.colorScheme.primary 
                                            else 
                                                MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color(0xFF000000)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Темная",
                                                color = Color.White,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    
                                    // Светлая тема
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(120.dp),
                                        onClick = {
                                            theme = 1
                                            settingsRepository.setWidgetTheme(appWidgetId, 1)
                                            // Обновляем виджет сразу при изменении темы
                                            updateCurrentWidget()
                                        },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (theme == 1) 
                                                MaterialTheme.colorScheme.primary 
                                            else 
                                                MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color(0xFFFFFFFF)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Светлая",
                                                color = Color.Black,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Настройка прозрачности
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Прозрачность",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "$opacity%",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                Slider(
                                    value = opacity.toFloat(),
                                    onValueChange = { newValue ->
                                        opacity = newValue.toInt()
                                        settingsRepository.setWidgetOpacity(appWidgetId, opacity)
                                        // Обновляем виджет в реальном времени при изменении прозрачности
                                        updateCurrentWidget()
                                    },
                                    valueRange = 20f..100f,
                                    steps = 15, // 20, 25, 30, ..., 100
                                    modifier = Modifier.fillMaxWidth()
                                )
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "20%",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "100%",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                // Превью с текущими настройками
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(80.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val bgAlpha = (opacity * 255 / 100).coerceIn(51, 255)
                                    val bgColor = if (theme == 0) {
                                        Color(android.graphics.Color.argb(bgAlpha, 0, 0, 0))
                                    } else {
                                        Color(android.graphics.Color.argb(bgAlpha, 255, 255, 255))
                                    }
                                    val textColor = if (theme == 0) Color.White else Color.Black
                                    
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(bgColor, RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Превью",
                                            color = textColor,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        // Кнопка сохранения
                        Button(
                            onClick = {
                                // Сохраняем настройки (уже сохранены при изменении)
                                // Обновляем виджет напрямую
                                updateCurrentWidget()
                                
                                // Возвращаем результат
                                val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                                setResult(RESULT_OK, resultValue)
                                finish()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Сохранить",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

