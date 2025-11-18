package com.katapandroid.lazybones.wear.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.katapandroid.lazybones.wear.model.PlanItem
import com.katapandroid.lazybones.wear.model.ReportItem
import com.katapandroid.lazybones.wear.model.WatchData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class WearDataRepository private constructor(private val appContext: Context) {
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val mutex = Mutex()
    private val _data = MutableStateFlow(loadFromPrefs())
    val data: StateFlow<WatchData> = _data.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            try {
                Tasks.await(
                    Wearable.getCapabilityClient(appContext)
                        .addLocalCapability(CAPABILITY_LAZYBONES)
                )
                Log.d(TAG, "✅ Wear capability registered: $CAPABILITY_LAZYBONES")
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Failed to register wear capability", e)
            }
        }
    }

    fun currentData(): WatchData = _data.value

    suspend fun updateFromJson(json: JSONObject) {
        val watchData = json.toWatchData()
        val plansArray = json.optJSONArray("plans") ?: JSONArray()
        val reportsArray = json.optJSONArray("reports") ?: JSONArray()
        saveData(watchData, plansArray, reportsArray)
    }

    suspend fun saveData(
        watchData: WatchData,
        plansArray: JSONArray = plansToJsonArray(watchData.plans),
        reportsArray: JSONArray = reportsToJsonArray(watchData.reports)
    ) {
        mutex.withLock {
            prefs.edit()
                .putInt(KEY_GOOD_COUNT, watchData.goodCount)
                .putInt(KEY_BAD_COUNT, watchData.badCount)
                .putString(KEY_REPORT_STATUS, watchData.reportStatus)
                .putString(KEY_POOL_STATUS, watchData.poolStatus)
                .putString(KEY_TIMER_TEXT, watchData.timerText)
                .putString(KEY_MOTIVATIONAL_SLOGAN, watchData.motivationalSlogan)
                .putStringSet(KEY_GOOD_ITEMS, watchData.goodItems.toSet())
                .putStringSet(KEY_BAD_ITEMS, watchData.badItems.toSet())
                .putString(KEY_PLANS_JSON, plansArray.toString())
                .putString(KEY_REPORTS_JSON, reportsArray.toString())
                .apply()
            _data.value = watchData.copy(
                plans = parsePlans(plansArray),
                reports = parseReports(reportsArray)
            )
        }
    }

    suspend fun refreshFromPrefs() {
        mutex.withLock {
            _data.value = loadFromPrefs()
        }
    }

    suspend fun ensureDataFromDataLayer(): Boolean = withContext(Dispatchers.IO) {
        if (_data.value.hasMeaningfulContent()) {
            return@withContext false
        }
        val dataClient = Wearable.getDataClient(appContext)
        val dataItems = Tasks.await(dataClient.getDataItems())
        try {
            val iterator = dataItems.iterator()
            while (iterator.hasNext()) {
                val item = iterator.next()
                if (item.uri.path == DATA_PATH) {
                    val dataMap = DataMapItem.fromDataItem(item).dataMap
                    val dataString = dataMap.getString("data") ?: continue
                    val json = JSONObject(dataString)
                    updateFromJson(json)
                    return@withContext true
                }
            }
        } finally {
            dataItems.release()
        }
        false
    }

    private fun loadFromPrefs(): WatchData {
        val plansJson = prefs.getString(KEY_PLANS_JSON, "[]") ?: "[]"
        val reportsJson = prefs.getString(KEY_REPORTS_JSON, "[]") ?: "[]"
        return WatchData(
            goodCount = prefs.getInt(KEY_GOOD_COUNT, 0),
            badCount = prefs.getInt(KEY_BAD_COUNT, 0),
            reportStatus = prefs.getString(KEY_REPORT_STATUS, null),
            poolStatus = prefs.getString(KEY_POOL_STATUS, null),
            timerText = prefs.getString(KEY_TIMER_TEXT, null),
            motivationalSlogan = prefs.getString(KEY_MOTIVATIONAL_SLOGAN, null),
            goodItems = prefs.getStringSet(KEY_GOOD_ITEMS, emptySet())?.toList() ?: emptyList(),
            badItems = prefs.getStringSet(KEY_BAD_ITEMS, emptySet())?.toList() ?: emptyList(),
            plans = runCatching { parsePlans(JSONArray(plansJson)) }.getOrElse { emptyList() },
            reports = runCatching { parseReports(JSONArray(reportsJson)) }.getOrElse { emptyList() }
        )
    }

    companion object {
        private const val TAG = "WearDataRepo"
        private const val PREFS_NAME = "wear_data"
        private const val CAPABILITY_LAZYBONES = "lazybones_data_sync"
        private const val DATA_PATH = "/lazybones/data"

        private const val KEY_GOOD_COUNT = "goodCount"
        private const val KEY_BAD_COUNT = "badCount"
        private const val KEY_REPORT_STATUS = "reportStatus"
        private const val KEY_POOL_STATUS = "poolStatus"
        private const val KEY_TIMER_TEXT = "timerText"
        private const val KEY_MOTIVATIONAL_SLOGAN = "motivationalSlogan"
        private const val KEY_GOOD_ITEMS = "goodItems"
        private const val KEY_BAD_ITEMS = "badItems"
        private const val KEY_PLANS_JSON = "plansJson"
        private const val KEY_REPORTS_JSON = "reportsJson"

        @Volatile
        private var instance: WearDataRepository? = null

        fun getInstance(context: Context): WearDataRepository {
            return instance ?: synchronized(this) {
                instance ?: WearDataRepository(context.applicationContext).also { instance = it }
            }
        }

        private fun JSONArray.toStringList(): List<String> =
            (0 until length()).mapNotNull { index -> optString(index, null) }

        private fun JSONArray.toPlanItems(): List<PlanItem> =
            (0 until length()).mapNotNull { index ->
                val obj = optJSONObject(index) ?: return@mapNotNull null
                PlanItem(
                    id = obj.optLong("id"),
                    text = obj.optString("text"),
                    date = obj.optLong("date", 0L)
                )
            }

        private fun JSONArray.toReportItems(): List<ReportItem> =
            (0 until length()).mapNotNull { index ->
                val obj = optJSONObject(index) ?: return@mapNotNull null
                ReportItem(
                    id = obj.optLong("id"),
                    date = obj.optLong("date"),
                    goodCount = obj.optInt("goodCount"),
                    badCount = obj.optInt("badCount"),
                    published = obj.optBoolean("published"),
                    goodItems = (obj.optJSONArray("goodItems") ?: JSONArray()).toStringList(),
                    badItems = (obj.optJSONArray("badItems") ?: JSONArray()).toStringList(),
                    checklist = (obj.optJSONArray("checklist") ?: JSONArray()).toStringList()
                )
            }

        private fun plansToJsonArray(plans: List<PlanItem>): JSONArray {
            val array = JSONArray()
            plans.forEach { plan ->
                array.put(
                    JSONObject().apply {
                        put("id", plan.id)
                        put("text", plan.text)
                        put("date", plan.date)
                    }
                )
            }
            return array
        }

        private fun reportsToJsonArray(reports: List<ReportItem>): JSONArray {
            val array = JSONArray()
            reports.forEach { report ->
                array.put(
                    JSONObject().apply {
                        put("id", report.id)
                        put("date", report.date)
                        put("goodCount", report.goodCount)
                        put("badCount", report.badCount)
                        put("published", report.published)
                        put("goodItems", JSONArray(report.goodItems))
                        put("badItems", JSONArray(report.badItems))
                        put("checklist", JSONArray(report.checklist))
                    }
                )
            }
            return array
        }

        private fun parsePlans(array: JSONArray): List<PlanItem> = array.toPlanItems()
        private fun parseReports(array: JSONArray): List<ReportItem> = array.toReportItems()

        private fun JSONObject.toWatchData(): WatchData {
            val plansArray = optJSONArray("plans") ?: JSONArray()
            val reportsArray = optJSONArray("reports") ?: JSONArray()
            return WatchData(
                goodCount = optInt("goodCount", 0),
                badCount = optInt("badCount", 0),
                reportStatus = optStringOrNull("reportStatus"),
                poolStatus = optStringOrNull("poolStatus"),
                timerText = optStringOrNull("timerText"),
                motivationalSlogan = optStringOrNull("motivationalSlogan"),
                goodItems = (optJSONArray("goodItems") ?: JSONArray()).toStringList(),
                badItems = (optJSONArray("badItems") ?: JSONArray()).toStringList(),
                plans = parsePlans(plansArray),
                reports = parseReports(reportsArray)
            )
        }

        private fun JSONObject.optStringOrNull(key: String): String? {
            return if (has(key) && !isNull(key)) optString(key) else null
        }
    }
}

