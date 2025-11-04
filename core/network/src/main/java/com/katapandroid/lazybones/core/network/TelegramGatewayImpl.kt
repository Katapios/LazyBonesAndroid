package com.katapandroid.lazybones.core.network

import com.katapandroid.lazybones.core.domain.model.Post
import com.katapandroid.lazybones.core.domain.model.TelegramMessage
import com.katapandroid.lazybones.core.domain.service.TelegramGateway
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class TelegramGatewayImpl : TelegramGateway {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    override suspend fun sendTestMessage(token: String, chatId: String): Result<String> = withContext(Dispatchers.IO) {
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

    override suspend fun publishReport(
        token: String,
        chatId: String,
        post: Post,
        deviceName: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

            val message = buildString {
                appendLine("📅 Отчёт за ${dateFormat.format(post.date)}")
                appendLine("📱 Устройство: ${deviceName.ifEmpty { DEFAULT_DEVICE_NAME }}")
                appendLine("⏰ Время: ${timeFormat.format(post.date)}")
                appendLine()

                if (post.checklist.isNotEmpty()) {
                    appendLine("📋 План на день:")
                    post.checklist.forEachIndexed { index, item ->
                        val isCompleted = post.goodItems.contains(item)
                        val icon = if (isCompleted) "✅" else "❌"
                        appendLine("${index + 1}. $icon $item")
                    }
                    appendLine()
                }

                if (post.checklist.isEmpty()) {
                    if (post.goodItems.isNotEmpty()) {
                        appendLine("✅ Я молодец:")
                        post.goodItems.forEachIndexed { index, item ->
                            appendLine("${index + 1}. ✅ $item")
                        }
                        appendLine()
                    }
                    if (post.badItems.isNotEmpty()) {
                        appendLine("❌ Я не молодец:")
                        post.badItems.forEachIndexed { index, item ->
                            appendLine("${index + 1}. ❌ $item")
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

    override suspend fun fetchRecentMessages(
        token: String,
        offset: Long?
    ): Result<Pair<List<TelegramMessage>, Long?>> = withContext(Dispatchers.IO) {
        try {
            val url = buildGetUpdatesUrl(token, offset)
            val request = Request.Builder().url(url).get().build()
            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                val errorMessage = response.body?.string()?.let { body ->
                    try {
                        val json = JSONObject(body)
                        json.optString("description", "HTTP error: ${response.code}")
                    } catch (_: Exception) {
                        "HTTP error: ${response.code} - ${response.message}"
                    }
                } ?: "HTTP error: ${response.code} - ${response.message}"
                return@withContext Result.failure(Exception(errorMessage))
            }

            val body = response.body?.string() ?: "{}"
            val json = JSONObject(body)
            if (!json.optBoolean("ok", false)) {
                val description = json.optString("description", "Unknown error")
                val errorCode = json.optInt("error_code", 0)
                val message = if (errorCode > 0) {
                    "Telegram API error ($errorCode): $description"
                } else {
                    "Telegram API error: $description"
                }
                return@withContext Result.failure(Exception(message))
            }

            val resultArray = json.optJSONArray("result")
            val messages = mutableListOf<TelegramMessage>()
            var maxUpdateId: Long? = offset

            if (resultArray != null) {
                for (index in 0 until resultArray.length()) {
                    val update = resultArray.optJSONObject(index) ?: continue
                    val updateId = update.optLong("update_id")
                    if (maxUpdateId == null || updateId > maxUpdateId) {
                        maxUpdateId = updateId
                    }

                    val messageNode = update.optJSONObject("message")
                        ?: update.optJSONObject("channel_post")
                        ?: update.optJSONObject("edited_message")
                        ?: update.optJSONObject("edited_channel_post")
                        ?: continue

                    val chat = messageNode.optJSONObject("chat") ?: continue
                    val chatId = chat.optLong("id")

                    val text = messageNode.optString("text", "").ifBlank {
                        messageNode.optString("caption", "")
                    }.trim()

                    if (text.isNotEmpty()) {
                        val dateSeconds = messageNode.optLong("date", 0)
                        val messageId = messageNode.optLong("message_id", 0)
                        messages.add(
                            TelegramMessage(
                                chatId = chatId,
                                messageId = messageId,
                                dateSeconds = dateSeconds,
                                text = text
                            )
                        )
                    }
                }
            }

            Result.success(messages to maxUpdateId)
        } catch (e: Exception) {
            Result.failure(Exception("Ошибка при получении сообщений: ${e.message ?: e.javaClass.simpleName}"))
        }
    }

    override suspend fun resolveChatOpenLink(token: String, chatId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val trimmed = chatId.trim()
            if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                return@withContext Result.success(trimmed)
            }
            if (trimmed.startsWith("t.me/")) {
                return@withContext Result.success("https://$trimmed")
            }
            if (trimmed.startsWith("@") || trimmed.matches(USERNAME_REGEX)) {
                val username = trimmed.removePrefix("@")
                return@withContext Result.success("https://t.me/$username")
            }

            val numericId = trimmed.toLongOrNull()
                ?: return@withContext Result.failure(Exception("Некорректный chat_id: '$trimmed'."))

            val url = "https://api.telegram.org/bot$token/getChat?chat_id=$numericId"
            val request = Request.Builder().url(url).get().build()
            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                val errorMsg = try {
                    val errorJson = JSONObject(errorBody)
                    errorJson.optString("description", "HTTP error: ${response.code}")
                } catch (_: Exception) {
                    "HTTP error: ${response.code} - ${response.message}"
                }
                return@withContext Result.failure(Exception(errorMsg))
            }

            val bodyStr = response.body?.string() ?: "{}"
            val json = JSONObject(bodyStr)
            if (!json.optBoolean("ok", false)) {
                val description = json.optString("description", "Unknown error")
                return@withContext Result.failure(Exception("Telegram API error: $description"))
            }

            val result = json.optJSONObject("result") ?: return@withContext Result.failure(
                Exception("Не удалось получить информацию о группе")
            )

            val username = result.optString("username", "").trim()
            val inviteLink = result.optString("invite_link", "").trim()
            val chatType = result.optString("type", "").lowercase(Locale.getDefault())

            val link = when {
                username.isNotEmpty() -> "https://t.me/$username"
                inviteLink.isNotEmpty() -> inviteLink
                chatType == "group" || chatType == "supergroup" -> {
                    val idStr = numericId.toString()
                    if (idStr.startsWith("-100")) {
                        val shortId = idStr.removePrefix("-100")
                        "https://t.me/c/$shortId"
                    } else {
                        "tg://openmessage?chat_id=$numericId"
                    }
                }
                else -> "tg://resolve?chat_id=$numericId"
            }

            Result.success(link)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Ошибка при получении ссылки на группу"))
        }
    }

    override suspend fun resolveChatNumericId(token: String, chatId: String): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val trimmed = chatId.trim()
            trimmed.toLongOrNull()?.let { return@withContext Result.success(it) }

            val variants = listOf(
                trimmed,
                if (trimmed.startsWith("@")) trimmed else "@$trimmed",
                if (trimmed.startsWith("@")) trimmed.removePrefix("@") else trimmed
            ).distinct()

            for (variant in variants) {
                try {
                    val encoded = java.net.URLEncoder.encode(variant, "UTF-8")
                    val url = "https://api.telegram.org/bot$token/getChat?chat_id=$encoded"
                    val request = Request.Builder().url(url).get().build()
                    val response = httpClient.newCall(request).execute()

                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: "{}"
                        val json = JSONObject(body)
                        if (json.optBoolean("ok", false)) {
                            val result = json.optJSONObject("result")
                            val id = result?.optLong("id")
                            if (id != null) {
                                return@withContext Result.success(id)
                            }
                        }
                    }
                } catch (_: Exception) {
                    // ignore and try next variant
                }
            }

            Result.failure(Exception("Не удалось получить ID для чата '$trimmed'."))
        } catch (e: Exception) {
            Result.failure(Exception("Ошибка при определении ID чата: ${e.message}"))
        }
    }

    private fun buildGetUpdatesUrl(token: String, offset: Long?): String = buildString {
        append("https://api.telegram.org/bot")
        append(token)
        append("/getUpdates?limit=100")
        if (offset != null && offset > 0) {
            append("&offset=")
            append(offset)
        }
        append("&allowed_updates=%5B%22message%22%2C%22channel_post%22%5D")
    }

    private fun sendMessage(token: String, chatId: String, text: String): Result<String> {
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
                if (jsonResponse.optBoolean("ok", false)) {
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

    companion object {
        private const val DEFAULT_DEVICE_NAME = "LazyBones"
        private val USERNAME_REGEX = Regex("^[A-Za-z0-9_]+$")
    }
}
