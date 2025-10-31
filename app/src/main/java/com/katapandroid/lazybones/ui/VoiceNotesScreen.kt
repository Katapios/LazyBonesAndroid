package com.katapandroid.lazybones.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.katapandroid.lazybones.data.VoiceNote
import org.koin.androidx.compose.getViewModel
import java.util.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceNotesScreen(
    viewModel: VoiceNotesViewModel = getViewModel(),
    onRecord: () -> Unit = {},
    onPlay: (VoiceNote) -> Unit = {},
    onDelete: (VoiceNote) -> Unit = {}
) {
    val notes = viewModel.voiceNotes.collectAsState().value
    val coroutineScope = rememberCoroutineScope()
    Scaffold(
        topBar = { TopAppBar(title = { Text("Голосовые заметки") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onRecord) {
                Text("Rec")
            }
        }
    ) { padding ->
        if (notes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Нет голосовых заметок")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(notes) { note ->
                    VoiceNoteCard(
                        note,
                        onPlay = { onPlay(note) },
                        onDelete = {
                            coroutineScope.launch {
                                viewModel.deleteVoiceNote(note)
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceNoteCard(note: VoiceNote, onPlay: () -> Unit = {}, onDelete: () -> Unit = {}) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Файл: ${note.filePath}", style = MaterialTheme.typography.bodyMedium)
                Text("Длительность: ${note.duration / 1000}s", style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onPlay) { Icon(Icons.Default.PlayArrow, contentDescription = "Play") }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
            }
        }
    }
}

@Preview(showBackground = true)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceNotesScreenPreview() {
    val mockNotes = listOf(
        VoiceNote(1, "/audio/note1.m4a", Date(), 12000),
        VoiceNote(2, "/audio/note2.m4a", Date(), 8000)
    )
    MaterialTheme {
        VoiceNotesScreenMock(notes = mockNotes)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceNotesScreenMock(notes: List<VoiceNote>) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Голосовые заметки") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = {}) {
                Text("Rec")
            }
        }
    ) { padding ->
        if (notes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Нет голосовых заметок")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(notes) { note ->
                    VoiceNoteCard(note)
                }
            }
        }
    }
} 