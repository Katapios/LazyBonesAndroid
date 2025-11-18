package com.katapandroid.lazybones.wear

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.material.MaterialTheme as WearMaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.TimeTextDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.wear.compose.material.Card
import androidx.wear.compose.material.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import com.katapandroid.lazybones.wear.data.WearDataRepository
import com.katapandroid.lazybones.wear.model.WatchData
import com.katapandroid.lazybones.wear.screens.PlansScreen
import com.katapandroid.lazybones.wear.screens.ReportsScreen
import com.katapandroid.lazybones.wear.screens.StatsScreen
import com.katapandroid.lazybones.wear.widget.WearWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var wearRepository: WearDataRepository
    private val connectionInfoState = MutableStateFlow("")
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wearRepository = WearDataRepository.getInstance(applicationContext)

        lifecycleScope.launch {
            runCatching { wearRepository.ensureDataFromDataLayer() }
                .onFailure { Log.w("MainActivity", "Failed to pull initial data", it) }
        }

        lifecycleScope.launch {
            updateConnectionInfo()
        }
        
        setContent {
            MainContent(
                repository = wearRepository,
                connectionInfoState = connectionInfoState,
                onUpdateWidget = { updateWidget(it) }
            )
        }
    }
    
    private suspend fun updateConnectionInfo() = withContext(Dispatchers.IO) {
        try {
            val nodeClient = Wearable.getNodeClient(this@MainActivity)
            val nodes = Tasks.await(nodeClient.connectedNodes)
            val capabilityClient = Wearable.getCapabilityClient(this@MainActivity)
            val capabilityInfo = Tasks.await(
                capabilityClient.getCapability(
                    CAPABILITY_LAZYBONES,
                    CapabilityClient.FILTER_REACHABLE
                )
            )
            val status = if (nodes.isNotEmpty()) "📱 Подключено" else "⚠️ Нет подключения"
            connectionInfoState.value = status
            Log.d(
                "MainActivity",
                "Connected nodes=${nodes.map { it.displayName }} capabilityNodes=${capabilityInfo.nodes.size}"
            )
        } catch (e: Exception) {
            connectionInfoState.value = "⚠️ Нет данных"
            Log.e("MainActivity", "Error updating connection info", e)
        }
    }

    private fun updateWidget(data: WatchData) {
        try {
            val manager = AppWidgetManager.getInstance(this)
            val widgetIds = manager.getAppWidgetIds(
                ComponentName(this, WearWidgetProvider::class.java)
            )
            if (widgetIds.isNotEmpty()) {
                WearWidgetProvider().onUpdate(this, manager, widgetIds)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error updating widget", e)
        }
    }

    companion object {
        private const val CAPABILITY_LAZYBONES = "lazybones_data_sync"
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MainContent(
    repository: WearDataRepository,
    connectionInfoState: MutableStateFlow<String>,
    onUpdateWidget: (WatchData) -> Unit
) {
    val dataState by repository.data.collectAsState()
    val connectionInfo by connectionInfoState.collectAsState("")

    LaunchedEffect(dataState) {
        if (dataState.hasMeaningfulContent()) {
            onUpdateWidget(dataState)
        }
    }

    WearMaterialTheme {
        Scaffold(
            timeText = {
                TimeText(
                    timeTextStyle = TimeTextDefaults.timeTextStyle(
                        color = WearMaterialTheme.colors.primary
                    )
                )
            }
        ) {
            val pagerState = rememberPagerState(pageCount = { 4 })
            
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> MainScreen(
                        context = LocalContext.current,
                        goodCount = dataState.goodCount,
                        badCount = dataState.badCount,
                        reportStatus = dataState.reportStatus,
                        poolStatus = dataState.poolStatus,
                        timerText = dataState.timerText,
                        goodItems = dataState.goodItems,
                        badItems = dataState.badItems,
                        connectionInfo = connectionInfo
                    )
                    1 -> PlansScreen(plans = dataState.plans)
                    2 -> ReportsScreen(reports = dataState.reports)
                    3 -> StatsScreen(
                        reports = dataState.reports,
                        plans = dataState.plans,
                        reportStatus = dataState.reportStatus
                    )
                }
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(4) { index ->
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .size(4.dp)
                            .background(
                                color = if (pagerState.currentPage == index) 
                                    WearMaterialTheme.colors.primary
                                else 
                                    WearMaterialTheme.colors.onSurface.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    context: android.content.Context,
    goodCount: Int = 0,
    badCount: Int = 0,
    reportStatus: String? = null,
    poolStatus: String? = null,
    timerText: String? = null,
    goodItems: List<String> = emptyList(),
    badItems: List<String> = emptyList(),
    connectionInfo: String = ""
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        fun translateStatus(status: String?): String {
            return when (status?.uppercase()) {
                "PUBLISHED" -> "Опубликован"
                "SAVED" -> "Сохранён"
                "DRAFT" -> "Черновик"
                "IN_PROGRESS" -> "Заполняется"
                "NOT_FILLED" -> "Не заполнен"
                "NONE" -> "Нет отчёта"
                null -> "Нет данных"
                else -> status
            }
        }
        
        fun translatePoolStatus(status: String?): String {
            return when (status) {
                "ACTIVE" -> "Активен"
                "BEFORE_START" -> "До начала"
                "AFTER_END" -> "Завершён"
                null -> "Нет данных"
                else -> status
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "✓",
                    fontSize = 24.sp,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$goodCount",
                    fontSize = 20.sp,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Хорошо",
                    fontSize = 12.sp,
                    color = Color(0xFF4CAF50)
                )
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "✗",
                    fontSize = 24.sp,
                    color = Color(0xFFF44336),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$badCount",
                    fontSize = 20.sp,
                    color = Color(0xFFF44336),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Плохо",
                    fontSize = 12.sp,
                    color = Color(0xFFF44336)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(4.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = { }
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Статус отчёта",
                    fontSize = 10.sp,
                    color = WearMaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = translateStatus(reportStatus),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = { }
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Статус пула",
                    fontSize = 10.sp,
                    color = WearMaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = translatePoolStatus(poolStatus),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = { }
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Таймер",
                    fontSize = 10.sp,
                    color = WearMaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = timerText ?: "Нет данных",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun HorizontalDivider() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(vertical = 4.dp)
    )
}

