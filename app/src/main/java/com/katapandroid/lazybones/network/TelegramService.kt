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
        badItems: List<String>,
        deviceName: String = "LazyBones"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            
            val message = buildString {
                appendLine("📅 Отчёт за ${dateFormat.format(date)}")
                appendLine("📱 Устройство: ${deviceName.ifEmpty { "LazyBones" }}")
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

    suspend fun fetchRecentMessages(
        token: String,
        offset: Long?
    ): Result<Pair<List<TelegramMessage>, Long?>> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = "https://api.telegram.org/bot$token/getUpdates"
            // Для получения новых сообщений используем offset (если есть)
            // Если offset нет или 0 - получаем последние обновления
            val url = buildString {
                append(baseUrl)
                append("?limit=100") // Увеличиваем лимит для получения больше сообщений
                if (offset != null && offset > 0) {
                    append("&offset=")
                    append(offset)
                }
                // allowed_updates=["message","channel_post"] (URL-encoded)
                append("&allowed_updates=%5B%22message%22%2C%22channel_post%22%5D")
            }

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                val errorMsg = try {
                    val errorJson = JSONObject(errorBody)
                    errorJson.optString("description", "HTTP error: ${response.code}")
                } catch (e: Exception) {
                    "HTTP error: ${response.code} - ${response.message}"
                }
                return@withContext Result.failure(Exception(errorMsg))
            }

            val bodyStr = response.body?.string() ?: "{}"
            val json = JSONObject(bodyStr)
            if (!json.optBoolean("ok", false)) {
                val description = json.optString("description", "Unknown error")
                val errorCode = json.optInt("error_code", 0)
                val errorMsg = if (errorCode > 0) {
                    "Telegram API error ($errorCode): $description"
                } else {
                    "Telegram API error: $description"
                }
                return@withContext Result.failure(Exception(errorMsg))
            }

            val result = json.optJSONArray("result")
            val messages = mutableListOf<TelegramMessage>()
            var maxUpdateId: Long? = offset
            
            if (result != null) {
                for (i in 0 until result.length()) {
                    try {
                        val upd = result.optJSONObject(i) ?: continue
                        val updateId = upd.optLong("update_id")
                        if (maxUpdateId == null || updateId > maxUpdateId) {
                            maxUpdateId = updateId
                        }
                        val message = upd.optJSONObject("message")
                        val channelPost = upd.optJSONObject("channel_post")
                        val editedMessage = upd.optJSONObject("edited_message")
                        val editedChannelPost = upd.optJSONObject("edited_channel_post")
                        
                        val node = message ?: channelPost ?: editedMessage ?: editedChannelPost ?: continue
                        val chat = node.optJSONObject("chat") ?: continue
                        val chatId = chat.optLong("id")
                        
                        // Получаем текст из разных возможных источников
                        val text = node.optString("text", "").trim()
                        val caption = node.optString("caption", "").trim()
                        val messageText = if (text.isNotEmpty()) text else caption
                        
                        if (messageText.isNotEmpty()) {
                            val date = node.optLong("date", 0)
                            val messageId = node.optLong("message_id", 0)
                            messages.add(
                                TelegramMessage(
                                    chatId = chatId,
                                    messageId = messageId,
                                    dateSeconds = date,
                                    text = messageText
                                )
                            )
                        }
                    } catch (e: Exception) {
                        // Пропускаем некорректные обновления, продолжаем обработку остальных
                        continue
                    }
                }
            }
            
            // Если offset был установлен, но обновлений нет - это нормально (нет новых сообщений)
            Result.success(messages to maxUpdateId)
        } catch (e: Exception) {
            Result.failure(Exception("Ошибка при получении сообщений: ${e.message ?: e.javaClass.simpleName}"))
        }
    }

    suspend fun resolveChatOpenLink(token: String, chatId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val trimmed = chatId.trim()
            // Если это уже ссылка - используем как есть (включая invite links типа https://t.me/+...)
            if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                return@withContext Result.success(trimmed)
            }
            // Если это ссылка без протокола (t.me/+... или t.me/...)
            if (trimmed.startsWith("t.me/")) {
                return@withContext Result.success("https://$trimmed")
            }
            // Если это username (с @ или без) - формируем ссылку
            if (trimmed.startsWith("@") || trimmed.matches(Regex("^[A-Za-z0-9_]+$"))) {
                val username = trimmed.removePrefix("@")
                return@withContext Result.success("https://t.me/$username")
            }
            // Если это числовой ID (включая отрицательные для групп) - получаем данные через API
            val idLong = trimmed.toLongOrNull() ?: return@withContext Result.failure(
                Exception("Некорректный chat_id: '$trimmed'. Ожидается числовой ID (например: -1001234567890) или username (например: @groupname)")
            )
            
            // Пробуем получить информацию о чате через API
            val url = "https://api.telegram.org/bot$token/getChat?chat_id=$idLong"
            val request = Request.Builder().url(url).get().build()
            val response = httpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                val errorMsg = try {
                    val errorJson = JSONObject(errorBody)
                    val description = errorJson.optString("description", "Unknown error")
                    when {
                        description.contains("chat not found", ignoreCase = true) -> 
                            "Группа не найдена. Убедитесь, что chat_id корректный и бот является участником группы."
                        description.contains("not enough rights", ignoreCase = true) -> 
                            "Недостаточно прав. Бот должен быть участником группы."
                        else -> "Ошибка при получении информации о группе: $description"
                    }
                } catch (e: Exception) {
                    "HTTP error: ${response.code} - ${response.message}"
                }
                return@withContext Result.failure(Exception(errorMsg))
            }
            
            val bodyStr = response.body?.string() ?: "{}"
            val json = JSONObject(bodyStr)
            if (!json.optBoolean("ok", false)) {
                val description = json.optString("description", "Unknown error")
                val errorMsg = when {
                    description.contains("chat not found", ignoreCase = true) -> 
                        "Группа не найдена. Проверьте chat_id в настройках."
                    description.contains("not enough rights", ignoreCase = true) -> 
                        "Недостаточно прав для доступа к группе."
                    else -> "Telegram API error: $description"
                }
                return@withContext Result.failure(Exception(errorMsg))
            }
            
            val result = json.optJSONObject("result") ?: return@withContext Result.failure(
                Exception("Не удалось получить информацию о группе")
            )
            
            // Пробуем получить username
            val username = result.optString("username", "").trim()
            val inviteLink = result.optString("invite_link", "").trim()
            val chatType = result.optString("type", "").lowercase()
            
            val link = when {
                username.isNotBlank() -> {
                    // Группа с username - открываем через публичную ссылку
                    "https://t.me/$username"
                }
                inviteLink.isNotBlank() -> {
                    // Есть invite link - используем его
                    inviteLink
                }
                chatType == "group" || chatType == "supergroup" -> {
                    // Для приватных групп/супергрупп без username и без invite link
                    // можно попробовать открыть через deep link, но это может не сработать
                    val idStr = idLong.toString()
                    if (idStr.startsWith("-100")) {
                        // Супергруппа: пробуем формат https://t.me/c/{chat_id без префикса -100}
                        // Но для приватных групп это может не работать без дополнительных параметров
                        val shortId = idStr.removePrefix("-100")
                        // Пробуем оба варианта - сначала через deep link (предпочтительнее)
                        // Формат tg://openmessage?chat_id=... не работает для супергрупп
                        // Используем https://t.me/c/... который может работать если группа была публичной
                        "https://t.me/c/$shortId"
                    } else {
                        // Обычная группа: пробуем deep link (может не работать для приватных)
                        "tg://openmessage?chat_id=$idLong"
                    }
                }
                else -> {
                    // Неизвестный тип - пробуем deep link
                    "tg://resolve?chat_id=$idLong"
                }
            }
            
            Result.success(link)
        } catch (e: Exception) {
            val errorMsg = when {
                e.message?.contains("chat_id", ignoreCase = true) == true -> e.message
                else -> "Ошибка при получении ссылки на группу: ${e.message ?: e.javaClass.simpleName}"
            }
            Result.failure(Exception(errorMsg))
        }
    }

    suspend fun resolveChatNumericId(token: String, chatId: String): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val trimmed = chatId.trim()
            
            // Если это уже числовой ID - возвращаем как есть
            val idLong = trimmed.toLongOrNull()
            if (idLong != null) return@withContext Result.success(idLong)

            // Если это username - пытаемся получить ID через getChat
            // Пробуем разные форматы: с @ и без
            val usernameVariants = listOf(
                trimmed,
                if (trimmed.startsWith("@")) trimmed else "@$trimmed",
                if (trimmed.startsWith("@")) trimmed.removePrefix("@") else trimmed
            ).distinct()

            for (username in usernameVariants) {
                try {
                    val url = "https://api.telegram.org/bot$token/getChat?chat_id=" + java.net.URLEncoder.encode(username, "UTF-8")
                    val request = Request.Builder().url(url).get().build()
                    val response = httpClient.newCall(request).execute()
                    
                    if (response.isSuccessful) {
                        val bodyStr = response.body?.string() ?: "{}"
                        val json = JSONObject(bodyStr)
                        if (json.optBoolean("ok", false)) {
                            val result = json.optJSONObject("result")
                            val id = result?.optLong("id")
                            if (id != null) return@withContext Result.success(id)
                        }
                    }
                } catch (e: Exception) {
                    // Пробуем следующий вариант
                    continue
                }
            }
            
            Result.failure(Exception("Не удалось получить ID для чата '$trimmed'. Убедитесь, что бот является участником группы или используйте числовой chat_id (например: -1001234567890)"))
        } catch (e: Exception) {
            Result.failure(Exception("Ошибка при определении ID чата: ${e.message}"))
        }
    }
} 