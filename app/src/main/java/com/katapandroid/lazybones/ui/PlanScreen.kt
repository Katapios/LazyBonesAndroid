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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(
    viewModel: PlanViewModel = getViewModel(),
    onSaveReport: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = План, 1 = Теги
    val tabTitles = listOf("План", "Теги")

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TabRow(selectedTabIndex = selectedTab) {
            tabTitles.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(title) }
                )
            }
        }
        when (selectedTab) {
            0 -> PlanTab(viewModel)
            1 -> TagsTab(viewModel)
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onSaveReport,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Сохранить как отчет")
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
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    if (input.text.isNotBlank()) {
                        viewModel.addPlanItem(input.text)
                        input = TextFieldValue()
                    }
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить")
            }
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(planItems, key = { it.id }) { item ->
                if (editingId == item.id) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = editingText,
                            onValueChange = { editingText = it },
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            if (editingText.text.isNotBlank()) {
                                viewModel.updatePlanItem(item.copy(text = editingText.text))
                                editingId = null
                            }
                        }) {
                            Text("OK")
                        }
                        IconButton(onClick = { editingId = null }) {
                            Text("X")
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(item.text, modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            editingId = item.id
                            editingText = TextFieldValue(item.text)
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Редактировать")
                        }
                        IconButton(onClick = { showDeleteDialog = true to item }) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить")
                        }
                    }
                }
            }
        }
    }
    if (showDeleteDialog.first) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false to null },
            title = { Text("Удалить пункт?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog.second?.let { viewModel.deletePlanItem(it) }
                    showDeleteDialog = false to null
                }) { Text("Удалить") }
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
        TabRow(selectedTabIndex = if (selectedTagTab == TagType.GOOD) 0 else 1) {
            Tab(selected = selectedTagTab == TagType.GOOD, onClick = { selectedTagTab = TagType.GOOD }, text = { Text("Хорошие") })
            Tab(selected = selectedTagTab == TagType.BAD, onClick = { selectedTagTab = TagType.BAD }, text = { Text("Плохие") })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("Добавить тег") },
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    if (input.text.isNotBlank()) {
                        viewModel.addTag(input.text, selectedTagTab)
                        input = TextFieldValue()
                    }
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить тег")
            }
        }
        Spacer(Modifier.height(8.dp))
        val tags = if (selectedTagTab == TagType.GOOD) goodTags else badTags
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tags, key = { it.id }) { tag ->
                if (editingId == tag.id) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = editingText,
                            onValueChange = { editingText = it },
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            if (editingText.text.isNotBlank()) {
                                viewModel.updateTag(tag.copy(text = editingText.text))
                                editingId = null
                            }
                        }) {
                            Text("OK")
                        }
                        IconButton(onClick = { editingId = null }) {
                            Text("X")
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(tag.text, modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            editingId = tag.id
                            editingText = TextFieldValue(tag.text)
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Редактировать тег")
                        }
                        IconButton(onClick = { showDeleteDialog = true to tag }) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить тег")
                        }
                    }
                }
            }
        }
    }
    if (showDeleteDialog.first) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false to null },
            title = { Text("Удалить тег?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog.second?.let { viewModel.deleteTag(it) }
                    showDeleteDialog = false to null
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false to null }) { Text("Отмена") }
            }
        )
    }
} 