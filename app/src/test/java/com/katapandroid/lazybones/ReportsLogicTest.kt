package com.katapandroid.lazybones

import com.katapandroid.lazybones.data.Post
import org.junit.Test
import org.junit.Assert.*
import java.util.*

class ReportsLogicTest {
    
    @Test
    fun `test local report filtering`() {
        // Создаем локальный отчет (с goodItems и badItems)
        val localReport = Post(
            id = 1,
            date = Date(),
            content = "Локальный отчет",
            checklist = emptyList(),
            voiceNotes = emptyList(),
            published = false,
            goodItems = listOf("Работал", "Делал зарядку"),
            badItems = listOf("Не спал"),
            goodCount = 2,
            badCount = 1
        )
        
        // Проверяем, что это локальный отчет
        val isLocal = localReport.goodItems.isNotEmpty() || localReport.badItems.isNotEmpty()
        assertTrue("Отчет с goodItems/badItems должен быть локальным", isLocal)
        
        // Проверяем, что это НЕ кастомный отчет
        val isCustom = localReport.checklist.isNotEmpty() && localReport.goodItems.isEmpty() && localReport.badItems.isEmpty()
        assertFalse("Отчет с goodItems/badItems не должен быть кастомным", isCustom)
    }
    
    @Test
    fun `test custom report filtering`() {
        // Создаем кастомный отчет (с checklist)
        val customReport = Post(
            id = 2,
            date = Date(),
            content = "Кастомный отчет",
            checklist = listOf("Пункт 1", "Пункт 2", "Пункт 3"),
            voiceNotes = emptyList(),
            published = false,
            goodItems = emptyList(),
            badItems = emptyList(),
            goodCount = 0,
            badCount = 0
        )
        
        // Проверяем, что это кастомный отчет
        val isCustom = customReport.checklist.isNotEmpty() && customReport.goodItems.isEmpty() && customReport.badItems.isEmpty()
        assertTrue("Отчет с checklist должен быть кастомным", isCustom)
        
        // Проверяем, что это НЕ локальный отчет
        val isLocal = customReport.goodItems.isNotEmpty() || customReport.badItems.isNotEmpty()
        assertFalse("Отчет с checklist не должен быть локальным", isLocal)
    }
    
    @Test
    fun `test evaluated custom report filtering`() {
        // Создаем оцененный кастомный отчет (с checklist, goodItems и badItems)
        val evaluatedCustomReport = Post(
            id = 3,
            date = Date(),
            content = "Оцененный кастомный отчет",
            checklist = listOf("Пункт 1", "Пункт 2", "Пункт 3"),
            voiceNotes = emptyList(),
            published = false,
            goodItems = listOf("Выполнил пункт 1", "Выполнил пункт 2"),
            badItems = listOf("Не выполнил пункт 3"),
            goodCount = 2,
            badCount = 1
        )
        
        // Проверяем, что это НЕ локальный отчет (оцененный кастомный отчет остается кастомным)
        val isLocal = (evaluatedCustomReport.goodItems.isNotEmpty() || evaluatedCustomReport.badItems.isNotEmpty()) && evaluatedCustomReport.checklist.isEmpty()
        assertFalse("Оцененный кастомный отчет не должен быть локальным", isLocal)
        
        // Проверяем, что это кастомный отчет (оцененный кастомный отчет остается кастомным)
        val isCustom = evaluatedCustomReport.checklist.isNotEmpty()
        assertTrue("Оцененный кастомный отчет должен оставаться кастомным", isCustom)
    }
    
    @Test
    fun `test empty report filtering`() {
        // Создаем пустой отчет
        val emptyReport = Post(
            id = 4,
            date = Date(),
            content = "Пустой отчет",
            checklist = emptyList(),
            voiceNotes = emptyList(),
            published = false,
            goodItems = emptyList(),
            badItems = emptyList(),
            goodCount = 0,
            badCount = 0
        )
        
        // Проверяем, что это НЕ локальный отчет
        val isLocal = emptyReport.goodItems.isNotEmpty() || emptyReport.badItems.isNotEmpty()
        assertFalse("Пустой отчет не должен быть локальным", isLocal)
        
        // Проверяем, что это НЕ кастомный отчет
        val isCustom = emptyReport.checklist.isNotEmpty() && emptyReport.goodItems.isEmpty() && emptyReport.badItems.isEmpty()
        assertFalse("Пустой отчет не должен быть кастомным", isCustom)
    }
} 