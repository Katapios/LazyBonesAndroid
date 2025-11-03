package com.katapandroid.lazybones.network

data class TelegramMessage(
    val chatId: Long,
    val messageId: Long,
    val dateSeconds: Long,
    val text: String
)


