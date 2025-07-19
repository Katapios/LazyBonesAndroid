package com.katapandroid.lazybones.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.text.SimpleDateFormat
import java.util.*
import com.katapandroid.lazybones.data.Post
import org.koin.androidx.compose.koinViewModel

@Composable
fun CustomReportEvaluationScreen(
    post: Post,
    onDismiss: () -> Unit,
    onSave: (Post) -> Unit,
    viewModel: ReportsViewModel = koinViewModel()
) {
    var checkedItems by remember { mutableStateOf(setOf<Int>()) }
    val dateFormat = remember { SimpleDateFormat("d MMMM yyyy", Locale.getDefault()) }
    
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
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Оценка отчёта",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Дата отчёта
                Text(
                    text = dateFormat.format(post.date),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                
                // Список пунктов
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(post.checklist.indices.toList()) { idx ->
                        val item = post.checklist[idx]
                        val isChecked = checkedItems.contains(idx)
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isChecked) 
                                    MaterialTheme.colorScheme.primaryContainer 
                                else 
                                    MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${idx + 1}. $item",
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        checkedItems = if (isChecked) {
                                            checkedItems - idx
                                        } else {
                                            checkedItems + idx
                                        }
                                    }
                                ) {
                                                                    Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = if (isChecked) "Выполнено" else "Не выполнено",
                                    tint = if (isChecked) Color(0xFF4CAF50) else Color(0xFF9E9E9E),
                                    modifier = Modifier.size(24.dp)
                                )
                                }
                            }
                        }
                    }
                }
                
                // Кнопка сохранения
                Button(
                    onClick = {
                        val goodItems = checkedItems.map { post.checklist[it] }
                        val badItems = post.checklist.filterIndexed { idx, _ -> !checkedItems.contains(idx) }
                        val isGood = goodItems.size > badItems.size
                        
                        val updatedPost = post.copy(
                            goodItems = goodItems,
                            badItems = badItems,
                            goodCount = goodItems.size,
                            badCount = badItems.size
                        )
                        
                        onSave(updatedPost)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = "Сохранить оценку",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
} 