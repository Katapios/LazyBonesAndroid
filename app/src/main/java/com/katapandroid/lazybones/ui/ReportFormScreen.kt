package com.katapandroid.lazybones.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.katapandroid.lazybones.ui.ReportFormViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import org.koin.androidx.compose.getViewModel
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.AnimatedVisibility

@Composable
fun ReportFormScreen(viewModel: ReportFormViewModel = getViewModel(), onBack: () -> Unit = {}) {
    val goodTags by viewModel.goodTags.collectAsState()
    val badTags by viewModel.badTags.collectAsState()

    // Состояния good/bad
    var selectedTab by remember { mutableStateOf(0) } // 0 = good, 1 = bad
    var wheelGoodIdx by remember { mutableStateOf(0) }
    var wheelBadIdx by remember { mutableStateOf(0) }
    var selectedGoodTags by remember { mutableStateOf(listOf<String>()) }
    var selectedBadTags by remember { mutableStateOf(listOf<String>()) }
    var goodFields by remember { mutableStateOf(mapOf<String, TextFieldValue>()) }
    var badFields by remember { mutableStateOf(mapOf<String, TextFieldValue>()) }

    val allGoodTags = goodTags.map { it.text }
    val allBadTags = badTags.map { it.text }

    val wheelTags = if (selectedTab == 0) allGoodTags.filter { it !in selectedGoodTags } else allBadTags.filter { it !in selectedBadTags }
    val selectedTags = if (selectedTab == 0) selectedGoodTags else selectedBadTags
    val fields = if (selectedTab == 0) goodFields else badFields
    val setSelectedTags: (List<String>) -> Unit = if (selectedTab == 0) { { selectedGoodTags = it } } else { { selectedBadTags = it } }
    val setFields: (Map<String, TextFieldValue>) -> Unit = if (selectedTab == 0) { { goodFields = it } } else { { badFields = it } }
    val wheelIdx = if (selectedTab == 0) wheelGoodIdx else wheelBadIdx
    val setWheelIdx: (Int) -> Unit = if (selectedTab == 0) { { wheelGoodIdx = it } } else { { wheelBadIdx = it } }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        // Верхняя панель
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("Отмена") }
            Spacer(Modifier.weight(1f))
            Text("Создание отчёта", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        // Переключатель good/bad
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFFF5F5F5), RoundedCornerShape(16.dp))
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TabButton(
                selected = selectedTab == 0,
                text = "👍 молодец (${selectedGoodTags.size})",
                onClick = { selectedTab = 0 },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            TabButton(
                selected = selectedTab == 1,
                text = "👎 лаботряс (${selectedBadTags.size})",
                onClick = { selectedTab = 1 },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(12.dp))
        // WheelPicker тегов
        if (wheelTags.isNotEmpty()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(40.dp)
                        .horizontalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center
                ) {
                    Row {
                        wheelTags.forEachIndexed { idx, tag ->
                            val selected = idx == wheelIdx
                            Text(
                                text = tag,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { setWheelIdx(idx) }
                            )
                        }
                    }
                }
                IconButton(
                    onClick = {
                        val tag = wheelTags.getOrNull(wheelIdx)
                        if (tag != null) {
                            setSelectedTags(selectedTags + tag)
                            setFields(fields + (tag to TextFieldValue()))
                        }
                    },
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Добавить", tint = Color.White)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        // Горизонтальный слайдер выбранных тегов
        if (selectedTags.isNotEmpty()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                selectedTags.forEach { tag ->
                    ChipTag(
                        text = tag,
                        selected = true,
                        onRemove = {
                            setSelectedTags(selectedTags - tag)
                            setFields(fields - tag)
                        }
                    )
                    Spacer(Modifier.width(8.dp))
                }
            }
        }
        // Поле для добавления кастомного пункта (good/bad)
        var customInput by remember { mutableStateOf(TextFieldValue()) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = customInput,
                onValueChange = { customInput = it },
                placeholder = { Text("Добавить пункт") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = {
                    val text = customInput.text.trim()
                    if (text.isNotEmpty()) {
                        if (selectedTab == 0) {
                            setSelectedTags(selectedGoodTags + text)
                            setFields(goodFields + (text to TextFieldValue(text)))
                        } else {
                            setSelectedTags(selectedBadTags + text)
                            setFields(badFields + (text to TextFieldValue(text)))
                        }
                        customInput = TextFieldValue()
                    }
                },
                modifier = Modifier.size(48.dp),
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFF2196F3))
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить", tint = Color.White)
            }
        }
        Spacer(Modifier.height(8.dp))
        // Карточки выбранных good/bad пунктов с редактированием и удалением
        var editingKey by remember { mutableStateOf<String?>(null) }
        var editingText by remember { mutableStateOf(TextFieldValue()) }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            selectedTags.forEach { tag ->
                AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
                    if (editingKey == tag) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            elevation = CardDefaults.cardElevation(6.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                                OutlinedTextField(
                                    value = editingText,
                                    onValueChange = { editingText = it },
                                    modifier = Modifier.weight(1f),
                                    shape = MaterialTheme.shapes.small
                                )
                                Spacer(Modifier.width(8.dp))
                                IconButton(onClick = {
                                    val newText = editingText.text.trim()
                                    if (newText.isNotBlank()) {
                                        // Обновляем ключ в selectedTags и fields
                                        val newTags = selectedTags.map { if (it == tag) newText else it }
                                        setSelectedTags(newTags)
                                        setFields(fields - tag + (newText to (fields[tag] ?: TextFieldValue(newText)).copy(text = newText)))
                                        editingKey = null
                                    }
                                }, colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFF2196F3))) {
                                    Text("OK", color = Color.White)
                                }
                                IconButton(onClick = { editingKey = null }) {
                                    Text("X", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            elevation = CardDefaults.cardElevation(4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
                                Text(fields[tag]?.text ?: tag, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                                IconButton(onClick = {
                                    editingKey = tag
                                    editingText = TextFieldValue(fields[tag]?.text ?: tag)
                                }, colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFF2196F3))) {
                                    Icon(Icons.Default.Edit, contentDescription = "Редактировать", tint = Color.White)
                                }
                                IconButton(onClick = {
                                    setSelectedTags(selectedTags - tag)
                                    setFields(fields - tag)
                                }, colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        // Кнопки
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { /* сохранить */ },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Сохранить")
            }
            Spacer(Modifier.width(16.dp))
            Button(
                onClick = { /* опубликовать */ },
                modifier = Modifier.weight(1f),
                enabled = false, // пока не реализовано
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB2DFDB)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Опубликовать")
            }
        }
    }
}

@Composable
private fun TabButton(selected: Boolean, text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        colors = if (selected) ButtonDefaults.buttonColors(containerColor = Color(0xFFB2FFB2))
        else ButtonDefaults.buttonColors(containerColor = Color(0xFFF5F5F5)),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Text(text, color = if (selected) Color(0xFF388E3C) else Color.Black, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun ChipTag(text: String, selected: Boolean, onRemove: () -> Unit) {
    Box(
        Modifier
            .background(
                color = if (selected) Color(0xFFB2FFB2) else Color(0xFFE0E0E0),
                shape = RoundedCornerShape(16.dp)
            )
            .border(1.dp, if (selected) Color(0xFF388E3C) else Color.Gray, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text, color = if (selected) Color(0xFF388E3C) else Color.Black)
            IconButton(onClick = onRemove, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Удалить", tint = Color.Gray, modifier = Modifier.size(16.dp))
            }
        }
    }
} 