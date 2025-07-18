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
import org.koin.androidx.compose.getViewModel
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut

@Composable
fun PlanScreen(
    viewModel: PlanViewModel = getViewModel(),
    onSaveReport: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = План, 1 = Теги
    val tabTitles = listOf("План", "Теги")

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TabRow(selectedTabIndex = selectedTab, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
            tabTitles.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(title, style = MaterialTheme.typography.titleMedium) }
                )
            }
        }
        AnimatedVisibility(visible = selectedTab == 0, enter = fadeIn(), exit = fadeOut()) {
            PlanTab(viewModel)
        }
        AnimatedVisibility(visible = selectedTab == 1, enter = fadeIn(), exit = fadeOut()) {
            TagsTab(viewModel)
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onSaveReport,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Text("Сохранить как отчет", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun PlanTab(viewModel: PlanViewModel) {
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
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
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
                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить", tint = MaterialTheme.colorScheme.primary)
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
                                    if (editingText.text.isNotBlank()) {
                                        viewModel.updatePlanItem(item.copy(text = editingText.text))
                                        editingId = null
                                    }
                                }, colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                                    Text("OK", color = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { editingId = null }) {
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
                                Text(item.text, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                                IconButton(onClick = {
                                    editingId = item.id
                                    editingText = TextFieldValue(item.text)
                                }, colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                                    Icon(Icons.Default.Edit, contentDescription = "Редактировать", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { showDeleteDialog = true to item }, colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
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

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        TabRow(selectedTabIndex = if (selectedTagTab == TagType.GOOD) 0 else 1, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
            Tab(selected = selectedTagTab == TagType.GOOD, onClick = { selectedTagTab = TagType.GOOD }, text = { Text("Хорошие", style = MaterialTheme.typography.titleMedium) })
            Tab(selected = selectedTagTab == TagType.BAD, onClick = { selectedTagTab = TagType.BAD }, text = { Text("Плохие", style = MaterialTheme.typography.titleMedium) })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("Добавить тег") },
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.medium,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
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
                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить тег", tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.height(16.dp))
        val tags = if (selectedTagTab == TagType.GOOD) goodTags else badTags
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(tags, key = { it.id }) { tag ->
                AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
                    if (editingId == tag.id) {
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
                                    if (editingText.text.isNotBlank()) {
                                        viewModel.updateTag(tag.copy(text = editingText.text))
                                        editingId = null
                                    }
                                }, colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                                    Text("OK", color = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { editingId = null }) {
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
                                Text(tag.text, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                                IconButton(onClick = {
                                    editingId = tag.id
                                    editingText = TextFieldValue(tag.text)
                                }, colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                                    Icon(Icons.Default.Edit, contentDescription = "Редактировать тег", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { showDeleteDialog = true to tag }, colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Удалить тег", tint = MaterialTheme.colorScheme.error)
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
                TextButton(onClick = {
                    showDeleteDialog.second?.let { viewModel.deleteTag(it) }
                    showDeleteDialog = false to null
                }) { Text("Удалить", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false to null }) { Text("Отмена") }
            }
        )
    }
} 