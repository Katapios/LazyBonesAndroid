package com.katapandroid.lazybones.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.wear.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.material.*
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.*
import com.google.android.gms.wearable.CapabilityClient
import com.katapandroid.lazybones.wear.sync.WearDataReceiver
import com.katapandroid.lazybones.wear.screens.PlansScreen
import com.katapandroid.lazybones.wear.screens.ReportsScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class WatchData(
    val goodCount: Int,
    val badCount: Int,
    val reportStatus: String?,
    val poolStatus: String?,
    val timerText: String?,
    val goodItems: List<String>,
    val badItems: List<String>,
    val plans: List<com.katapandroid.lazybones.wear.screens.PlanItem> = emptyList(),
    val reports: List<com.katapandroid.lazybones.wear.screens.ReportItem> = emptyList()
)

class MainActivity : ComponentActivity() {
    private lateinit var dataReceiver: WearDataReceiver
    private val activity = this
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        android.util.Log.d("MainActivity", "✅ MainActivity created")
        
        // WearableListenerService запускается автоматически системой
        android.util.Log.d("MainActivity", "✅ WearableListenerService будет запущен системой автоматически")
        
        // Инициализируем прием данных от телефона
        dataReceiver = WearDataReceiver()
        
        // Настраиваем подробное логирование в receiver
        dataReceiver.onDataReceived = { good, bad ->
            android.util.Log.d("MainActivity", "🎉 WearDataReceiver.onDataReceived: good=$good, bad=$bad")
        }
        dataReceiver.onAllDataReceived = { good, bad, status, pool, timer, goods, bads ->
            android.util.Log.d("MainActivity", "🎉 WearDataReceiver.onAllDataReceived called!")
        }
        
        val dataClient = Wearable.getDataClient(this)
        
        // Добавляем слушатель для всех событий (без фильтра по URI)
        dataClient.addListener(dataReceiver)
        android.util.Log.d("MainActivity", "✅ Data receiver initialized, listener added (listening to all paths)")
        
        // Также добавляем слушатель сообщений
        val messageClient = Wearable.getMessageClient(this)
        messageClient.addListener(dataReceiver)
        android.util.Log.d("MainActivity", "✅ Message receiver also added")
        
        // Проверяем подключение к телефону и регистрируем capability
        lifecycleScope.launch {
            try {
                // Регистрируем capability для связи с телефоном
                val capabilityClient = Wearable.getCapabilityClient(this@MainActivity)
                val capabilityInfo = withContext(Dispatchers.IO) {
                    Tasks.await(capabilityClient.addLocalCapability("lazybones_data_sync"))
                }
                android.util.Log.d("MainActivity", "✅ Capability registered: lazybones_data_sync")
                
                // Проверяем подключенные nodes
                val nodes = withContext(Dispatchers.IO) {
                    Tasks.await(Wearable.getNodeClient(this@MainActivity).connectedNodes)
                }
                if (nodes.isNotEmpty()) {
                    android.util.Log.d("MainActivity", "✅ Connected to phone: ${nodes[0].displayName} (id=${nodes[0].id})")
                } else {
                    android.util.Log.w("MainActivity", "⚠️ No phone connected")
                }
                
                // Проверяем capability на телефоне
                try {
                    val phoneCapability = withContext(Dispatchers.IO) {
                        Tasks.await(capabilityClient.getCapability("lazybones_data_sync", CapabilityClient.FILTER_REACHABLE))
                    }
                    android.util.Log.d("MainActivity", "📱 Phone capability: nodes=${phoneCapability.nodes.size}")
                    if (phoneCapability.nodes.isNotEmpty()) {
                        for (node in phoneCapability.nodes) {
                            android.util.Log.d("MainActivity", "   Phone node: ${node.displayName} (id=${node.id})")
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("MainActivity", "⚠️ Could not check phone capability", e)
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error checking connection", e)
                e.printStackTrace()
            }
        }
        
        setContent {
            val context = this
            
            var dataState by remember { 
                mutableStateOf(WatchData(0, 0, null, null, null, emptyList(), emptyList(), emptyList(), emptyList()))
            }
            
            @OptIn(ExperimentalFoundationApi::class)
            val pagerState = rememberPagerState(pageCount = { 3 })
            
            var connectionInfo by remember { mutableStateOf("") }
            
            // Загружаем начальные данные из SharedPreferences и периодически обновляем
            LaunchedEffect(Unit) {
            fun loadDataFromPrefs() {
                val prefs = context.getSharedPreferences("wear_data", android.content.Context.MODE_PRIVATE)
                
                // Парсим планы
                val plansJson = prefs.getString("plansJson", "[]") ?: "[]"
                android.util.Log.d("MainActivity", "📖 Reading plans from prefs: $plansJson")
                val plans = try {
                    if (plansJson.isNotEmpty() && plansJson != "[]") {
                        val jsonArray = JSONArray(plansJson)
                        android.util.Log.d("MainActivity", "📖 Plans array length: ${jsonArray.length()}")
                        (0 until jsonArray.length()).map { i ->
                            val planObj = jsonArray.getJSONObject(i)
                            val planId = planObj.getLong("id")
                            val planText = planObj.getString("text")
                            val planDate = if (planObj.has("date") && !planObj.isNull("date")) {
                                try {
                                    planObj.getLong("date")
                                } catch (e: Exception) {
                                    android.util.Log.e("MainActivity", "Error reading plan date", e)
                                    0L
                                }
                            } else {
                                0L
                            }
                            android.util.Log.d("MainActivity", "📖 Plan $i: id=$planId, text='$planText', date=$planDate (${if (planDate > 0) java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault()).format(java.util.Date(planDate)) else "no date"})")
                            com.katapandroid.lazybones.wear.screens.PlanItem(
                                id = planId,
                                text = planText,
                                date = planDate
                            )
                        }
                    } else {
                        android.util.Log.d("MainActivity", "⚠️ Plans JSON empty in prefs")
                        emptyList()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "❌ Error parsing plans from prefs", e)
                    e.printStackTrace()
                    emptyList()
                }
                
                // Парсим отчёты
                val reportsJson = prefs.getString("reportsJson", "[]") ?: "[]"
                android.util.Log.d("MainActivity", "📖 Reading reports from prefs: ${reportsJson.take(200)}")
                val reports = try {
                    if (reportsJson.isNotEmpty() && reportsJson != "[]") {
                        val jsonArray = JSONArray(reportsJson)
                        android.util.Log.d("MainActivity", "📖 Reports array length: ${jsonArray.length()}")
                        (0 until jsonArray.length()).map { i ->
                            val reportObj = jsonArray.getJSONObject(i)
                            android.util.Log.d("MainActivity", "📖 Parsing report $i")
                        val goodItemsList = mutableListOf<String>()
                        if (reportObj.has("goodItems")) {
                            val goodItemsArray = reportObj.getJSONArray("goodItems")
                            for (j in 0 until goodItemsArray.length()) {
                                goodItemsList.add(goodItemsArray.getString(j))
                            }
                        }
                        val badItemsList = mutableListOf<String>()
                        if (reportObj.has("badItems")) {
                            val badItemsArray = reportObj.getJSONArray("badItems")
                            for (j in 0 until badItemsArray.length()) {
                                badItemsList.add(badItemsArray.getString(j))
                            }
                        }
                        val checklistList = mutableListOf<String>()
                        if (reportObj.has("checklist")) {
                            val checklistArray = reportObj.getJSONArray("checklist")
                            for (j in 0 until checklistArray.length()) {
                                checklistList.add(checklistArray.getString(j))
                            }
                        }
                            val reportId = reportObj.getLong("id")
                            val reportDate = reportObj.getLong("date")
                            val reportGoodCount = reportObj.getInt("goodCount")
                            val reportBadCount = reportObj.getInt("badCount")
                            android.util.Log.d("MainActivity", "📖 Report $i: id=$reportId, date=$reportDate, good=$reportGoodCount, bad=$reportBadCount")
                            com.katapandroid.lazybones.wear.screens.ReportItem(
                                id = reportId,
                                date = reportDate,
                                goodCount = reportGoodCount,
                                badCount = reportBadCount,
                                published = reportObj.getBoolean("published"),
                                goodItems = goodItemsList,
                                badItems = badItemsList,
                                checklist = checklistList
                            )
                        }
                    } else {
                        android.util.Log.d("MainActivity", "⚠️ Reports JSON empty in prefs")
                        emptyList()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "❌ Error parsing reports from prefs", e)
                    e.printStackTrace()
                    emptyList()
                }
                
                val newData = WatchData(
                    goodCount = prefs.getInt("goodCount", 0),
                    badCount = prefs.getInt("badCount", 0),
                    reportStatus = prefs.getString("reportStatus", null),
                    poolStatus = prefs.getString("poolStatus", null),
                    timerText = prefs.getString("timerText", null),
                    goodItems = prefs.getStringSet("goodItems", emptySet())?.toList() ?: emptyList(),
                    badItems = prefs.getStringSet("badItems", emptySet())?.toList() ?: emptyList(),
                    plans = plans,
                    reports = reports
                )
                
                // ВСЕГДА логируем что читаем
                android.util.Log.d("MainActivity", "📖 Reading from prefs: good=${newData.goodCount}, bad=${newData.badCount}, timer=${newData.timerText ?: "null"}, status=${newData.reportStatus ?: "null"}")
                
                // ВСЕГДА обновляем состояние (чтобы UI обновлялся)
                val current = dataState
                // Обновляем если данные изменились ИЛИ если это первое чтение (goodCount=0 и badCount=0)
                val isFirstRead = current.goodCount == 0 && current.badCount == 0 && current.reportStatus == null && current.timerText == null
                // Всегда обновляем, если данные изменились (включая планы и отчёты)
                val plansChanged = newData.plans.size != current.plans.size || 
                    newData.plans != current.plans
                val reportsChanged = newData.reports.size != current.reports.size || 
                    newData.reports != current.reports
                    
                // КРИТИЧНО: НЕ перезаписываем планы и отчёты, если они пустые, но у нас уже есть данные
                // Это предотвращает исчезновение данных
                // ВАЖНО: Если у нас уже есть данные, НЕ обновляем их пустыми значениями
                val shouldUpdatePlans = if (newData.plans.isEmpty() && current.plans.isNotEmpty()) {
                    android.util.Log.d("MainActivity", "⚠️ [PREFS] New plans empty but current has ${current.plans.size}, KEEPING CURRENT - NO UPDATE")
                    false
                } else if (newData.plans.isNotEmpty() && plansChanged) {
                    android.util.Log.d("MainActivity", "✅ [PREFS] Plans changed and new data is not empty, updating")
                    true
                } else {
                    false
                }
                
                val shouldUpdateReports = if (newData.reports.isEmpty() && current.reports.isNotEmpty()) {
                    android.util.Log.d("MainActivity", "⚠️ [PREFS] New reports empty but current has ${current.reports.size}, KEEPING CURRENT - NO UPDATE")
                    false
                } else if (newData.reports.isNotEmpty() && reportsChanged) {
                    android.util.Log.d("MainActivity", "✅ [PREFS] Reports changed and new data is not empty, updating")
                    true
                } else {
                    false
                }
                    
                // Обновляем только если изменились другие данные (goodCount, badCount, etc) ИЛИ если планы/отчёты действительно изменились (и не пустые)
                // НО: если планы/отчёты пустые, а у нас уже есть данные - НЕ обновляем вообще
                val shouldUpdate = isFirstRead || 
                    newData.goodCount != current.goodCount || 
                    newData.badCount != current.badCount ||
                    newData.reportStatus != current.reportStatus ||
                    newData.poolStatus != current.poolStatus ||
                    newData.timerText != current.timerText ||
                    newData.goodItems != current.goodItems ||
                    newData.badItems != current.badItems ||
                    shouldUpdatePlans ||
                    shouldUpdateReports
                
                if (shouldUpdate) {
                    // ВСЕГДА сохраняем текущие планы/отчёты, если новые пустые
                    val finalPlans = if (newData.plans.isEmpty() && current.plans.isNotEmpty()) {
                        android.util.Log.d("MainActivity", "🔄 [PREFS] Keeping existing plans: ${current.plans.size}")
                        current.plans
                    } else {
                        newData.plans
                    }
                    
                    val finalReports = if (newData.reports.isEmpty() && current.reports.isNotEmpty()) {
                        android.util.Log.d("MainActivity", "🔄 [PREFS] Keeping existing reports: ${current.reports.size}")
                        current.reports
                    } else {
                        newData.reports
                    }
                    
                    val finalData = WatchData(
                        newData.goodCount,
                        newData.badCount,
                        newData.reportStatus,
                        newData.poolStatus,
                        newData.timerText,
                        newData.goodItems,
                        newData.badItems,
                        finalPlans,
                        finalReports
                    )
                    
                    android.util.Log.d("MainActivity", "🔄 [PREFS] Updating UI: plans=${finalData.plans.size}, reports=${finalData.reports.size}")
                    android.util.Log.d("MainActivity", "   Current: plans=${current.plans.size}, reports=${current.reports.size}")
                    android.util.Log.d("MainActivity", "   New: plans=${newData.plans.size}, reports=${newData.reports.size}")
                    android.util.Log.d("MainActivity", "   Final: plans=${finalData.plans.size}, reports=${finalData.reports.size}")
                    dataState = finalData
                    android.util.Log.d("MainActivity", "✅ [PREFS] dataState updated!")
                } else {
                    android.util.Log.d("MainActivity", "ℹ️ [PREFS] Data unchanged, no UI update needed")
                }
            }
                
                // Загружаем сразу
                loadDataFromPrefs()
                
                // Периодически проверяем обновления (каждые 30 секунд, очень редко чтобы не перезаписывать данные)
                // ВАЖНО: Читаем только если данные действительно пустые, иначе пропускаем
                while (true) {
                    delay(30000) // Увеличено до 30 секунд
                    // Читаем ТОЛЬКО если данные действительно пустые
                    val currentState = dataState
                    if (currentState.plans.isEmpty() && currentState.reports.isEmpty() && 
                        currentState.goodCount == 0 && currentState.badCount == 0 && 
                        currentState.timerText == null) {
                        android.util.Log.d("MainActivity", "📖 Data completely empty, reading from prefs...")
                        loadDataFromPrefs()
                    } else {
                        android.util.Log.d("MainActivity", "ℹ️ Data present (plans=${currentState.plans.size}, reports=${currentState.reports.size}), SKIPPING prefs read to avoid overwrite")
                    }
                    
                    // Также пытаемся прочитать напрямую из Data Layer - ПРОБУЕМ ВСЕ ВОЗМОЖНЫЕ URI
                    try {
                        withContext(Dispatchers.IO) {
                            val dataClient = Wearable.getDataClient(context)
                            
                            // Пробуем разные варианты URI
                            // Также пытаемся получить node ID телефона
                            val phoneNodeId = try {
                                val nodes = Tasks.await(Wearable.getNodeClient(context).connectedNodes)
                                if (nodes.isNotEmpty()) {
                                    val nodeId = nodes[0].id
                                    android.util.Log.d("MainActivity", "📱 Phone node ID: $nodeId")
                                    nodeId
                                } else {
                                    null
                                }
                            } catch (e: Exception) {
                                android.util.Log.d("MainActivity", "⚠️ Could not get phone node: ${e.message}")
                                null
                            }
                            
                            val uriVariants = mutableListOf(
                                android.net.Uri.parse("wear:/lazybones/data"),
                                android.net.Uri.parse("wear://*/lazybones/data"),
                                android.net.Uri.parse("/lazybones/data")
                            )
                            
                            // Если знаем node ID, пробуем его тоже
                            if (phoneNodeId != null) {
                                uriVariants.add(android.net.Uri.parse("wear://$phoneNodeId/lazybones/data"))
                                android.util.Log.d("MainActivity", "➕ Added URI with phone node ID: wear://$phoneNodeId/lazybones/data")
                            } else {
                                android.util.Log.w("MainActivity", "⚠️ Could not get phone node ID!")
                            }
                            
                            // Также пробуем получить node ID из логируемых URI на телефоне
                            // Из логов видно: wear://eaa67cb/lazybones/data
                            val knownPhoneNodeId = "eaa67cb"
                            uriVariants.add(android.net.Uri.parse("wear://$knownPhoneNodeId/lazybones/data"))
                            android.util.Log.d("MainActivity", "➕ Added known phone node URI: wear://$knownPhoneNodeId/lazybones/data")
                            
                            for (uri in uriVariants) {
                                try {
                                    android.util.Log.d("MainActivity", "🔍 Trying to read from Data Layer: $uri")
                                    val dataItem = Tasks.await(dataClient.getDataItem(uri))
                                    
                                    if (dataItem != null) {
                                        android.util.Log.d("MainActivity", "✅✅✅ FOUND DATA ITEM! URI: ${dataItem.uri}")
                                        // Обрабатываем данные
                                        val dataMap = com.google.android.gms.wearable.DataMapItem.fromDataItem(dataItem).dataMap
                                        val dataString = dataMap.getString("data")
                                        
                                        if (dataString != null) {
                                            android.util.Log.d("MainActivity", "✅ Reading data from found item")
                                            val json = org.json.JSONObject(dataString)
                                            val goodCount = json.getInt("goodCount")
                                            val badCount = json.getInt("badCount")
                                            val reportStatus = if (json.has("reportStatus")) json.getString("reportStatus") else null
                                            val poolStatus = if (json.has("poolStatus")) json.getString("poolStatus") else null
                                            val timerText = if (json.has("timerText")) json.getString("timerText") else null
                                            
                                            val goodItems = mutableListOf<String>()
                                            if (json.has("goodItems")) {
                                                val goodItemsArray = json.getJSONArray("goodItems")
                                                for (i in 0 until goodItemsArray.length()) {
                                                    goodItems.add(goodItemsArray.getString(i))
                                                }
                                            }
                                            
                                            val badItems = mutableListOf<String>()
                                            if (json.has("badItems")) {
                                                val badItemsArray = json.getJSONArray("badItems")
                                                for (i in 0 until badItemsArray.length()) {
                                                    badItems.add(badItemsArray.getString(i))
                                                }
                                            }
                                            
                                            android.util.Log.d("MainActivity", "✅✅✅ PARSED DATA: good=$goodCount, bad=$badCount, timer=$timerText")
                                            
                                            // Сохраняем в SharedPreferences
                                            context.getSharedPreferences("wear_data", android.content.Context.MODE_PRIVATE)
                                                .edit()
                                                .putInt("goodCount", goodCount)
                                                .putInt("badCount", badCount)
                                                .putString("reportStatus", reportStatus)
                                                .putString("poolStatus", poolStatus)
                                                .putString("timerText", timerText)
                                                .putStringSet("goodItems", goodItems.toSet())
                                                .putStringSet("badItems", badItems.toSet())
                                                .apply()
                                            
                                            android.util.Log.d("MainActivity", "💾 Saved to SharedPreferences from direct read!")
                                            
                                            // Обновляем состояние
                                            val newData = WatchData(goodCount, badCount, reportStatus, poolStatus, timerText, goodItems, badItems, emptyList(), emptyList())
                                            val current = dataState
                                            if (newData.goodCount != current.goodCount || 
                                                newData.badCount != current.badCount ||
                                                newData.timerText != current.timerText ||
                                                newData.reportStatus != current.reportStatus ||
                                                newData.poolStatus != current.poolStatus) {
                                                android.util.Log.d("MainActivity", "🔄🔄🔄 UPDATING UI FROM DIRECT READ!")
                                                dataState = newData
                                            }
                                            
                                            // Выходим после успешного чтения
                                            break
                                        }
                                    }
                                } catch (e: com.google.android.gms.common.api.ApiException) {
                                    android.util.Log.d("MainActivity", "ℹ️ ApiException for $uri (status=${e.statusCode})")
                                } catch (e: java.util.concurrent.ExecutionException) {
                                    val cause = e.cause
                                    if (cause is com.google.android.gms.common.api.ApiException) {
                                        android.util.Log.d("MainActivity", "ℹ️ ExecutionException -> ApiException for $uri (status=${cause.statusCode})")
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.d("MainActivity", "ℹ️ Exception for $uri: ${e.message}")
                                }
                            }
                            
                            // Также пробуем получить ВСЕ data items (включая с разных nodes)
                            try {
                                android.util.Log.d("MainActivity", "🔍 Trying to get ALL data items...")
                                val allDataItems = Tasks.await(dataClient.getDataItems())
                                android.util.Log.d("MainActivity", "📦 Found ${allDataItems.count} total data items")
                                
                                if (allDataItems.count > 0) {
                                    for (item in allDataItems) {
                                        android.util.Log.d("MainActivity", "   📦 Data item: ${item.uri}, path: ${item.uri.path}, host: ${item.uri.host}")
                                        
                                        // Проверяем все пути, которые могут содержать наши данные
                                        val itemPath = item.uri.path ?: ""
                                        if (itemPath.contains("lazybones") || itemPath.contains("data")) {
                                            android.util.Log.d("MainActivity", "✅✅✅ FOUND POTENTIAL DATA ITEM: ${item.uri}")
                                            
                                            try {
                                                val dataMap = com.google.android.gms.wearable.DataMapItem.fromDataItem(item).dataMap
                                                
                                                // Пробуем прочитать как строку "data"
                                                val dataString = dataMap.getString("data")
                                                if (dataString != null) {
                                                    android.util.Log.d("MainActivity", "✅ Found 'data' field in DataMap!")
                                                    try {
                                                        val json = org.json.JSONObject(dataString)
                                                        val goodCount = json.getInt("goodCount")
                                                        val badCount = json.getInt("badCount")
                                                        
                                                        android.util.Log.d("MainActivity", "✅✅✅ PARSED FROM ALL ITEMS: good=$goodCount, bad=$badCount")
                                                        
                                                        val reportStatus = if (json.has("reportStatus")) json.getString("reportStatus") else null
                                                        val poolStatus = if (json.has("poolStatus")) json.getString("poolStatus") else null
                                                        val timerText = if (json.has("timerText")) json.getString("timerText") else null
                                                        
                                                        val goodItems = mutableListOf<String>()
                                                        if (json.has("goodItems")) {
                                                            val goodItemsArray = json.getJSONArray("goodItems")
                                                            for (i in 0 until goodItemsArray.length()) {
                                                                goodItems.add(goodItemsArray.getString(i))
                                                            }
                                                        }
                                                        
                                                        val badItems = mutableListOf<String>()
                                                        if (json.has("badItems")) {
                                                            val badItemsArray = json.getJSONArray("badItems")
                                                            for (i in 0 until badItemsArray.length()) {
                                                                badItems.add(badItemsArray.getString(i))
                                                            }
                                                        }
                                                        
                                                        // Сохраняем и обновляем
                                                        context.getSharedPreferences("wear_data", android.content.Context.MODE_PRIVATE)
                                                            .edit()
                                                            .putInt("goodCount", goodCount)
                                                            .putInt("badCount", badCount)
                                                            .putString("reportStatus", reportStatus)
                                                            .putString("poolStatus", poolStatus)
                                                            .putString("timerText", timerText)
                                                            .putStringSet("goodItems", goodItems.toSet())
                                                            .putStringSet("badItems", badItems.toSet())
                                                            .apply()
                                                        
                                                        android.util.Log.d("MainActivity", "💾 Saved to SharedPreferences!")
                                                        
                                                        dataState = WatchData(
                                                            goodCount,
                                                            badCount,
                                                            reportStatus,
                                                            poolStatus,
                                                            timerText,
                                                            goodItems,
                                                            badItems,
                                                            emptyList(),
                                                            emptyList()
                                                        )
                                                        
                                                        android.util.Log.d("MainActivity", "🔄🔄🔄 UPDATED FROM ALL ITEMS!")
                                                        break // Выходим после успешного чтения
                                                    } catch (jsonE: Exception) {
                                                        android.util.Log.d("MainActivity", "⚠️ Error parsing JSON: ${jsonE.message}")
                                                    }
                                                } else {
                                                    android.util.Log.d("MainActivity", "ℹ️ No 'data' field in DataMap, trying all keys...")
                                                    // Пробуем все ключи в DataMap
                                                    for (key in dataMap.keySet()) {
                                                        android.util.Log.d("MainActivity", "   Key: $key = ${dataMap.getString(key)?.take(50)}")
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                android.util.Log.d("MainActivity", "⚠️ Error reading DataMap: ${e.message}")
                                            }
                                        }
                                    }
                                } else {
                                    android.util.Log.d("MainActivity", "ℹ️ No data items found at all in Data Layer")
                                }
                                allDataItems.close()
                            } catch (e: Exception) {
                                android.util.Log.d("MainActivity", "ℹ️ Could not get all data items: ${e.message}")
                                e.printStackTrace()
                            }
                        }
                    } catch (e: Exception) {
                        // Игнорируем ошибки чтения - это нормально, если данных еще нет
                        android.util.Log.d("MainActivity", "ℹ️ Error reading from Data Layer: ${e.message}")
                    }
                }
            }
            
            // Устанавливаем callback для получения данных через DataReceiver
            DisposableEffect(dataReceiver) {
                val callback: (Int, Int, String?, String?, String?, List<String>, List<String>) -> Unit =
                    { good, bad, status, pool, timer, goods, bads ->
                        android.util.Log.d("MainActivity", "🎉 ====== DATA RECEIVED IN MAINACTIVITY ======")
                        android.util.Log.d("MainActivity", "   good=$good, bad=$bad, status=$status, pool=$pool, timer=$timer")

                        // Парсим планы и отчёты из SharedPreferences
                        val prefs = context.getSharedPreferences("wear_data", android.content.Context.MODE_PRIVATE)
                        val plansJson = prefs.getString("plansJson", "[]") ?: "[]"
                        android.util.Log.d("MainActivity", "📋 [CALLBACK] Parsing plans from JSON: $plansJson")
                        val plans = try {
                            if (plansJson.isNotEmpty() && plansJson != "[]") {
                                val jsonArray = JSONArray(plansJson)
                                android.util.Log.d("MainActivity", "📋 [CALLBACK] Plans JSON array length: ${jsonArray.length()}")
                                (0 until jsonArray.length()).map { i ->
                                    val planObj = jsonArray.getJSONObject(i)
                                    val planId = planObj.getLong("id")
                                    val planText = planObj.getString("text")
                                    val planDate = if (planObj.has("date") && !planObj.isNull("date")) {
                                        try {
                                            planObj.getLong("date")
                                        } catch (e: Exception) {
                                            android.util.Log.e("MainActivity", "Error reading plan date in callback", e)
                                            0L
                                        }
                                    } else {
                                        0L
                                    }
                                    android.util.Log.d("MainActivity", "📋 [CALLBACK] Parsed plan $i: id=$planId, text='$planText', date=$planDate (${if (planDate > 0) java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault()).format(java.util.Date(planDate)) else "no date"})")
                                    com.katapandroid.lazybones.wear.screens.PlanItem(
                                        id = planId,
                                        text = planText,
                                        date = planDate
                                    )
                                }
                            } else {
                                android.util.Log.d("MainActivity", "⚠️ [CALLBACK] Plans JSON is empty")
                                emptyList()
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("MainActivity", "❌ [CALLBACK] Error parsing plans JSON", e)
                            e.printStackTrace()
                            emptyList()
                        }
                        
                        val reportsJson = prefs.getString("reportsJson", "[]") ?: "[]"
                        android.util.Log.d("MainActivity", "📋 [CALLBACK] Parsing reports from JSON: ${reportsJson.take(200)}")
                        val reports = try {
                            if (reportsJson.isNotEmpty() && reportsJson != "[]") {
                                val jsonArray = JSONArray(reportsJson)
                                android.util.Log.d("MainActivity", "📋 [CALLBACK] Reports JSON array length: ${jsonArray.length()}")
                                (0 until jsonArray.length()).map { i ->
                                    val reportObj = jsonArray.getJSONObject(i)
                                    android.util.Log.d("MainActivity", "📋 [CALLBACK] Parsing report $i")
                                    val goodItemsList = mutableListOf<String>()
                                    if (reportObj.has("goodItems")) {
                                        val goodItemsArray = reportObj.getJSONArray("goodItems")
                                        for (j in 0 until goodItemsArray.length()) {
                                            goodItemsList.add(goodItemsArray.getString(j))
                                        }
                                    }
                                    val badItemsList = mutableListOf<String>()
                                    if (reportObj.has("badItems")) {
                                        val badItemsArray = reportObj.getJSONArray("badItems")
                                        for (j in 0 until badItemsArray.length()) {
                                            badItemsList.add(badItemsArray.getString(j))
                                        }
                                    }
                                    val checklistList = mutableListOf<String>()
                                    if (reportObj.has("checklist")) {
                                        val checklistArray = reportObj.getJSONArray("checklist")
                                        for (j in 0 until checklistArray.length()) {
                                            checklistList.add(checklistArray.getString(j))
                                        }
                                    }
                                    val reportId = reportObj.getLong("id")
                                    val reportDate = reportObj.getLong("date")
                                    val reportGoodCount = reportObj.getInt("goodCount")
                                    val reportBadCount = reportObj.getInt("badCount")
                                    android.util.Log.d("MainActivity", "📋 [CALLBACK] Parsed report $i: id=$reportId, date=$reportDate, good=$reportGoodCount, bad=$reportBadCount")
                                    com.katapandroid.lazybones.wear.screens.ReportItem(
                                        id = reportId,
                                        date = reportDate,
                                        goodCount = reportGoodCount,
                                        badCount = reportBadCount,
                                        published = reportObj.getBoolean("published"),
                                        goodItems = goodItemsList,
                                        badItems = badItemsList,
                                        checklist = checklistList
                                    )
                                }
                            } else {
                                android.util.Log.d("MainActivity", "⚠️ [CALLBACK] Reports JSON is empty")
                                emptyList()
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("MainActivity", "❌ [CALLBACK] Error parsing reports JSON", e)
                            e.printStackTrace()
                            emptyList()
                        }
                        
                        // КРИТИЧНО: Сохраняем текущие данные, если новые пустые
                        val currentState = dataState
                        val finalPlans = if (plans.isEmpty() && currentState.plans.isNotEmpty()) {
                            android.util.Log.d("MainActivity", "⚠️ [CALLBACK] New plans empty but current has ${currentState.plans.size}, KEEPING CURRENT")
                            currentState.plans
                        } else {
                            plans
                        }
                        
                        val finalReports = if (reports.isEmpty() && currentState.reports.isNotEmpty()) {
                            android.util.Log.d("MainActivity", "⚠️ [CALLBACK] New reports empty but current has ${currentState.reports.size}, KEEPING CURRENT")
                            currentState.reports
                        } else {
                            reports
                        }
                        
                        val newData = WatchData(good, bad, status, pool, timer, goods, bads, finalPlans, finalReports)
                        
                        // Логируем для отладки
                        android.util.Log.d("MainActivity", "🎯 [CALLBACK] Setting new data state: plans=${newData.plans.size}, reports=${newData.reports.size}")
                        android.util.Log.d("MainActivity", "   Plans: ${newData.plans.map { it.text }.take(3)}")
                        android.util.Log.d("MainActivity", "   Plans full: ${newData.plans.map { "id=${it.id}, text='${it.text}', date=${it.date}" }}")
                        android.util.Log.d("MainActivity", "   Reports: ${newData.reports.map { "${it.id}:${it.date}" }.take(3)}")
                        android.util.Log.d("MainActivity", "   Reports full: ${newData.reports.map { "id=${it.id}, date=${it.date}, good=${it.goodCount}, bad=${it.badCount}" }}")
                        
                        // Сохраняем в SharedPreferences только если данные действительно новые и не пустые
                        if (finalPlans.isNotEmpty() || finalReports.isNotEmpty()) {
                            // Сохраняем только если данные изменились
                            val currentPlansJson = context.getSharedPreferences("wear_data", android.content.Context.MODE_PRIVATE)
                                .getString("plansJson", "[]") ?: "[]"
                            val currentReportsJson = context.getSharedPreferences("wear_data", android.content.Context.MODE_PRIVATE)
                                .getString("reportsJson", "[]") ?: "[]"
                            
                            if (plansJson != currentPlansJson || reportsJson != currentReportsJson) {
                                context.getSharedPreferences("wear_data", android.content.Context.MODE_PRIVATE)
                                    .edit()
                                    .putString("plansJson", plansJson)
                                    .putString("reportsJson", reportsJson)
                                    .apply()
                                android.util.Log.d("MainActivity", "💾 [CALLBACK] Saved NEW plans and reports to SharedPreferences")
                            } else {
                                android.util.Log.d("MainActivity", "ℹ️ [CALLBACK] Plans and reports unchanged, NOT overwriting SharedPreferences")
                            }
                        } else {
                            android.util.Log.d("MainActivity", "⚠️ [CALLBACK] Plans and reports empty, NOT overwriting SharedPreferences")
                        }
                        
                        // ВСЕГДА обновляем состояние СРАЗУ после парсинга
                        android.util.Log.d("MainActivity", "🔄 [CALLBACK] Updating UI state: plans=${newData.plans.size}, reports=${newData.reports.size}")
                        dataState = newData
                        android.util.Log.d("MainActivity", "✅ [CALLBACK] dataState updated!")

                        // Обновляем виджет
                        try {
                            android.appwidget.AppWidgetManager.getInstance(context).let { manager ->
                                val widgetIds = manager.getAppWidgetIds(
                                    android.content.ComponentName(context, com.katapandroid.lazybones.wear.widget.WearWidgetProvider::class.java)
                                )
                                if (widgetIds.isNotEmpty()) {
                                    com.katapandroid.lazybones.wear.widget.WearWidgetProvider()
                                        .onUpdate(context, manager, widgetIds)
                                    android.util.Log.d("MainActivity", "📱 Widget updated")
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("MainActivity", "Error updating widget", e)
                        }
                    }

                val originalCallback = dataReceiver.onDataReceived
                val originalAllCallback = dataReceiver.onAllDataReceived

                dataReceiver.onDataReceived = { good, bad ->
                    originalCallback?.invoke(good, bad)
                    android.util.Log.d("MainActivity", "📥 onDataReceived called: good=$good, bad=$bad")
                }
                dataReceiver.onAllDataReceived = { good, bad, status, pool, timer, goods, bads ->
                    originalAllCallback?.invoke(good, bad, status, pool, timer, goods, bads)
                    android.util.Log.d("MainActivity", "📥 onAllDataReceived called")
                    callback(good, bad, status, pool, timer, goods, bads)
                }

                android.util.Log.d("MainActivity", "✅ DataReceiver callbacks set up")

                onDispose {
                    dataReceiver.onDataReceived = originalCallback
                    dataReceiver.onAllDataReceived = originalAllCallback
                }
            }
            
            MaterialTheme {
                @OptIn(ExperimentalFoundationApi::class)
                Scaffold(
                    timeText = {
                        TimeText(
                            timeTextStyle = TimeTextDefaults.timeTextStyle(
                                color = MaterialTheme.colors.primary
                            )
                        )
                    }
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        when (page) {
                            0 -> MainScreen(
                                context = context,
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
                        }
                    }
                    
                    // Индикатор страниц внизу
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        repeat(3) { index ->
                            Box(
                                modifier = Modifier
                                    .padding(2.dp)
                                    .size(4.dp)
                                    .background(
                                        color = if (pagerState.currentPage == index) 
                                            MaterialTheme.colors.primary 
                                        else 
                                            MaterialTheme.colors.onSurface.copy(alpha = 0.3f),
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            val dataClient = Wearable.getDataClient(this)
            dataClient.removeListener(dataReceiver)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error removing listener", e)
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
        // Функция для перевода статуса на русский
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
        
        // Функция для перевода статуса пула на русский
        fun translatePoolStatus(status: String?): String {
            return when (status) {
                "ACTIVE" -> "Активен"
                "BEFORE_START" -> "До начала"
                "AFTER_END" -> "Завершён"
                null -> "Нет данных"
                else -> status
            }
        }
        
        // Красивое отображение Good и Bad
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Good
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
            
            // Bad
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
        
        // Статус отчета
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
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = translateStatus(reportStatus),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
        
        // Статус пула
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
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = translatePoolStatus(poolStatus),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
        
        // Таймер
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
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
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
