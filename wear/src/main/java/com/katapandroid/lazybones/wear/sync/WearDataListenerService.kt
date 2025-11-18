package com.katapandroid.lazybones.wear.sync

import android.util.Log
import com.google.android.gms.wearable.*
import com.katapandroid.lazybones.wear.data.WearDataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * WearableListenerService для приема данных от телефона
 * Это правильный способ для Wear OS - система автоматически доставляет сообщения
 */
class WearDataListenerService : WearableListenerService() {
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + Dispatchers.IO)
    private val repository by lazy { WearDataRepository.getInstance(applicationContext) }
    
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
            Log.d(
                TAG,
                "✅ Parsed data payload: good=${json.optInt("goodCount")}, bad=${json.optInt("badCount")}, plans=${json.optJSONArray("plans")?.length() ?: 0}, reports=${json.optJSONArray("reports")?.length() ?: 0}"
            )
            serviceScope.launch {
                runCatching { repository.updateFromJson(json) }
                    .onFailure { Log.e(TAG, "❌ Error saving data to repository", it) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error parsing data", e)
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}

