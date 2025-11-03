package com.katapandroid.lazybones.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.wear.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.material.*
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.*
import com.google.android.gms.wearable.CapabilityClient
import com.katapandroid.lazybones.wear.sync.WearDataReceiver
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class WatchData(
    val goodCount: Int,
    val badCount: Int,
    val reportStatus: String?,
    val poolStatus: String?,
    val timerText: String?,
    val goodItems: List<String>,
    val badItems: List<String>
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
                mutableStateOf(WatchData(0, 0, null, null, null, emptyList(), emptyList()))
            }
            
            var connectionInfo by remember { mutableStateOf("") }
            
            // Загружаем начальные данные из SharedPreferences и периодически обновляем
            LaunchedEffect(Unit) {
            fun loadDataFromPrefs() {
                val prefs = context.getSharedPreferences("wear_data", android.content.Context.MODE_PRIVATE)
                val newData = WatchData(
                    goodCount = prefs.getInt("goodCount", 0),
                    badCount = prefs.getInt("badCount", 0),
                    reportStatus = prefs.getString("reportStatus", null),
                    poolStatus = prefs.getString("poolStatus", null),
                    timerText = prefs.getString("timerText", null),
                    goodItems = prefs.getStringSet("goodItems", emptySet())?.toList() ?: emptyList(),
                    badItems = prefs.getStringSet("badItems", emptySet())?.toList() ?: emptyList()
                )
                
                // ВСЕГДА логируем что читаем
                android.util.Log.d("MainActivity", "📖 Reading from prefs: good=${newData.goodCount}, bad=${newData.badCount}, timer=${newData.timerText ?: "null"}, status=${newData.reportStatus ?: "null"}")
                
                // ВСЕГДА обновляем состояние (чтобы UI обновлялся)
                val current = dataState
                // Обновляем если данные изменились ИЛИ если это первое чтение (goodCount=0 и badCount=0)
                val isFirstRead = current.goodCount == 0 && current.badCount == 0 && current.reportStatus == null && current.timerText == null
                if (isFirstRead || 
                    newData.goodCount != current.goodCount || 
                    newData.badCount != current.badCount ||
                    newData.reportStatus != current.reportStatus ||
                    newData.poolStatus != current.poolStatus ||
                    newData.timerText != current.timerText ||
                    newData.goodItems != current.goodItems ||
                    newData.badItems != current.badItems) {
                    android.util.Log.d("MainActivity", "🔄 Updating UI: good=${newData.goodCount}, bad=${newData.badCount}, isFirstRead=$isFirstRead")
                    dataState = newData
                } else {
                    android.util.Log.d("MainActivity", "ℹ️ Data unchanged, no UI update needed")
                }
            }
                
                // Загружаем сразу
                loadDataFromPrefs()
                
                // Периодически проверяем обновления (каждые 2 секунды)
                while (true) {
                    delay(2000)
                    loadDataFromPrefs()
                    
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
                                            val newData = WatchData(goodCount, badCount, reportStatus, poolStatus, timerText, goodItems, badItems)
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
                                                            badItems
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

                        val newData = WatchData(good, bad, status, pool, timer, goods, bads)
                        
                        // ВСЕГДА сохраняем в SharedPreferences
                        context.getSharedPreferences("wear_data", android.content.Context.MODE_PRIVATE)
                            .edit()
                            .putInt("goodCount", good)
                            .putInt("badCount", bad)
                            .putString("reportStatus", status)
                            .putString("poolStatus", pool)
                            .putString("timerText", timer)
                            .putStringSet("goodItems", goods.toSet())
                            .putStringSet("badItems", bads.toSet())
                            .apply()
                        android.util.Log.d("MainActivity", "💾 Data saved to SharedPreferences")

                        // ВСЕГДА обновляем состояние
                        android.util.Log.d("MainActivity", "🔄 Updating UI state")
                        dataState = newData

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
                Scaffold(
                    timeText = {
                        TimeText(
                            timeTextStyle = TimeTextDefaults.timeTextStyle(
                                color = MaterialTheme.colors.primary
                            )
                        )
                    }
                ) {
                    MainScreen(
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
        // Заголовок
        Text(
            text = "LazyBones",
            style = MaterialTheme.typography.title1,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )
        
        // Диагностическая информация
        if (connectionInfo.isNotEmpty()) {
            Text(
                text = connectionInfo,
                style = MaterialTheme.typography.body2,
                fontSize = 10.sp,
                color = MaterialTheme.colors.secondary
            )
        }
        
        HorizontalDivider()
        
        // Счетчики
        Text(
            text = "Good: $goodCount",
            style = MaterialTheme.typography.title2,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = "Bad: $badCount",
            style = MaterialTheme.typography.title2,
            textAlign = TextAlign.Center
        )
        
        HorizontalDivider()
        
        // Статус отчета
        Text(
            text = "Статус: ${reportStatus ?: "нет данных"}",
            style = MaterialTheme.typography.body1,
            textAlign = TextAlign.Center
        )
        
        // Статус пула
        Text(
            text = "Пул: ${poolStatus ?: "нет данных"}",
            style = MaterialTheme.typography.body1,
            textAlign = TextAlign.Center
        )
        
        // Таймер
        Text(
            text = timerText ?: "Таймер: нет данных",
            style = MaterialTheme.typography.body1,
            textAlign = TextAlign.Center
        )
        
        HorizontalDivider()
        
        // Good items
        if (goodItems.isNotEmpty()) {
            Text(
                text = "Good items:",
                style = MaterialTheme.typography.body2,
                fontWeight = FontWeight.Bold
            )
            goodItems.forEach { item ->
                Text(
                    text = "• $item",
                    style = MaterialTheme.typography.body2,
                    fontSize = 10.sp
                )
            }
        }
        
        // Bad items
        if (badItems.isNotEmpty()) {
            Text(
                text = "Bad items:",
                style = MaterialTheme.typography.body2,
                fontWeight = FontWeight.Bold
            )
            badItems.forEach { item ->
                Text(
                    text = "• $item",
                    style = MaterialTheme.typography.body2,
                    fontSize = 10.sp
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
