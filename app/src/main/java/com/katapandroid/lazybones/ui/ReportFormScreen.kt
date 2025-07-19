package com.katapandroid.lazybones.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.katapandroid.lazybones.data.TagType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import org.koin.androidx.compose.getViewModel
import org.koin.androidx.compose.koinViewModel
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.AnimatedVisibility

@Composable
fun ReportFormScreen(viewModel: ReportFormViewModel = koinViewModel(), onBack: () -> Unit = {}) {
    val goodTags by viewModel.goodTags.collectAsState()
    val badTags by viewModel.badTags.collectAsState()
    val selectedGoodTags by viewModel.selectedGoodTags.collectAsState()
    val selectedBadTags by viewModel.selectedBadTags.collectAsState()
    val goodFields by viewModel.goodFields.collectAsState()
    val badFields by viewModel.badFields.collectAsState()

    // Состояния good/bad
    var selectedTab by remember { mutableStateOf(0) } // 0 = good, 1 = bad
    var wheelGoodIdx by remember { mutableStateOf(0) }
    var wheelBadIdx by remember { mutableStateOf(0) }
    
    // Состояние для бабла "Сохранить тег"
    var lastInputText by remember { mutableStateOf("") }
    var showSaveTagBubble by remember { mutableStateOf(false) }

    val allGoodTags = goodTags.map { it.text }
    val allBadTags = badTags.map { it.text }

    val wheelTags = if (selectedTab == 0) allGoodTags.filter { it !in selectedGoodTags } else allBadTags.filter { it !in selectedBadTags }
    val selectedTags = if (selectedTab == 0) selectedGoodTags else selectedBadTags
    val fields = if (selectedTab == 0) goodFields else badFields
    val setSelectedTags: (List<String>) -> Unit = if (selectedTab == 0) { { viewModel.setSelectedGoodTags(it) } } else { { viewModel.setSelectedBadTags(it) } }
    val setFields: (Map<String, TextFieldValue>) -> Unit = if (selectedTab == 0) { { viewModel.setGoodFields(it) } } else { { viewModel.setBadFields(it) } }
    val wheelIdx = if (selectedTab == 0) wheelGoodIdx else wheelBadIdx
    val setWheelIdx: (Int) -> Unit = if (selectedTab == 0) { { wheelGoodIdx = it } } else { { wheelBadIdx = it } }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Верхняя панель с градиентным фоном
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(), 
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    )
                ) { 
                    Icon(
                        Icons.Default.ArrowBack, 
                        contentDescription = "Назад",
                        tint = MaterialTheme.colorScheme.onSurface
                    ) 
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "Создание отчёта", 
                    style = MaterialTheme.typography.headlineSmall, 
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.weight(1f))
                // Пустое место для баланса
                Spacer(Modifier.width(48.dp))
            }
        }
        // Переключатель good/bad с улучшенным дизайном
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = { selectedTab = 0 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedTab == 0) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = if (selectedTab == 0) 4.dp else 0.dp
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "👍",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Молодец",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 0) 
                                MaterialTheme.colorScheme.onPrimary 
                            else 
                                MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "(${selectedGoodTags.size})",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (selectedTab == 0) 
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            else 
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                Button(
                    onClick = { selectedTab = 1 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedTab == 1) 
                            MaterialTheme.colorScheme.error 
                        else 
                            MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = if (selectedTab == 1) 4.dp else 0.dp
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "👎",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Лаботряс",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 1) 
                                MaterialTheme.colorScheme.onError 
                            else 
                                MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "(${selectedBadTags.size})",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (selectedTab == 1) 
                                MaterialTheme.colorScheme.onError.copy(alpha = 0.8f)
                            else 
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        // WheelPicker тегов с улучшенным дизайном
        if (wheelTags.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .weight(1f)
                            .height(48.dp)
                            .horizontalScroll(rememberScrollState()),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            wheelTags.forEachIndexed { idx, tag ->
                                val selected = idx == wheelIdx
                                Card(
                                    modifier = Modifier
                                        .clickable { setWheelIdx(idx) },
                                    shape = RoundedCornerShape(8.dp),
                                    elevation = CardDefaults.cardElevation(
                                        defaultElevation = if (selected) 4.dp else 1.dp
                                    ),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (selected) 
                                            if (selectedTab == 0) 
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                            else 
                                                MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                                        else 
                                            MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Text(
                                        text = tag,
                                        color = if (selected) 
                                            if (selectedTab == 0) 
                                                MaterialTheme.colorScheme.primary 
                                            else 
                                                MaterialTheme.colorScheme.error
                                        else 
                                            MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    FloatingActionButton(
                        onClick = {
                            val tag = wheelTags.getOrNull(wheelIdx)
                            if (tag != null) {
                                setSelectedTags(selectedTags + tag)
                                setFields(fields + (tag to TextFieldValue(tag)))
                            }
                        },
                        modifier = Modifier.size(48.dp),
                        containerColor = if (selectedTab == 0) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.error,
                        contentColor = if (selectedTab == 0) 
                            MaterialTheme.colorScheme.onPrimary 
                        else 
                            MaterialTheme.colorScheme.onError
                    ) {
                        Icon(
                            Icons.Default.Add, 
                            contentDescription = "Добавить тег"
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        // Поле для добавления кастомного пункта (good/bad)
        var customInput by remember { mutableStateOf(TextFieldValue()) }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(2.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
            ) {
                OutlinedTextField(
                    value = customInput,
                    onValueChange = { 
                        customInput = it
                        // Показываем бабл только если есть текст и он не пустой
                        if (it.text.trim().isNotEmpty()) {
                            lastInputText = it.text.trim()
                            showSaveTagBubble = true
                        } else {
                            showSaveTagBubble = false
                        }
                    },
                    placeholder = { 
                        Text(
                            "Добавить пункт",
                            style = MaterialTheme.typography.bodyMedium
                        ) 
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = if (selectedTab == 0) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.error,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge
                )
                Spacer(Modifier.width(12.dp))
                FloatingActionButton(
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
                            showSaveTagBubble = false
                        }
                    },
                    modifier = Modifier.size(48.dp),
                    containerColor = if (selectedTab == 0) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.error,
                    contentColor = if (selectedTab == 0) 
                        MaterialTheme.colorScheme.onPrimary 
                    else 
                        MaterialTheme.colorScheme.onError
                ) {
                    Icon(
                        Icons.Default.Add, 
                        contentDescription = "Добавить пункт"
                    )
                }
            }
        }
        
        // Бабл "Сохранить тег" с улучшенным дизайном
        if (showSaveTagBubble && lastInputText.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedTab == 0) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (selectedTab == 0) Icons.Default.Add else Icons.Default.Add,
                            contentDescription = null,
                            tint = if (selectedTab == 0) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Сохранить тег?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == 0) 
                                MaterialTheme.colorScheme.onPrimaryContainer 
                            else 
                                MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "\"$lastInputText\"",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (selectedTab == 0) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { showSaveTagBubble = false }
                        ) {
                            Text(
                                "Отмена",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                try {
                                    // Проверяем, что тег не существует
                                    val existingTags = if (selectedTab == 0) allGoodTags else allBadTags
                                    if (lastInputText !in existingTags) {
                                        // Сохраняем тег через ViewModel
                                        viewModel.addTag(lastInputText, if (selectedTab == 0) TagType.GOOD else TagType.BAD)
                                        println("Saving tag: $lastInputText")
                                    } else {
                                        println("Tag already exists: $lastInputText")
                                    }
                                } catch (e: Exception) {
                                    println("Error in save tag button: ${e.message}")
                                    e.printStackTrace()
                                }
                                showSaveTagBubble = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedTab == 0) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.error
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "Сохранить",
                                color = if (selectedTab == 0) 
                                    MaterialTheme.colorScheme.onPrimary 
                                else 
                                    MaterialTheme.colorScheme.onError
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        // Карточки выбранных good/bad пунктов с улучшенным дизайном
        var editingKey by remember { mutableStateOf<String?>(null) }
        var editingText by remember { mutableStateOf(TextFieldValue()) }
        
        if (selectedTags.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Выбранные пункты (${selectedTags.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(8.dp))
        }
        
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(selectedTags) { tag ->
                AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
                    if (editingKey == tag) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                OutlinedTextField(
                                    value = editingText,
                                    onValueChange = { editingText = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                        focusedBorderColor = if (selectedTab == 0) 
                                            MaterialTheme.colorScheme.primary 
                                        else 
                                            MaterialTheme.colorScheme.error,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                    ),
                                    textStyle = MaterialTheme.typography.bodyLarge
                                )
                                Spacer(Modifier.height(12.dp))
                                Row(
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = { editingKey = null }
                                    ) {
                                        Text(
                                            "Отмена",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            val newText = editingText.text.trim()
                                            if (newText.isNotBlank()) {
                                                // Обновляем ключ в selectedTags и fields
                                                val newTags = selectedTags.map { if (it == tag) newText else it }
                                                setSelectedTags(newTags)
                                                setFields(fields - tag + (newText to (fields[tag] ?: TextFieldValue(newText)).copy(text = newText)))
                                                editingKey = null
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (selectedTab == 0) 
                                                MaterialTheme.colorScheme.primary 
                                            else 
                                                MaterialTheme.colorScheme.error
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            "Сохранить",
                                            color = if (selectedTab == 0) 
                                                MaterialTheme.colorScheme.onPrimary 
                                            else 
                                                MaterialTheme.colorScheme.onError
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(2.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically, 
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(
                                    if (selectedTab == 0) Icons.Default.Add else Icons.Default.Add,
                                    contentDescription = null,
                                    tint = if (selectedTab == 0) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    fields[tag]?.text ?: tag, 
                                    modifier = Modifier.weight(1f), 
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        editingKey = tag
                                        editingText = TextFieldValue(fields[tag]?.text ?: tag)
                                    },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.Edit, 
                                        contentDescription = "Редактировать", 
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        setSelectedTags(selectedTags - tag)
                                        setFields(fields - tag)
                                    },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.Delete, 
                                        contentDescription = "Удалить", 
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        // Кнопки с улучшенным дизайном
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    "Действия",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            // Сохраняем отчёт с накопленными good/bad пунктами
                            viewModel.saveReport(
                                goodItems = selectedGoodTags,
                                badItems = selectedBadTags,
                                onSaved = onBack
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Add, 
                                contentDescription = null, 
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Сохранить", 
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Button(
                        onClick = { /* опубликовать */ },
                        modifier = Modifier.weight(1f),
                        enabled = false, // пока не реализовано
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send, 
                                contentDescription = null, 
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Опубликовать", 
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
    }
}



@Composable
private fun ChipTag(text: String, onRemove: () -> Unit) {
    Card(
        modifier = Modifier.padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text, 
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium
            )
            IconButton(
                onClick = onRemove, 
                modifier = Modifier.size(20.dp),
                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(
                    Icons.Default.Close, 
                    contentDescription = "Удалить", 
                    tint = MaterialTheme.colorScheme.onError, 
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
} 