@file:OptIn(ExperimentalMaterial3Api::class)
package com.katapandroid.lazybones.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.katapandroid.lazybones.data.PlanItem
import com.katapandroid.lazybones.data.Tag
import com.katapandroid.lazybones.data.TagType
import org.koin.androidx.compose.koinViewModel
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import com.katapandroid.lazybones.data.PostRepository
import org.koin.compose.koinInject

@Composable
fun PlanScreen(
    viewModel: PlanViewModel = koinViewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val postRepository = koinInject<PostRepository>()
    var selectedTab by remember { mutableStateOf(0) } // 0 = План, 1 = Теги
    val tabTitles = listOf("План", "Теги")

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
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
            PlanTab(viewModel, postRepository, snackbarHostState, coroutineScope)
        }
        AnimatedVisibility(visible = selectedTab == 1, enter = fadeIn(), exit = fadeOut()) {
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
    coroutineScope: CoroutineScope
) {
    val planItems by viewModel.planItems.collectAsState()
    var input by remember { mutableStateOf(TextFieldValue()) }
    var editingId by remember { mutableStateOf<Long?>(null) }
    var editingText by remember { mutableStateOf(TextFieldValue()) }
    var showDeleteDialog by remember { mutableStateOf<Pair<Boolean, PlanItem?>>(false to null) }

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
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            elevation = CardDefaults.cardElevation(2.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("OK", color = MaterialTheme.colorScheme.onPrimary)
                                }
                                IconButton(
                                    onClick = { editingId = null },
                                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("X", color = MaterialTheme.colorScheme.onError)
                                }
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            elevation = CardDefaults.cardElevation(2.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
                                Text(item.text, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                                IconButton(
                                    onClick = {
                                        editingId = item.id
                                        editingText = TextFieldValue(item.text)
                                    },
                                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Редактировать", tint = MaterialTheme.colorScheme.onPrimary)
                                }
                                IconButton(
                                    onClick = { showDeleteDialog = true to item },
                                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.onError)
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        if (planItems.isNotEmpty()) {
            Button(
                onClick = {
                    coroutineScope.launch {
                        viewModel.saveAsCustomReport(postRepository)
                        snackbarHostState.showSnackbar("Кастомный отчет сформирован")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Text("Сформировать отчет", style = MaterialTheme.typography.titleMedium)
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
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            elevation = CardDefaults.cardElevation(2.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            elevation = CardDefaults.cardElevation(2.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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