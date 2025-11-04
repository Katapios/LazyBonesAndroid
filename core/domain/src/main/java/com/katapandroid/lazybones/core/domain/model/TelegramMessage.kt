package com.katapandroid.lazybones.core.domain.model

data class TelegramMessage(
    val chatId: Long,
    val messageId: Long,
    val dateSeconds: Long,
    val text: String
)
