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
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = koinViewModel()) {
    val phoneName by viewModel.phoneName.collectAsState()
    val telegramToken by viewModel.telegramToken.collectAsState()
    val telegramChatId by viewModel.telegramChatId.collectAsState()
    val telegramBotId by viewModel.telegramBotId.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val notificationMode by viewModel.notificationMode.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val testMessageResult by viewModel.testMessageResult.collectAsState()
    val phoneNameSaveStatus by viewModel.phoneNameSaveStatus.collectAsState()
    
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
        Surface(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = phoneName,
                    onValueChange = { 
                        android.util.Log.d("SettingsScreen", "Phone name changed to: '$it'")
                        viewModel.setPhoneName(it) 
                    },
                    placeholder = { Text("Введите имя телефона") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Имя устройства") }
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { 
                        android.util.Log.d("SettingsScreen", "Save button clicked, current phoneName: '$phoneName'")
                        viewModel.savePhoneName()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Сохранить имя")
                }
                
                // Показываем статус сохранения
                phoneNameSaveStatus?.let { status ->
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = status,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = { viewModel.clearPhoneNameSaveStatus() }
                            ) {
                                Text(
                                    text = "✕",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
        // Telegram
        Text("ИНТЕГРАЦИЯ С ГРУППОЙ В ТЕЛЕГРАММ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = telegramToken,
                    onValueChange = { viewModel.setTelegramToken(it) },
                    placeholder = { Text("Токен Telegram-бота") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = telegramChatId,
                    onValueChange = { viewModel.setTelegramChatId(it) },
                    placeholder = { Text("chat_id группы") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = telegramBotId,
                    onValueChange = { viewModel.setTelegramBotId(it) },
                    placeholder = { Text("ID бота (опционально)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.saveTelegramSettings() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Сохранить Telegram-данные")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { viewModel.testTelegramConnection() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (isLoading) "Проверка..." else "Проверить связь")
                }
                
                // Показываем результат тестирования
                testMessageResult?.let { result ->
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = if (result.startsWith("✅")) 
                            MaterialTheme.colorScheme.primaryContainer 
                        else 
                            MaterialTheme.colorScheme.errorContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = result,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (result.startsWith("✅")) 
                                    MaterialTheme.colorScheme.onPrimaryContainer 
                                else 
                                    MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(Modifier.weight(1f))
                            TextButton(
                                onClick = { viewModel.clearTestMessageResult() }
                            ) {
                                Text("✕", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
        // Уведомления
        Text("НАСТРОЙКА УВЕДОМЛЕНИЙ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Получать уведомления", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Switch(checked = notificationsEnabled, onCheckedChange = { viewModel.setNotificationsEnabled(it) })
                }
                AnimatedVisibility(visible = notificationsEnabled) {
                    Column {
                        Spacer(Modifier.height(8.dp))
                        SegmentedButtonRow(
                            options = listOf("Каждый час", "2 раза в день"),
                            selectedIndex = notificationMode,
                            onSelect = { viewModel.setNotificationMode(it) }
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
                shape = RoundedCornerShape(8.dp)
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