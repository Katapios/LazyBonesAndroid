package com.katapandroid.lazybones.core.domain.service

import com.katapandroid.lazybones.core.domain.model.Post
import com.katapandroid.lazybones.core.domain.model.TelegramMessage

interface TelegramGateway {
    suspend fun sendTestMessage(token: String, chatId: String): Result<String>
    suspend fun publishReport(
        token: String,
        chatId: String,
        post: Post,
        deviceName: String = "LazyBones"
    ): Result<String>
    suspend fun fetchRecentMessages(token: String, offset: Long?): Result<Pair<List<TelegramMessage>, Long?>>
    suspend fun resolveChatOpenLink(token: String, chatId: String): Result<String>
    suspend fun resolveChatNumericId(token: String, chatId: String): Result<Long>
}
