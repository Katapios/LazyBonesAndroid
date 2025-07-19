package com.katapandroid.lazybones.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.text.SimpleDateFormat
import java.util.*
import com.katapandroid.lazybones.data.Post
import org.koin.androidx.compose.koinViewModel

@Composable
fun TelegramPublishDialog(
    post: Post,
    onDismiss: () -> Unit,
    onPublish: (String, String) -> Unit,
    isPublishing: Boolean,
    publishResult: String?,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val savedToken by viewModel.telegramToken.collectAsState()
    val savedChatId by viewModel.telegramChatId.collectAsState()
    
    var token by remember { mutableStateOf(savedToken) }
    var chatId by remember { mutableStateOf(savedChatId) }
    val dateFormat = remember { SimpleDateFormat("d MMMM yyyy", Locale.getDefault()) }
    
    // Обновляем поля при изменении сохраненных настроек
    LaunchedEffect(savedToken) {
        if (token.isEmpty()) {
            token = savedToken
        }
    }
    LaunchedEffect(savedChatId) {
        if (chatId.isEmpty()) {
            chatId = savedChatId
        }
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Заголовок
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Публикация в Telegram",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть")
                    }
                }
                
                // Информация об отчете
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Отчет за ${dateFormat.format(post.date)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Пунктов в плане: ${post.checklist.size}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (post.goodItems.isNotEmpty() || post.badItems.isNotEmpty()) {
                            Text(
                                text = "Выполнено: ${post.goodItems.size}, Не выполнено: ${post.badItems.size}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                // Настройки Telegram
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "Настройки Telegram",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = token,
                        onValueChange = { token = it },
                        placeholder = { Text("Токен Telegram-бота") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isPublishing
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = chatId,
                        onValueChange = { chatId = it },
                        placeholder = { Text("chat_id группы") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isPublishing
                    )
                }
                
                Spacer(Modifier.weight(1f))
                
                // Результат публикации
                publishResult?.let { result ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (result.startsWith("✅")) 
                                MaterialTheme.colorScheme.primaryContainer 
                            else 
                                MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = result,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (result.startsWith("✅")) 
                                MaterialTheme.colorScheme.onPrimaryContainer 
                            else 
                                MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }
                
                // Кнопки
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !isPublishing
                    ) {
                        Text("Отмена")
                    }
                    
                    Button(
                        onClick = { onPublish(token, chatId) },
                        modifier = Modifier.weight(1f),
                        enabled = token.isNotEmpty() && chatId.isNotEmpty() && !isPublishing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (isPublishing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Icon(
                            Icons.Default.Send,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (isPublishing) "Публикация..." else "Опубликовать")
                    }
                }
            }
        }
    }
} 