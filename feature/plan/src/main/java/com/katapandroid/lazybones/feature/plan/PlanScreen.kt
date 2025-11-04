@file:OptIn(ExperimentalMaterial3Api::class)
package com.katapandroid.lazybones.feature.plan

import android.util.Log
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.katapandroid.lazybones.core.domain.model.PlanItem
import com.katapandroid.lazybones.core.domain.model.Tag
import org.koin.androidx.compose.koinViewModel
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import com.katapandroid.lazybones.core.domain.repository.PostRepository
import com.katapandroid.lazybones.core.domain.model.Post
import com.katapandroid.lazybones.ui.ReportFormViewModel
import org.koin.androidx.compose.get
import com.katapandroid.lazybones.ui.MainViewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.text.font.FontWeight
import com.katapandroid.lazybones.core.domain.model.TagType
import com.katapandroid.lazybones.feature.widget.LazyBonesWidgetProvider

@Composable
fun PlanScreen(
    viewModel: PlanViewModel = koinViewModel(),
    initialTab: Int = 0
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val postRepository = get<PostRepository>()
    val mainViewModel = koinViewModel<MainViewModel>()
    var selectedTab by remember { mutableStateOf(initialTab) } // 0 = План, 1 = Отчет, 2 = Теги
    val tabTitles = listOf("План", "Отчет", "Теги")

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(WindowInsets.systemBars.asPaddingValues())
            .padding(bottom = 80.dp)
    ) {
        TabRow(selectedTabIndex = selectedTab, containerColor = MaterialTheme.colorScheme.surface) {
            tabTitles.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(title, style = MaterialTheme.typography.titleMedium) }
                )
            }
        }
        AnimatedVisibility(visible = selectedTab == 0, enter = fadeIn(), exit = fadeOut()) {
            PlanTab(viewModel, postRepository, snackbarHostState, coroutineScope, mainViewModel)
        }
        AnimatedVisibility(visible = selectedTab == 1, enter = fadeIn(), exit = fadeOut()) {
            ReportFormTab(
                viewModel = koinViewModel<ReportFormViewModel>(),
                snackbarHostState = snackbarHostState,
                coroutineScope = coroutineScope,
                mainViewModel = mainViewModel
            )
        }
        AnimatedVisibility(visible = selectedTab == 2, enter = fadeIn(), exit = fadeOut()) {
            TagsTab(viewModel)
        }
        SnackbarHost(hostState = snackbarHostState)
    }
}

@Composable
private fun PlanTab(
    viewModel: PlanViewModel,
    postRepository: PostRepository,
    snackbarHostState: SnackbarHostState,
    coroutineScope: CoroutineScope,
    mainViewModel: MainViewModel
) {
    val context = LocalContext.current
    val planItems by viewModel.planItems.collectAsState()
    var input by remember { mutableStateOf(TextFieldValue()) }
    var editingId by remember { mutableStateOf<Long?>(null) }
    var editingText by remember { mutableStateOf(TextFieldValue()) }
    var showDeleteDialog by remember { mutableStateOf<Pair<Boolean, PlanItem?>>(false to null) }
    var completedItems by remember { mutableStateOf<Set<Long>>(emptySet()) }
    
    // Автоматически обновляем виджет при изменении пунктов плана
    LaunchedEffect(planItems.size) {
        LazyBonesWidgetProvider.updateAllWidgets(context)
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("Добавить пункт плана") },
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (input.text.isNotBlank()) {
                        viewModel.addPlanItem(input.text)
                        input = TextFieldValue()
                    }
                },
                modifier = Modifier.size(48.dp),
                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(planItems, key = { it.id }) { item ->
                AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
                    if (editingId == item.id) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
                                OutlinedTextField(
                                    value = editingText,
                                    onValueChange = { editingText = it },
                                    modifier = Modifier.weight(1f),
                                    shape = MaterialTheme.shapes.small,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                    )
                                )
                                Spacer(Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        if (editingText.text.isNotBlank()) {
                                            viewModel.updatePlanItem(item.copy(text = editingText.text))
                                            editingId = null
                                        }
                                    },
                                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                ) {
                                    Text("OK", color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                                IconButton(
                                    onClick = { editingId = null },
                                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                                ) {
                                    Text("X", color = MaterialTheme.colorScheme.onErrorContainer)
                                }
                            }
                        }
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = if (completedItems.contains(item.id)) 
                                MaterialTheme.colorScheme.primaryContainer 
                            else 
                                MaterialTheme.colorScheme.surface
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
                                IconButton(
                                    onClick = {
                                        completedItems = if (completedItems.contains(item.id)) {
                                            completedItems - item.id
                                        } else {
                                            completedItems + item.id
                                        }
                                    },
                                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Transparent)
                                ) {
                                    Icon(
                                        if (completedItems.contains(item.id)) Icons.Default.Check else Icons.Default.Close,
                                        contentDescription = if (completedItems.contains(item.id)) "Выполнено" else "Не выполнено",
                                        tint = if (completedItems.contains(item.id)) 
                                            MaterialTheme.colorScheme.primary 
                                        else 
                                            MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    item.text, 
                                    modifier = Modifier.weight(1f), 
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (completedItems.contains(item.id)) 
                                        MaterialTheme.colorScheme.onPrimaryContainer 
                                    else 
                                        MaterialTheme.colorScheme.onSurface
                                )
                                IconButton(
                                    onClick = {
                                        editingId = item.id
                                        editingText = TextFieldValue(item.text)
                                    },
                                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Transparent)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Редактировать", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(
                                    onClick = { showDeleteDialog = true to item },
                                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Transparent)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        if (planItems.isNotEmpty()) {
            val canCreatePlan = mainViewModel.canCreatePlan.collectAsState().value
            Button(
                onClick = {
                    if (!canCreatePlan) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Создание плана заблокировано. Используйте настройки для разблокировки.")
                        }
                        return@Button
                    }
                    coroutineScope.launch {
                        try {
                            // Создаем копию списка перед операциями, чтобы избежать проблем с изменением Flow
                            val itemsList = planItems.toList()
                            val checklist = itemsList.map { it.text }
                            // Сохраняем план БЕЗ оценки (только checklist, без goodItems/badItems)
                            // Оценку можно будет сделать позже в разделе "Отчеты"
                            val post = Post(
                                date = java.util.Date(),
                                content = "План на день",
                                checklist = checklist,
                                voiceNotes = listOf(),
                                published = false,
                                isDraft = false,
                                goodItems = emptyList(), // Оценка будет сделана позже
                                badItems = emptyList(),
                                goodCount = 0,
                                badCount = 0
                            )
                            postRepository.insert(post)
                            // Обновляем виджет
                            LazyBonesWidgetProvider.updateAllWidgets(context)
                            snackbarHostState.showSnackbar("План сохранен. Оцените его позже в разделе \"Отчеты\"")
                        } catch (e: Exception) {
                            Log.e("PlanScreen", "Error saving plan", e)
                            snackbarHostState.showSnackbar("Ошибка при сохранении плана")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (canCreatePlan) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.errorContainer
                ),
                enabled = canCreatePlan
            ) {
                Text(
                    if (canCreatePlan) "Сохранить план" else "План заблокирован",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
    if (showDeleteDialog.first) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false to null },
            title = { Text("Удалить пункт?", style = MaterialTheme.typography.titleMedium) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog.second?.let { viewModel.deletePlanItem(it) }
                    showDeleteDialog = false to null
                }) { Text("Удалить", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false to null }) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun TagsTab(viewModel: PlanViewModel) {
    var selectedTagTab by remember { mutableStateOf(TagType.GOOD) }
    val goodTags by viewModel.goodTags.collectAsState()
    val badTags by viewModel.badTags.collectAsState()
    var input by remember { mutableStateOf(TextFieldValue()) }
    var editingId by remember { mutableStateOf<Long?>(null) }
    var editingText by remember { mutableStateOf(TextFieldValue()) }
    var showDeleteDialog by remember { mutableStateOf<Pair<Boolean, Tag?>>(false to null) }

    Column(Modifier.fillMaxSize()) {
        // Заголовок с переключателем типов тегов
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { selectedTagTab = TagType.GOOD },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedTagTab == TagType.GOOD) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    "Хорошие теги",
                    color = if (selectedTagTab == TagType.GOOD) 
                        MaterialTheme.colorScheme.onPrimary 
                    else 
                        MaterialTheme.colorScheme.onSurface
                )
            }
            Button(
                onClick = { selectedTagTab = TagType.BAD },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedTagTab == TagType.BAD) 
                        MaterialTheme.colorScheme.error 
                    else 
                        MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    "Плохие теги",
                    color = if (selectedTagTab == TagType.BAD) 
                        MaterialTheme.colorScheme.onError 
                    else 
                        MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Поле ввода для добавления тега
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("Добавить ${if (selectedTagTab == TagType.GOOD) "хороший" else "плохой"} тег") },
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = if (selectedTagTab == TagType.GOOD) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.error,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (input.text.isNotBlank()) {
                        viewModel.addTag(input.text, selectedTagTab)
                        input = TextFieldValue()
                    }
                },
                modifier = Modifier.size(48.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (selectedTagTab == TagType.GOOD) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    Icons.Default.Add, 
                    contentDescription = "Добавить тег", 
                    tint = if (selectedTagTab == TagType.GOOD) 
                        MaterialTheme.colorScheme.onPrimary 
                    else 
                        MaterialTheme.colorScheme.onError
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Список тегов
        val tags = if (selectedTagTab == TagType.GOOD) goodTags else badTags
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tags, key = { it.id }) { tag ->
                AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
                    if (editingId == tag.id) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically, 
                                modifier = Modifier.padding(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = editingText,
                                    onValueChange = { editingText = it },
                                    modifier = Modifier.weight(1f),
                                    shape = MaterialTheme.shapes.small,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                        focusedBorderColor = if (selectedTagTab == TagType.GOOD) 
                                            MaterialTheme.colorScheme.primary 
                                        else 
                                            MaterialTheme.colorScheme.error,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                    )
                                )
                                Spacer(Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        if (editingText.text.isNotBlank()) {
                                            viewModel.updateTag(tag.copy(text = editingText.text))
                                            editingId = null
                                        }
                                    },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text("OK", color = MaterialTheme.colorScheme.onPrimary)
                                }
                                IconButton(
                                    onClick = { editingId = null },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text("X", color = MaterialTheme.colorScheme.onError)
                                }
                            }
                        }
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically, 
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    tag.text, 
                                    modifier = Modifier.weight(1f), 
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (selectedTagTab == TagType.GOOD) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.error
                                )
                                IconButton(
                                    onClick = {
                                        editingId = tag.id
                                        editingText = TextFieldValue(tag.text)
                                    },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.Edit, 
                                        contentDescription = "Редактировать тег", 
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                                IconButton(
                                    onClick = { showDeleteDialog = true to tag },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.Delete, 
                                        contentDescription = "Удалить тег", 
                                        tint = MaterialTheme.colorScheme.onError
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog.first) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false to null },
            title = { Text("Удалить тег?", style = MaterialTheme.typography.titleMedium) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog.second?.let { viewModel.deleteTag(it) }
                        showDeleteDialog = false to null
                    }
                ) { 
                    Text("Удалить", color = MaterialTheme.colorScheme.error) 
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false to null }
                ) { 
                    Text("Отмена") 
                }
            }
        )
    }
}

@Composable
private fun ReportFormTab(
    viewModel: ReportFormViewModel,
    snackbarHostState: SnackbarHostState,
    coroutineScope: CoroutineScope,
    mainViewModel: MainViewModel
) {
    val context = LocalContext.current
    
    // Загружаем отчет за сегодня при каждом входе на экран
    LaunchedEffect(Unit) {
        viewModel.loadTodayReportIfEmpty()
    }
    
    // Скопируем логику из ReportFormScreen, но без верхней панели и без onBack
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
        // Переключатель good/bad (без верхней панели с кнопкой назад)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TextButton(
                    onClick = { selectedTab = 0 },
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = if (selectedTab == 0) 
                            MaterialTheme.colorScheme.primaryContainer 
                        else 
                            Color.Transparent
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("👍", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Молодец",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 0) 
                                MaterialTheme.colorScheme.onPrimaryContainer 
                            else 
                                MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "(${selectedGoodTags.size})",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (selectedTab == 0) 
                                MaterialTheme.colorScheme.onPrimaryContainer 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                TextButton(
                    onClick = { selectedTab = 1 },
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = if (selectedTab == 1) 
                            MaterialTheme.colorScheme.errorContainer 
                        else 
                            Color.Transparent
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("👎", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Лаботряс",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 1) 
                                MaterialTheme.colorScheme.onErrorContainer 
                            else 
                                MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "(${selectedBadTags.size})",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (selectedTab == 1) 
                                MaterialTheme.colorScheme.onErrorContainer 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        // Остальной контент из ReportFormScreen (wheel picker, поля ввода, список выбранных пунктов, кнопки)
        // Для упрощения, используем тот же код, что и в ReportFormScreen, начиная со строки 204
        // Но нужно адаптировать его для вкладки (без padding для bottom tab bar, без onBack)
        
        // Пока добавим упрощенную версию - просто вызовем ReportFormScreen без верхней панели
        // Но лучше создать отдельную функцию, которая будет содержать основную логику
        
        // Временно используем простую версию, скопировав код из ReportFormScreen
        Spacer(Modifier.height(12.dp))
        // WheelPicker тегов - аналогично ReportFormScreen
        if (wheelTags.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface
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
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            wheelTags.forEachIndexed { idx, tag ->
                                val selected = idx == wheelIdx
                                Surface(
                                    modifier = Modifier.clickable { setWheelIdx(idx) },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (selected) 
                                        if (selectedTab == 0) 
                                            MaterialTheme.colorScheme.primaryContainer
                                        else 
                                            MaterialTheme.colorScheme.errorContainer
                                    else 
                                        Color.Transparent
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
                    IconButton(
                        onClick = {
                            val tag = wheelTags.getOrNull(wheelIdx)
                            if (tag != null) {
                                val newTags = selectedTags + tag
                                setSelectedTags(newTags)
                                setFields(fields + (tag to TextFieldValue(tag)))
                                
                                // Автоматически сохраняем черновик отчета при добавлении пункта
                                val newGoodTags = if (selectedTab == 0) newTags else selectedGoodTags
                                val newBadTags = if (selectedTab == 1) newTags else selectedBadTags
                                coroutineScope.launch {
                                    viewModel.saveDraftReport(newGoodTags, newBadTags)
                                    LazyBonesWidgetProvider.updateAllWidgets(context)
                                }
                            }
                        },
                        modifier = Modifier.size(48.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (selectedTab == 0) 
                                MaterialTheme.colorScheme.primaryContainer 
                            else 
                                MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Icon(
                            Icons.Default.Add, 
                            contentDescription = "Добавить тег",
                            tint = if (selectedTab == 0) 
                                MaterialTheme.colorScheme.onPrimaryContainer 
                            else 
                                MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        // Поле для добавления кастомного пункта (good/bad)
        var customInput by remember { mutableStateOf(TextFieldValue()) }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
            ) {
                OutlinedTextField(
                    value = customInput,
                    onValueChange = { 
                        customInput = it
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
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge
                )
                Spacer(Modifier.width(12.dp))
                IconButton(
                    onClick = {
                        val text = customInput.text.trim()
                        if (text.isNotEmpty()) {
                            val newGoodTags = if (selectedTab == 0) selectedGoodTags + text else selectedGoodTags
                            val newBadTags = if (selectedTab == 1) selectedBadTags + text else selectedBadTags
                            
                            // Обновляем локальное состояние
                            if (selectedTab == 0) {
                                setSelectedTags(selectedGoodTags + text)
                                setFields(goodFields + (text to TextFieldValue(text)))
                            } else {
                                setSelectedTags(selectedBadTags + text)
                                setFields(badFields + (text to TextFieldValue(text)))
                            }
                            customInput = TextFieldValue()
                            showSaveTagBubble = false
                            
                            // Автоматически сохраняем черновик отчета при добавлении пункта
                            coroutineScope.launch {
                                viewModel.saveDraftReport(newGoodTags, newBadTags)
                                LazyBonesWidgetProvider.updateAllWidgets(context)
                            }
                        }
                    },
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (selectedTab == 0) 
                            MaterialTheme.colorScheme.primaryContainer 
                        else 
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Icon(
                        Icons.Default.Add, 
                        contentDescription = "Добавить пункт",
                        tint = if (selectedTab == 0) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else 
                            MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
        
        // Бабл "Сохранить тег"
        if (showSaveTagBubble && lastInputText.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(8.dp),
                color = if (selectedTab == 0) 
                    MaterialTheme.colorScheme.primaryContainer 
                else 
                    MaterialTheme.colorScheme.errorContainer
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
                                    val existingTags = if (selectedTab == 0) allGoodTags else allBadTags
                                    if (lastInputText !in existingTags) {
                                        viewModel.addTag(lastInputText, if (selectedTab == 0) TagType.GOOD else TagType.BAD)
                                    }
                                } catch (e: Exception) {
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
        // Карточки выбранных good/bad пунктов
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
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface
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
                                                val newTags = selectedTags.map { if (it == tag) newText else it }
                                                setSelectedTags(newTags)
                                                setFields(fields - tag + (newText to (fields[tag] ?: TextFieldValue(newText)).copy(text = newText)))
                                                editingKey = null
                                                
                                                // Обновляем черновик отчета при редактировании пункта
                                                val newGoodTags = if (selectedTab == 0) newTags else selectedGoodTags
                                                val newBadTags = if (selectedTab == 1) newTags else selectedBadTags
                                                coroutineScope.launch {
                                                    viewModel.saveDraftReport(newGoodTags, newBadTags)
                                                    LazyBonesWidgetProvider.updateAllWidgets(context)
                                                }
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
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface
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
                                        val newTags = selectedTags - tag
                                        setSelectedTags(newTags)
                                        setFields(fields - tag)
                                        
                                        // Обновляем черновик отчета при удалении пункта
                                        val newGoodTags = if (selectedTab == 0) newTags else selectedGoodTags
                                        val newBadTags = if (selectedTab == 1) newTags else selectedBadTags
                                        coroutineScope.launch {
                                            viewModel.saveDraftReport(newGoodTags, newBadTags)
                                            LazyBonesWidgetProvider.updateAllWidgets(context)
                                        }
                                    },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = Color.Transparent
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
        // Кнопка сохранения отчета
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                val canCreateReport = mainViewModel.canCreateReport.collectAsState().value
                Button(
                    onClick = {
                        if (!canCreateReport) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Создание отчета заблокировано. Используйте настройки для разблокировки.")
                            }
                            return@Button
                        }
                        // Сохраняем отчёт за сегодня с накопленными good/bad пунктами
                        // Пункты остаются на экране для дальнейшего редактирования
                        coroutineScope.launch {
                            viewModel.saveReport(
                                goodItems = selectedGoodTags,
                                badItems = selectedBadTags,
                                onSaved = {
                                    // Обновляем виджет, но НЕ очищаем пункты
                                    LazyBonesWidgetProvider.updateAllWidgets(context)
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Отчет сохранен")
                                    }
                                }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (canCreateReport) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(8.dp),
                    enabled = canCreateReport
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
                            if (canCreateReport) "Сохранить отчёт" else "Отчет заблокирован", 
                            color = if (canCreateReport) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
} 