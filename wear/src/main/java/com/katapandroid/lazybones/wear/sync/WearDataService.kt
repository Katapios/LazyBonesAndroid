package com.katapandroid.lazybones.wear.sync

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.google.android.gms.wearable.Wearable

/**
 * Сервис для постоянного приема данных от телефона
 * Работает в фоне, даже когда приложение закрыто
 */
class WearDataService : Service() {
    private lateinit var dataReceiver: WearDataReceiver
    
    companion object {
        private const val TAG = "WearDataService"
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "✅ Service created")
        
        // На Wear OS можно использовать обычный сервис без foreground
        // startForeground не требуется для Wear OS
        
        dataReceiver = WearDataReceiver()
        
        // Настраиваем callback для сохранения данных
        dataReceiver.onAllDataReceived = { good, bad, status, pool, timer, goods, bads ->
            Log.d(TAG, "📥 Data received in service: good=$good, bad=$bad")
            saveDataToSharedPreferences(good, bad, status, pool, timer, goods, bads)
        }
        
        // Регистрируем слушатели
        val dataClient = Wearable.getDataClient(this)
        dataClient.addListener(dataReceiver)
        
        val messageClient = Wearable.getMessageClient(this)
        messageClient.addListener(dataReceiver)
        
        Log.d(TAG, "✅ Listeners registered")
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
        
        Log.d(TAG, "💾 Data saved to SharedPreferences")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "📥 Service started")
        return START_STICKY // Перезапускать сервис если он был убит
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "❌ Service destroyed")
        
        try {
            val dataClient = Wearable.getDataClient(this)
            dataClient.removeListener(dataReceiver)
            
            val messageClient = Wearable.getMessageClient(this)
            messageClient.removeListener(dataReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing listeners", e)
        }
    }
}

