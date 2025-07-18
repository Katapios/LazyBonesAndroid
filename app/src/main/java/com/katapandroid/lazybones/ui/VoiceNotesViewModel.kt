package com.katapandroid.lazybones.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katapandroid.lazybones.data.VoiceNote
import com.katapandroid.lazybones.data.VoiceNoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class VoiceNotesViewModel(
    private val voiceNoteRepository: VoiceNoteRepository
) : ViewModel() {
    private val _voiceNotes = MutableStateFlow<List<VoiceNote>>(emptyList())
    val voiceNotes: StateFlow<List<VoiceNote>> = _voiceNotes.asStateFlow()

    init {
        voiceNoteRepository.getAllVoiceNotes().onEach { notes ->
            _voiceNotes.value = notes
        }.launchIn(viewModelScope)
    }
} 