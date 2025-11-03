package com.katapandroid.lazybones.ui.util

import android.content.Intent
import android.net.Uri

object TelegramIntents {
    fun openChatIntent(link: String, forceGeneric: Boolean = false): Intent {
        val uri = Uri.parse(link)
        return Intent(Intent.ACTION_VIEW, uri).apply {
            if (!forceGeneric) {
                // Пробуем разные возможные package names для Telegram
                // Система выберет установленное приложение
                val telegramPackages = listOf(
                    "org.telegram.messenger",      // Официальный Telegram
                    "org.telegram.messenger.web",  // Telegram Web
                    "org.telegram.plus"             // Telegram Plus
                )
                // Не указываем конкретный package - позволим системе выбрать
                // setPackage может не работать если указанный package не установлен
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
    }
}


