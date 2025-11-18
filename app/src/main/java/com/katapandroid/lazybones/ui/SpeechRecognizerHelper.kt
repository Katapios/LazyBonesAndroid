package com.katapandroid.lazybones.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Утилита для распознавания речи
 */
@Composable
fun rememberSpeechRecognizer(
    onResult: (String) -> Unit,
    onError: ((String) -> Unit)? = null
): SpeechRecognizerState {
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }
    var hasPermission by remember { mutableStateOf(false) }
    
    // Проверяем разрешение
    LaunchedEffect(Unit) {
        hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    // Запрос разрешения
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (!isGranted && onError != null) {
            onError("Разрешение на запись аудио не предоставлено")
        }
    }
    
    // Результат распознавания речи
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val results = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = results?.firstOrNull() ?: ""
            if (spokenText.isNotEmpty()) {
                onResult(spokenText)
            } else if (onError != null) {
                onError("Не удалось распознать речь")
            }
        } else if (onError != null) {
            onError("Распознавание речи отменено")
        }
    }
    
    var currentText by remember { mutableStateOf("") }
    
    return remember {
        object : SpeechRecognizerState {
            override fun startListening(currentTextFieldText: String) {
                if (!hasPermission) {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    return
                }
                
                // Если поле уже заполнено и мы нажимаем снова - очищаем поле
                if (currentTextFieldText.isNotEmpty() && !isListening) {
                    onResult("")
                    return
                }
                
                if (isListening) {
                    // Если уже слушаем, просто отменяем
                    isListening = false
                    return
                }
                
                if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                    onError?.invoke("Распознавание речи недоступно на этом устройстве")
                    return
                }
                
                currentText = currentTextFieldText
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Говорите...")
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }
                
                isListening = true
                speechLauncher.launch(intent)
            }
            
            override val isActive: Boolean
                get() = isListening
        }
    }
}

interface SpeechRecognizerState {
    fun startListening(currentTextFieldText: String = "")
    val isActive: Boolean
}

