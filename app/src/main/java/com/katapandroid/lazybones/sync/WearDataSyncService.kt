package com.katapandroid.lazybones.sync

import android.content.Context
import android.util.Log
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import androidx.annotation.VisibleForTesting
import org.json.JSONObject
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo

/**
 * Сервис для синхронизации данных с часами через Wearable Data Layer
 */
class WearDataSyncService(private val context: Context) {
    private val dataClient: DataClient = Wearable.getDataClient(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncLock = Any()
    @Volatile
    private var currentSyncJob: Job? = null
    @Volatile
    private var lastPayloadHash: Int? = null
    @Volatile
    private var lastPayloadTimestamp: Long = 0L
    
    companion object {
        private const val TAG = "WearDataSync"
        private const val DATA_PATH = "/lazybones/data"
        private const val CAPABILITY_LAZYBONES = "lazybones_data_sync"
        private const val SYNC_TIMEOUT_MS = 15_000L
        private const val RESEND_DEBOUNCE_MS = 5_000L
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
        badItems: List<String>,
        plans: List<com.katapandroid.lazybones.data.PlanItem> = emptyList(),
        reports: List<com.katapandroid.lazybones.data.Post> = emptyList(),
        planPosts: List<com.katapandroid.lazybones.data.Post> = emptyList()
    ) {
        val payloadJson = buildPayloadJson(
            goodCount,
            badCount,
            reportStatus,
            poolStatus,
            timerText,
            goodItems,
            badItems,
            plans,
            reports,
            planPosts
        )
        val payloadString = payloadJson.toString()
        val payloadHash = payloadString.hashCode()
        val now = System.currentTimeMillis()
        
        synchronized(syncLock) {
            if (lastPayloadHash == payloadHash && now - lastPayloadTimestamp < RESEND_DEBOUNCE_MS) {
                Log.d(TAG, "ℹ️ Skipping sync: payload unchanged within debounce window")
                return
            }
            lastPayloadHash = payloadHash
            lastPayloadTimestamp = now
            currentSyncJob?.cancel()
            currentSyncJob = scope.launch {
                runCatching {
                    withTimeout(SYNC_TIMEOUT_MS) {
                        sendPayloadToWear(payloadJson, payloadString)
                    }
                }.onFailure { throwable ->
                    if (throwable is CancellationException) {
                        Log.d(TAG, "🔁 Sync job cancelled (new sync scheduled)")
                    } else {
                        Log.e(TAG, "❌ Error syncing data", throwable)
                    }
                }
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

    @VisibleForTesting
    internal fun buildPayloadJson(
        goodCount: Int,
        badCount: Int,
        reportStatus: String?,
        poolStatus: String?,
        timerText: String?,
        goodItems: List<String>,
        badItems: List<String>,
        plans: List<com.katapandroid.lazybones.data.PlanItem>,
        reports: List<com.katapandroid.lazybones.data.Post>,
        planPosts: List<com.katapandroid.lazybones.data.Post>
    ): JSONObject {
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
        
        val plansArray = org.json.JSONArray()
        if (plans.isNotEmpty()) {
            val postMap = planPosts.associateBy { it.id }
            val plansByPostId = plans.groupBy { it.id / 1000 }
            plansByPostId.forEach { (postId, planItems) ->
                val planDate = postMap[postId]?.date?.time ?: System.currentTimeMillis()
                planItems.forEach { plan ->
                    val planObj = JSONObject().apply {
                        put("id", plan.id)
                        put("text", plan.text)
                        put("date", planDate)
                    }
                    plansArray.put(planObj)
                }
            }
        }
        data.put("plans", plansArray)
        Log.d(TAG, "📋 Plans array size=${plansArray.length()} (input plans=${plans.size})")
        
        val reportsArray = org.json.JSONArray()
        reports.filter { !it.isDraft }
            .sortedByDescending { it.date.time }
            .forEach { report ->
                val reportObj = JSONObject().apply {
                    put("id", report.id)
                    put("date", report.date.time)
                    put("goodCount", report.goodCount)
                    put("badCount", report.badCount)
                    put("published", report.published)
                    put("goodItems", org.json.JSONArray(report.goodItems))
                    put("badItems", org.json.JSONArray(report.badItems))
                    put("checklist", org.json.JSONArray(report.checklist))
                }
                reportsArray.put(reportObj)
            }
        data.put("reports", reportsArray)
        Log.d(TAG, "📑 Reports array size=${reportsArray.length()} (input reports=${reports.size})")
        return data
    }

    private suspend fun sendPayloadToWear(payloadJson: JSONObject, payloadString: String) {
        try {
            try {
                val capabilityClient = Wearable.getCapabilityClient(context)
                Tasks.await(capabilityClient.addLocalCapability(CAPABILITY_LAZYBONES))
                Log.d(TAG, "✅ Phone capability registered: $CAPABILITY_LAZYBONES")
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Failed to register capability", e)
            }
            
            val isConnected = isWearConnected()
            Log.d(TAG, "📱 Checking connection: connected=$isConnected")
            
            try {
                val capabilityClient = Wearable.getCapabilityClient(context)
                val capabilityInfo = Tasks.await(
                    capabilityClient.getCapability(CAPABILITY_LAZYBONES, CapabilityClient.FILTER_REACHABLE)
                )
                Log.d(TAG, "🔗 Capability check: watch nodes=${capabilityInfo.nodes.size}")
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Capability check failed", e)
            }
            
            if (!isConnected) {
                Log.w(TAG, "⚠️ No wearable devices connected, but sending data anyway")
            }
            
            Log.d(
                TAG,
                "📤 Starting sync: good=${payloadJson.optInt("goodCount")}, bad=${payloadJson.optInt("badCount")}, plans=${payloadJson.optJSONArray("plans")?.length()}, reports=${payloadJson.optJSONArray("reports")?.length()}"
            )
            
            val nodes = Tasks.await(Wearable.getNodeClient(context).connectedNodes)
            Log.d(TAG, "📱 Found ${nodes.size} connected nodes for Data Layer")
            
            var dataLayerSuccess = false
            if (nodes.isNotEmpty()) {
                for (node in nodes) {
                    try {
                        val putDataRequest = PutDataMapRequest.create(DATA_PATH).apply {
                            dataMap.putString("data", payloadString)
                            dataMap.putLong("timestamp", System.currentTimeMillis())
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
            
            try {
                val broadcastRequest = PutDataMapRequest.create(DATA_PATH).apply {
                    dataMap.putString("data", payloadString)
                    dataMap.putLong("timestamp", System.currentTimeMillis())
                }.asPutDataRequest().apply {
                    setUrgent()
                }
                
                Log.d(TAG, "📦 Sending data item to path: $DATA_PATH (broadcast)")
                val resultDataItem = Tasks.await(dataClient.putDataItem(broadcastRequest))
                if (resultDataItem != null) {
                    Log.d(TAG, "✅ putDataItem successful (broadcast), URI: ${resultDataItem.uri}")
                    dataLayerSuccess = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error in broadcast putDataItem", e)
            }
            
            if (!dataLayerSuccess) {
                Log.w(TAG, "⚠️ Data Layer failed, will try Message API only")
            }
            
            try {
                val messageClient = Wearable.getMessageClient(context)
                val messageNodes = Tasks.await(Wearable.getNodeClient(context).connectedNodes)
                Log.d(TAG, "📱 Connected nodes for message: ${messageNodes.size}")
                if (messageNodes.isNotEmpty()) {
                    for (node in messageNodes) {
                        try {
                            Log.d(TAG, "📨 Sending message to: ${node.displayName} (id=${node.id}, nearby=${node.isNearby})")
                            val messageTask = messageClient.sendMessage(
                                node.id,
                                "/lazybones/message",
                                payloadString.toByteArray()
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
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Unexpected error during sync", e)
            throw e
        }
    }
}

