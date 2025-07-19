package com.katapandroid.lazybones.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*
import org.koin.androidx.compose.koinViewModel
import com.katapandroid.lazybones.data.Post
import com.katapandroid.lazybones.ui.ReportsViewModel
import org.koin.androidx.compose.getViewModel
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Close

// Мок-данные больше не нужны, используем реальные данные из базы

data class ReportUi(
    val date: Date,
    val good: List<String>,
    val bad: List<String>,
    val isCustom: Boolean,
    val isSaved: Boolean
)

@Composable
fun ReportsScreen(viewModel: ReportsViewModel = koinViewModel()) {
    val posts by viewModel.posts.collectAsState()
    val customPosts by viewModel.customPosts.collectAsState()
    
    // Отдельные состояния для локальных и кастомных отчётов
    var localSelectionMode by remember { mutableStateOf(false) }
    var selectedLocalReports by remember { mutableStateOf(setOf<Int>()) }
    
    var customSelectionMode by remember { mutableStateOf(false) }
    var selectedCustomReports by remember { mutableStateOf(setOf<Int>()) }
    
    // Состояние для экрана оценки
    var showEvaluationScreen by remember { mutableStateOf<Post?>(null) }
    
    val dateFormat = remember { SimpleDateFormat("d MMMM yyyy", Locale.getDefault()) }
    val coroutineScope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Локальные отчёты
            item {
                SectionHeader(
                    title = "ЛОКАЛЬНЫЕ ОТЧЁТЫ",
                    selectionMode = localSelectionMode,
                    selectedCount = selectedLocalReports.size,
                    totalCount = posts.size,
                    onSelectAll = {
                        localSelectionMode = true
                        selectedLocalReports = posts.indices.toSet()
                    },
                    onCancel = {
                        localSelectionMode = false
                        selectedLocalReports = emptySet()
                    },
                    onDelete = {
                        coroutineScope.launch {
                            // Получаем актуальный список постов перед удалением
                            val currentPosts = posts.toList()
                            selectedLocalReports.forEach { idx ->
                                if (idx < currentPosts.size) {
                                    viewModel.deletePost(currentPosts[idx])
                                }
                            }
                            localSelectionMode = false
                            selectedLocalReports = emptySet()
                        }
                    }
                )
            }
            if (posts.isEmpty()) {
                item {
                    Text(
                        text = "Еще нет созданных локальных отчетов",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 16.dp)
                    )
                }
            } else {
                items(posts.indices.toList()) { idx ->
                    val post = posts[idx]
                    val report = ReportUi(
                        date = post.date,
                        good = post.goodItems,
                        bad = post.badItems,
                        isCustom = false,
                        isSaved = true
                    )
                    ReportCard(
                        report = report,
                        dateFormat = dateFormat,
                        selected = selectedLocalReports.contains(idx),
                        onSelect = {
                            // Всегда включаем режим выделения при первом нажатии на отчёт
                            if (!localSelectionMode) {
                                localSelectionMode = true
                                selectedLocalReports = setOf(idx)
                            } else {
                                // В режиме выделения переключаем состояние элемента
                                selectedLocalReports = if (selectedLocalReports.contains(idx))
                                    selectedLocalReports - idx else selectedLocalReports + idx
                                
                                // Если сняли выделение со всех элементов, выключаем режим выделения
                                if (selectedLocalReports.isEmpty()) {
                                    localSelectionMode = false
                                }
                            }
                        },
                        onSend = {},
                        onDelete = {},
                        onSave = {}
                    )
                }
            }
            // Кастомные отчёты
            item {
                Spacer(Modifier.height(16.dp))
                SectionHeader(
                    title = "КАСТОМНЫЕ ОТЧЁТЫ",
                    selectionMode = customSelectionMode,
                    selectedCount = selectedCustomReports.size,
                    totalCount = customPosts.size,
                    onSelectAll = {
                        customSelectionMode = true
                        selectedCustomReports = customPosts.indices.toSet()
                    },
                    onCancel = {
                        customSelectionMode = false
                        selectedCustomReports = emptySet()
                    },
                    onDelete = {
                        coroutineScope.launch {
                            val currentCustomPosts = customPosts.toList()
                            selectedCustomReports.forEach { idx ->
                                if (idx < currentCustomPosts.size) {
                                    viewModel.deleteCustomPost(currentCustomPosts[idx])
                                }
                            }
                            customSelectionMode = false
                            selectedCustomReports = emptySet()
                        }
                    }
                )
            }
            if (customPosts.isEmpty()) {
                item {
                    Text(
                        text = "Еще нет созданных кастомных отчетов",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 16.dp)
                    )
                }
            } else {
                items(customPosts.indices.toList()) { idx ->
                    val post = customPosts[idx]
                    val report = ReportUi(
                        date = post.date,
                        good = post.goodItems, // Для кастомных отчетов это будет пусто, пока не оценены
                        bad = post.badItems,   // Для кастомных отчетов это будет пусто, пока не оценены
                        isCustom = true,
                        isSaved = post.goodItems.isNotEmpty() || post.badItems.isNotEmpty() // Оценен ли отчет
                    )
                    ReportCard(
                        report = report,
                        dateFormat = dateFormat,
                        selected = selectedCustomReports.contains(idx),
                        onSelect = {
                            // Всегда включаем режим выделения при первом нажатии на отчёт
                            if (!customSelectionMode) {
                                customSelectionMode = true
                                selectedCustomReports = setOf(idx)
                            } else {
                                // В режиме выделения переключаем состояние элемента
                                selectedCustomReports = if (selectedCustomReports.contains(idx))
                                    selectedCustomReports - idx else selectedCustomReports + idx
                                
                                // Если сняли выделение со всех элементов, выключаем режим выделения
                                if (selectedCustomReports.isEmpty()) {
                                    customSelectionMode = false
                                }
                            }
                        },
                        onSend = {},
                        onDelete = { showEvaluationScreen = post },
                        onSave = {}
                    )
                }
            }
            // --- Секция ИЗ TELEGRAM ---
            item {
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "ИЗ TELEGRAM",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp, start = 0.dp, end = 0.dp)
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = { /* обновить */ }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Done, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(4.dp))
                        Text("Обновить")
                    }
                    OutlinedButton(onClick = { /* в группу */ }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Send, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(4.dp))
                        Text("В группу")
                    }
                }
                TelegramReportCard()
                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Техническое ограничение", color = Color(0xFFFF9800), style = MaterialTheme.typography.labelMedium)
                            Text(
                                "Бот не может видеть свои собственные сообщения через Telegram Bot API. Поэтому отправленные вами отчёты могут не отображаться в этом списке.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                OutlinedButton(
                    onClick = { /* очистить историю */ },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(4.dp))
                    Text("Очистить всю историю")
                }
            }
        }
    }
    
    // Экран оценки кастомного отчёта
    showEvaluationScreen?.let { post ->
        CustomReportEvaluationScreen(
            post = post,
            onDismiss = { showEvaluationScreen = null },
            onSave = { updatedPost ->
                coroutineScope.launch {
                    viewModel.updatePost(updatedPost)
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(
    title: String, 
    selectionMode: Boolean, 
    selectedCount: Int = 0,
    totalCount: Int = 0,
    onSelectAll: () -> Unit, 
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        if (selectionMode) {
            if (selectedCount > 0) {
                TextButton(onClick = onDelete) { 
                    Text("Удалить", color = MaterialTheme.colorScheme.error) 
                }
                TextButton(onClick = onCancel) { Text("Отмена") }
            }
        } else {
            if (totalCount > 0) {
                TextButton(onClick = onSelectAll) { Text("Выбрать все") }
            }
        }
    }
}

@Composable
private fun ReportCard(
    report: ReportUi,
    dateFormat: SimpleDateFormat,
    selected: Boolean,
    onSelect: () -> Unit,
    onSend: () -> Unit,
    onDelete: () -> Unit,
    onSave: () -> Unit
) {
    // Получаем оригинальный пост для доступа к checklist
    val posts by (koinViewModel<ReportsViewModel>().posts.collectAsState())
    val customPosts by (koinViewModel<ReportsViewModel>().customPosts.collectAsState())
    
    // Находим соответствующий пост
    val originalPost = if (report.isCustom) {
        customPosts.find { it.date == report.date }
    } else {
        posts.find { it.date == report.date }
    }
    
    val checklist = originalPost?.checklist ?: emptyList()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onSelect() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (report.isSaved) {
                    Icon(Icons.Default.Done, contentDescription = "Сохранено", tint = Color(0xFF4CAF50))
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = dateFormat.format(report.date),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                if (report.isCustom) {
                    IconButton(onClick = onSend) {
                        Icon(Icons.Default.Send, contentDescription = "Отправить", tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = onDelete,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Оценить", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            if (report.isCustom && !report.isSaved) {
                // Для неоценённых кастомных отчётов показываем checklist
                if (checklist.isNotEmpty()) {
                    Text("План на день:", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    checklist.forEachIndexed { idx, item ->
                        Text("${idx + 1}. $item", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else if (report.isCustom && report.isSaved) {
                // Для оценённых кастомных отчётов показываем и план, и результаты
                if (checklist.isNotEmpty()) {
                    Text("План на день:", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    checklist.forEachIndexed { idx, item ->
                        val isCompleted = report.good.contains(item)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${idx + 1}. $item", style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                if (isCompleted) Icons.Default.Check else Icons.Default.Close,
                                contentDescription = if (isCompleted) "Выполнено" else "Не выполнено",
                                tint = if (isCompleted) Color(0xFF4CAF50) else Color(0xFFD32F2F),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            } else if (report.good.isNotEmpty()) {
                // Для обычных локальных отчётов показываем good items
                Text("Я молодец:", color = Color(0xFF388E3C), fontWeight = FontWeight.Bold)
                report.good.forEachIndexed { idx, item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${idx + 1}. $item", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                    }
                }
            }
            if (report.bad.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                // Для оценённых кастомных отчётов и обычных отчётов показываем bad items
                Text("Я не молодец:", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                report.bad.forEachIndexed { idx, item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${idx + 1}. $item", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TelegramReportCard() {
    Card(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("18 July 2025", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.Send, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Text("Автор: GroupAnonymousBot", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text("📅 Отчёт за вторник, 15 июля 2025 г.\n📱 Устройство: Личинка", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            Text("✅ Я молодец:", color = Color(0xFF388E3C), fontWeight = FontWeight.Bold)
            Text("1. ✅ Работал, созванивался\n2. ✅ Делал зарядку\n3. ✅ Лёг спать пораньше", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Text("❌ Я не молодец:", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
            Text(
                "1. ❌ Пока мало что успешно сделал по работе\n2. ❌ Не решил рабочую задачу\n3. ❌ Опять объяснял Ай что нужно сохранить деньги на чёрный день, и опять оправдывался приводя примеры как мы жили до этого и что если я потеряю работу. Про такое точно надо молчать, и тупо говорить я так решил и не объяснять. Но я постоянно разъясняю, что как мне кажется оправдание.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(4.dp))
            Text("Опубликовано", color = Color(0xFF388E3C), style = MaterialTheme.typography.labelMedium)
        }
    }
}