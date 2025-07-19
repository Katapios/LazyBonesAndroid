package com.katapandroid.lazybones.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.FormBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class TelegramService {
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    suspend fun sendTestMessage(token: String, chatId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val message = """
                🤖 Тестовое сообщение от LazyBones бота
                
                📅 Дата: ${SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())}
                
                ✅ Связь с ботом установлена успешно!
                
                Теперь вы можете публиковать отчеты в эту группу.
            """.trimIndent()
            
            sendMessage(token, chatId, message)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun sendCustomReport(
        token: String, 
        chatId: String, 
        date: Date,
        checklist: List<String>,
        goodItems: List<String>,
        badItems: List<String>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            
            val message = buildString {
                appendLine("📅 Отчёт за ${dateFormat.format(date)}")
                appendLine("📱 Устройство: LazyBones")
                appendLine("⏰ Время: ${timeFormat.format(date)}")
                appendLine()
                
                if (checklist.isNotEmpty()) {
                    appendLine("📋 План на день:")
                    checklist.forEachIndexed { idx, item ->
                        val isCompleted = goodItems.contains(item)
                        val icon = if (isCompleted) "✅" else "❌"
                        appendLine("${idx + 1}. $icon $item")
                    }
                    appendLine()
                } 
                // Если checklist пустой — выводим good/bad секции
                if (checklist.isEmpty()) {
                    if (goodItems.isNotEmpty()) {
                        appendLine("✅ Я молодец:")
                        goodItems.forEachIndexed { idx, item ->
                            appendLine("${idx + 1}. ✅ $item")
                        }
                        appendLine()
                    }
                    if (badItems.isNotEmpty()) {
                        appendLine("❌ Я не молодец:")
                        badItems.forEachIndexed { idx, item ->
                            appendLine("${idx + 1}. ❌ $item")
                        }
                        appendLine()
                    }
                }
                appendLine("📤 Опубликовано через LazyBones")
            }
            
            sendMessage(token, chatId, message)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun sendMessage(token: String, chatId: String, text: String): Result<String> {
        return try {
            val url = "https://api.telegram.org/bot$token/sendMessage"
            
            val formBody = FormBody.Builder()
                .add("chat_id", chatId)
                .add("text", text)
                .add("parse_mode", "HTML")
                .build()
            
            val request = Request.Builder()
                .url(url)
                .post(formBody)
                .build()
            
            val response = httpClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val jsonResponse = JSONObject(responseBody ?: "{}")
                
                if (jsonResponse.getBoolean("ok")) {
                    Result.success("Message sent successfully")
                } else {
                    val errorCode = jsonResponse.optInt("error_code", -1)
                    val description = jsonResponse.optString("description", "Unknown error")
                    Result.failure(Exception("Telegram API error: $errorCode - $description"))
                }
            } else {
                Result.failure(Exception("HTTP error: ${response.code} - ${response.message}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 