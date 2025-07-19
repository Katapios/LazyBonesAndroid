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
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Close

// Мок-данные для верстки
private val mockLocalReports = listOf(
    ReportUi(
        date = Date(),
        good = listOf("Работавалы", "Спорт", "Питание"),
        bad = listOf("Переедание", "Мало сна"),
        isCustom = false,
        isSaved = true
    )
)
private val mockCustomReports = listOf(
    ReportUi(
        date = Date(System.currentTimeMillis() - 86400000L),
        good = listOf(),
        bad = listOf("бловорыва", "Цыфрфыфр", "Фывыфыфр", "Фывыфыф", "11111", "Фукака"),
        isCustom = true,
        isSaved = false
    )
)

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
    
    // Отдельные состояния для локальных и кастомных отчётов
    var localSelectionMode by remember { mutableStateOf(false) }
    var selectedLocalReports by remember { mutableStateOf(setOf<Int>()) }
    
    var customSelectionMode by remember { mutableStateOf(false) }
    var selectedCustomReports by remember { mutableStateOf(setOf<Int>()) }
    
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
                            println("DEBUG: Deleting local posts - selectedLocalReports: $selectedLocalReports")
                            // Получаем актуальный список постов перед удалением
                            val currentPosts = posts.toList()
                            println("DEBUG: Current posts count: ${currentPosts.size}")
                            selectedLocalReports.forEach { idx ->
                                if (idx < currentPosts.size) {
                                    println("DEBUG: Deleting local post at index: $idx")
                                    viewModel.deletePost(currentPosts[idx])
                                }
                            }
                            localSelectionMode = false
                            selectedLocalReports = emptySet()
                            println("DEBUG: Local selection cleared")
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
                                println("DEBUG: Entered local selection mode - selectedLocalReports: $selectedLocalReports")
                            } else {
                                // В режиме выделения переключаем состояние элемента
                                selectedLocalReports = if (selectedLocalReports.contains(idx))
                                    selectedLocalReports - idx else selectedLocalReports + idx
                                println("DEBUG: Local selection mode - selectedLocalReports: $selectedLocalReports")
                                
                                // Если сняли выделение со всех элементов, выключаем режим выделения
                                if (selectedLocalReports.isEmpty()) {
                                    localSelectionMode = false
                                    println("DEBUG: Local selection mode disabled - no items selected")
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
                    totalCount = mockCustomReports.size,
                    onSelectAll = {
                        customSelectionMode = true
                        selectedCustomReports = mockCustomReports.indices.toSet()
                    },
                    onCancel = {
                        customSelectionMode = false
                        selectedCustomReports = emptySet()
                    },
                    onDelete = {
                        coroutineScope.launch {
                            println("DEBUG: Deleting custom reports - selectedCustomReports: $selectedCustomReports")
                            // Здесь можно добавить логику удаления кастомных отчётов
                            // Пока что просто очищаем выделение
                            customSelectionMode = false
                            selectedCustomReports = emptySet()
                            println("DEBUG: Custom selection cleared")
                        }
                    }
                )
            }
            items(mockCustomReports.indices.toList()) { idx ->
                val report = mockCustomReports[idx]
                ReportCard(
                    report = report,
                    dateFormat = dateFormat,
                    selected = selectedCustomReports.contains(idx),
                    onSelect = {
                        // Всегда включаем режим выделения при первом нажатии на отчёт
                        if (!customSelectionMode) {
                            customSelectionMode = true
                            selectedCustomReports = setOf(idx)
                            println("DEBUG: Entered custom selection mode - selectedCustomReports: $selectedCustomReports")
                        } else {
                            // В режиме выделения переключаем состояние элемента
                            selectedCustomReports = if (selectedCustomReports.contains(idx))
                                selectedCustomReports - idx else selectedCustomReports + idx
                            println("DEBUG: Custom selection mode - selectedCustomReports: $selectedCustomReports")
                            
                            // Если сняли выделение со всех элементов, выключаем режим выделения
                            if (selectedCustomReports.isEmpty()) {
                                customSelectionMode = false
                                println("DEBUG: Custom selection mode disabled - no items selected")
                            }
                        }
                    },
                    onSend = {},
                    onDelete = {},
                    onSave = {}
                )
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
                Text(
                    text = dateFormat.format(report.date),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                if (report.isSaved) {
                    Icon(Icons.Default.Done, contentDescription = "Сохранено", tint = Color(0xFF4CAF50))
                }
                if (report.isCustom) {
                    IconButton(onClick = onSend) {
                        Icon(Icons.Default.Send, contentDescription = "Отправить", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            if (report.good.isNotEmpty()) {
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