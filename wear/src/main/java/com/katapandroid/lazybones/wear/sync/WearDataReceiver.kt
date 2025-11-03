package com.katapandroid.lazybones.wear.sync

import android.util.Log
import com.google.android.gms.wearable.*
import kotlinx.coroutines.*
import org.json.JSONObject

/**
 * Принимает данные от телефона через Wearable Data Layer
 */
class WearDataReceiver : DataClient.OnDataChangedListener, MessageClient.OnMessageReceivedListener {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    var onDataReceived: ((goodCount: Int, badCount: Int) -> Unit)? = null
    var onAllDataReceived: ((goodCount: Int, badCount: Int, reportStatus: String?, poolStatus: String?, timerText: String?, goodItems: List<String>, badItems: List<String>) -> Unit)? = null
    
    companion object {
        private const val TAG = "WearDataReceiver"
        const val DATA_PATH = "/lazybones/data"
    }
    
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        Log.d(TAG, "📥 ====== onDataChanged CALLED ======")
        Log.d(TAG, "   Events count: ${dataEvents.count}")
        Log.d(TAG, "   Listening for path: $DATA_PATH")
        Log.d(TAG, "   Thread: ${Thread.currentThread().name}")
        Log.d(TAG, "   DataEvents buffer: $dataEvents")
        
        if (dataEvents.count == 0) {
            Log.w(TAG, "⚠️ No events received")
            dataEvents.close()
            return
        }
        
        Log.d(TAG, "✅ Processing ${dataEvents.count} events")
        
        try {
            for (event in dataEvents) {
                val eventPath = event.dataItem.uri.path
                Log.d(TAG, "  📦 Event type: ${event.type} (TYPE_CHANGED=${DataEvent.TYPE_CHANGED})")
                Log.d(TAG, "     Path: $eventPath")
                Log.d(TAG, "     Full URI: ${event.dataItem.uri}")
                Log.d(TAG, "     Matches target path? ${eventPath == DATA_PATH}")
                
                if (event.type == DataEvent.TYPE_CHANGED && eventPath == DATA_PATH) {
                Log.d(TAG, "✅ Matched path! Processing data...")
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val dataString = dataMap.getString("data")
                
                try {
                    Log.d(TAG, "📥 Received data event: ${event.dataItem.uri}")
                    
                    val json = JSONObject(dataString)
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
                    
                    Log.d(TAG, "✅ Parsed data: good=$goodCount, bad=$badCount, status=$reportStatus, pool=$poolStatus, timer=$timerText")
                    Log.d(TAG, "Good items: ${goodItems.size}, Bad items: ${badItems.size}")
                    
                    // Вызываем callback с полными данными
                    onAllDataReceived?.invoke(goodCount, badCount, reportStatus, poolStatus, timerText, goodItems, badItems)
                    // Обратная совместимость со старым callback
                    onDataReceived?.invoke(goodCount, badCount)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error parsing data", e)
                    e.printStackTrace()
                }
            } else {
                Log.d(TAG, "  ⏭️ Skipping event - path doesn't match or wrong type")
            }
        }
        } finally {
            dataEvents.close()
        }
    }
    
    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d(TAG, "📨 ====== onMessageReceived CALLED ======")
        Log.d(TAG, "   Path: ${messageEvent.path}")
        Log.d(TAG, "   Source node: ${messageEvent.sourceNodeId}")
        Log.d(TAG, "   Data size: ${messageEvent.data.size} bytes")
        
        if (messageEvent.path == "/lazybones/message") {
            try {
                val dataString = String(messageEvent.data)
                Log.d(TAG, "📥 Received message data: $dataString")
                
                val json = JSONObject(dataString)
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
                
                Log.d(TAG, "✅ Parsed message data: good=$goodCount, bad=$badCount, status=$reportStatus, pool=$poolStatus, timer=$timerText")
                
                // Вызываем callback с полными данными
                onAllDataReceived?.invoke(goodCount, badCount, reportStatus, poolStatus, timerText, goodItems, badItems)
                onDataReceived?.invoke(goodCount, badCount)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error parsing message data", e)
                e.printStackTrace()
            }
        }
    }
}

