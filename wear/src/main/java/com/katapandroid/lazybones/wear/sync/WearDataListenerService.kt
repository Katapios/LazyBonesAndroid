package com.katapandroid.lazybones.wear.sync

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.android.gms.wearable.*
import org.json.JSONObject

/**
 * WearableListenerService для приема данных от телефона
 * Это правильный способ для Wear OS - система автоматически доставляет сообщения
 */
class WearDataListenerService : WearableListenerService() {
    
    companion object {
        private const val TAG = "WearDataListener"
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "✅ ====== WearDataListenerService CREATED ======")
        Log.d(TAG, "   Package: ${packageName}")
        Log.d(TAG, "   Service is ready to receive data")
    }
    
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        Log.d(TAG, "📥 ====== onDataChanged CALLED ======")
        Log.d(TAG, "   Events count: ${dataEvents.count}")
        
        if (dataEvents.count == 0) {
            Log.w(TAG, "⚠️ No events received")
            dataEvents.close()
            return
        }
        
        try {
            for (event in dataEvents) {
                val eventPath = event.dataItem.uri.path
                Log.d(TAG, "  📦 Event type: ${event.type}, path: $eventPath")
                
                if (event.type == DataEvent.TYPE_CHANGED && eventPath == "/lazybones/data") {
                    Log.d(TAG, "✅ Matched path! Processing data...")
                    val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                    val dataString = dataMap.getString("data")
                    
                    if (dataString != null) {
                        parseAndSaveData(dataString)
                    }
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
            val dataString = String(messageEvent.data)
            Log.d(TAG, "📥 Received message data: $dataString")
            parseAndSaveData(dataString)
        }
    }
    
    private fun parseAndSaveData(dataString: String) {
        try {
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
            
            saveDataToSharedPreferences(goodCount, badCount, reportStatus, poolStatus, timerText, goodItems, badItems)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error parsing data", e)
            e.printStackTrace()
        }
    }
    
    private fun saveDataToSharedPreferences(
        goodCount: Int,
        badCount: Int,
        reportStatus: String?,
        poolStatus: String?,
        timerText: String?,
        goodItems: List<String>,
        badItems: List<String>
    ) {
        val prefs = getSharedPreferences("wear_data", Context.MODE_PRIVATE)
        prefs.edit()
            .putInt("goodCount", goodCount)
            .putInt("badCount", badCount)
            .putString("reportStatus", reportStatus)
            .putString("poolStatus", poolStatus)
            .putString("timerText", timerText)
            .putStringSet("goodItems", goodItems.toSet())
            .putStringSet("badItems", badItems.toSet())
            .apply()
        
        Log.d(TAG, "💾 Data saved to SharedPreferences: good=$goodCount, bad=$badCount, timer=$timerText")
    }
}

