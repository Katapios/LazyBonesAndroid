package com.katapandroid.lazybones.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.AnimatedVisibility

@Composable
fun SettingsScreen() {
    var phoneName by remember { mutableStateOf(TextFieldValue()) }
    var telegramToken by remember { mutableStateOf(TextFieldValue()) }
    var telegramChatId by remember { mutableStateOf(TextFieldValue()) }
    var telegramBotId by remember { mutableStateOf(TextFieldValue()) }
    var notificationsEnabled by remember { mutableStateOf(false) }
    var notificationMode by remember { mutableStateOf(0) } // 0 = каждый час, 1 = 2 раза в день
    val notificationTimes = if (notificationMode == 0) listOf("17:00", "18:00", "19:00", "20:00", "21:00") else listOf("12:00", "21:00")

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Настройки",
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 24.dp)
        )
        // Имя телефона для виджета
        Text("ИМЯ ТЕЛЕФОНА ДЛЯ ВИДЖЕТА", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Card(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = phoneName,
                    onValueChange = { phoneName = it },
                    placeholder = { Text("Введите имя телефона") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { /* save phone name */ },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Сохранить имя")
                }
            }
        }
        // Telegram
        Text("ИНТЕГРАЦИЯ С ГРУППОЙ В ТЕЛЕГРАММ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Card(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = telegramToken,
                    onValueChange = { telegramToken = it },
                    placeholder = { Text("Токен Telegram-бота") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = telegramChatId,
                    onValueChange = { telegramChatId = it },
                    placeholder = { Text("chat_id группы") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = telegramBotId,
                    onValueChange = { telegramBotId = it },
                    placeholder = { Text("ID бота (опционально)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { /* save telegram data */ },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Сохранить Telegram-данные")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { /* check telegram connection */ },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Проверить связь")
                }
            }
        }
        // Уведомления
        Text("НАСТРОЙКА УВЕДОМЛЕНИЙ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Card(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Получать уведомления", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Switch(checked = notificationsEnabled, onCheckedChange = { notificationsEnabled = it })
                }
                AnimatedVisibility(visible = notificationsEnabled) {
                    Column {
                        Spacer(Modifier.height(8.dp))
                        SegmentedButtonRow(
                            options = listOf("Каждый час", "2 раза в день"),
                            selectedIndex = notificationMode,
                            onSelect = { notificationMode = it }
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (notificationMode == 0)
                                "Уведомления каждый час с 8:00 до 21:00.\nПоследнее уведомление в 21:00 — предостерегающее."
                            else
                                "Уведомления в 12:00 и 21:00.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("Сегодня уведомления:", style = MaterialTheme.typography.labelMedium)
                        Text(notificationTimes.joinToString(), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SegmentedButtonRow(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        options.forEachIndexed { idx, text ->
            val selected = idx == selectedIndex
            Button(
                onClick = { onSelect(idx) },
                shape = RoundedCornerShape(8.dp),
                colors = if (selected) ButtonDefaults.buttonColors()
                else ButtonDefaults.outlinedButtonColors(),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 2.dp)
            ) {
                Text(text)
                if (selected) Icon(Icons.Default.Check, contentDescription = null)
            }
        }
    }
} 