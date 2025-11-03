package com.katapandroid.lazybones.sync

import android.content.Context
import android.util.Log
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.*
import kotlinx.coroutines.*
import org.json.JSONObject
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo

/**
 * Сервис для синхронизации данных с часами через Wearable Data Layer
 */
class WearDataSyncService(private val context: Context) {
    private val dataClient: DataClient = Wearable.getDataClient(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    companion object {
        private const val TAG = "WearDataSync"
        private const val DATA_PATH = "/lazybones/data"
        private const val CAPABILITY_LAZYBONES = "lazybones_data_sync"
    }
    
    /**
     * Отправляет данные счетчиков на часы
     */
    fun syncCounters(goodCount: Int, badCount: Int) {
        syncAllData(goodCount, badCount, null, null, null, emptyList(), emptyList())
    }
    
    /**
     * Отправляет все данные на часы
     */
    fun syncAllData(
        goodCount: Int,
        badCount: Int,
        reportStatus: String?,
        poolStatus: String?,
        timerText: String?,
        goodItems: List<String>,
        badItems: List<String>
    ) {
        scope.launch {
            try {
                // Регистрируем capability на телефоне
                try {
                    val capabilityClient = Wearable.getCapabilityClient(context)
                    Tasks.await(capabilityClient.addLocalCapability(CAPABILITY_LAZYBONES))
                    Log.d(TAG, "✅ Phone capability registered: $CAPABILITY_LAZYBONES")
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Failed to register capability", e)
                }
                
                // Проверяем подключение к часам
                val isConnected = isWearConnected()
                Log.d(TAG, "📱 Checking connection: connected=$isConnected")
                
                // Проверяем capability для связи с часами
                try {
                    val capabilityClient = Wearable.getCapabilityClient(context)
                    val capabilityInfo = Tasks.await(capabilityClient.getCapability(CAPABILITY_LAZYBONES, CapabilityClient.FILTER_REACHABLE))
                    Log.d(TAG, "🔗 Capability check: watch nodes=${capabilityInfo.nodes.size}")
                    if (capabilityInfo.nodes.isNotEmpty()) {
                        for (node in capabilityInfo.nodes) {
                            Log.d(TAG, "   Watch node: ${node.displayName} (id=${node.id})")
                        }
                    } else {
                        Log.w(TAG, "⚠️ No watch nodes found with capability")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Capability check failed", e)
                }
                
                if (!isConnected) {
                    Log.w(TAG, "⚠️ No wearable devices connected, but sending data anyway")
                }
                
                Log.d(TAG, "📤 Starting sync: good=$goodCount, bad=$badCount, status=$reportStatus, pool=$poolStatus, timer=$timerText")
                
                val data = JSONObject().apply {
                    put("goodCount", goodCount)
                    put("badCount", badCount)
                    reportStatus?.let { put("reportStatus", it) }
                    poolStatus?.let { put("poolStatus", it) }
                    timerText?.let { put("timerText", it) }
                    put("goodItems", org.json.JSONArray(goodItems))
                    put("badItems", org.json.JSONArray(badItems))
                    put("timestamp", System.currentTimeMillis())
                }
                
                // Используем PutDataRequest напрямую через PutDataMapRequest
                // Пробуем отправить данные на все подключенные nodes явно
                val nodes = Tasks.await(Wearable.getNodeClient(context).connectedNodes)
                Log.d(TAG, "📱 Found ${nodes.size} connected nodes for Data Layer")
                
                var dataLayerSuccess = false
                if (nodes.isNotEmpty()) {
                    // Отправляем данные на каждый node явно
                    for (node in nodes) {
                        try {
                            val putDataRequest = PutDataMapRequest.create(DATA_PATH).apply {
                                dataMap.putString("data", data.toString())
                                dataMap.putLong("timestamp", System.currentTimeMillis())
                                // Пробуем добавить node ID в путь
                                dataMap.putString("nodeId", node.id)
                            }.asPutDataRequest().apply {
                                setUrgent()
                            }
                            
                            Log.d(TAG, "📦 Sending data item to node: ${node.displayName} (${node.id})")
                            val resultDataItem = Tasks.await(dataClient.putDataItem(putDataRequest))
                            if (resultDataItem != null) {
                                Log.d(TAG, "✅ putDataItem successful for ${node.displayName}, URI: ${resultDataItem.uri}")
                                dataLayerSuccess = true
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error putting data for ${node.displayName}", e)
                        }
                    }
                }
                
                // Также отправляем в общий путь (без node)
                try {
                    val putDataRequest = PutDataMapRequest.create(DATA_PATH).apply {
                        dataMap.putString("data", data.toString())
                        dataMap.putLong("timestamp", System.currentTimeMillis())
                    }.asPutDataRequest().apply {
                        setUrgent()
                    }
                    
                    Log.d(TAG, "📦 Sending data item to path: $DATA_PATH (broadcast)")
                    val resultDataItem = Tasks.await(dataClient.putDataItem(putDataRequest))
                    if (resultDataItem != null) {
                        Log.d(TAG, "✅ putDataItem successful (broadcast), URI: ${resultDataItem.uri}")
                        dataLayerSuccess = true
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error in broadcast putDataItem", e)
                }
                
                if (dataLayerSuccess) {
                    Log.d(TAG, "✅ Data synced successfully via Data Layer: good=$goodCount, bad=$badCount")
                } else {
                    Log.w(TAG, "⚠️ Data Layer failed, will try Message API only")
                }
                
                // Также отправляем через Message API напрямую на каждый node
                try {
                    val messageClient = Wearable.getMessageClient(context)
                    val nodes = Tasks.await(Wearable.getNodeClient(context).connectedNodes)
                    Log.d(TAG, "📱 Connected nodes for message: ${nodes.size}")
                    if (nodes.isNotEmpty()) {
                        for (node in nodes) {
                            try {
                                Log.d(TAG, "📨 Sending message to: ${node.displayName} (id=${node.id}, nearby=${node.isNearby})")
                                val messageTask = messageClient.sendMessage(
                                    node.id,
                                    "/lazybones/message",
                                    data.toString().toByteArray()
                                )
                                Tasks.await(messageTask)
                                Log.d(TAG, "✅ Message sent successfully to: ${node.displayName}")
                            } catch (e: Exception) {
                                Log.e(TAG, "❌ Error sending message to ${node.displayName}", e)
                            }
                        }
                    } else {
                        Log.w(TAG, "⚠️ No connected nodes available for message sending")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error in message sending section", e)
                    e.printStackTrace()
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error syncing data", e)
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Проверяет, подключены ли часы
     */
    suspend fun isWearConnected(): Boolean {
        return try {
            val nodes = Tasks.await(Wearable.getNodeClient(context).connectedNodes)
            nodes.isNotEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking wear connection", e)
            false
        }
    }
}

