package com.katapandroid.lazybones.ui

import com.katapandroid.lazybones.data.PoolStatus
import com.katapandroid.lazybones.data.PlanItem
import java.util.Random

/**
 * Утилита для генерации мотивационных лозунгов
 */
object MotivationalSlogan {
    
    private val random = Random()
    
    /**
     * Общие мотивационные лозунги (когда пул не активен)
     */
    private val restSlogans = listOf(
        "Отдыхай, LABотряс! Не пришло твое время",
        "Время для отдыха и восстановления",
        "Следующий пул скоро начнется",
        "Подготовься к новому дню",
        "Отдых - это тоже часть работы"
    )
    
    /**
     * Мотивационные лозунги когда нет планов
     */
    private val noPlansSlogans = listOf(
        "Не пора ли что-нибудь запланировать, LABотряс?",
        "Планы помогают достигать целей",
        "Что будем делать сегодня?",
        "Время создавать планы",
        "Каждый день - новая возможность"
    )
    
    /**
     * Шаблоны для лозунгов с планами
     */
    private val planTemplates = listOf(
        "Эй, а ты не забыл сделать «%s»?",
        "Как насчет «%s»?",
        "А что там с «%s»?",
        "Не пора ли заняться «%s»?",
        "Помнишь про «%s»?",
        "Время для «%s»!",
        "Сделай «%s» прямо сейчас",
        "«%s» ждет тебя"
    )
    
    /**
     * Общие мотивационные лозунги (когда пул активен, но нет конкретного плана)
     */
    private val activePoolSlogans = listOf(
        "Время действовать!",
        "Каждый момент важен",
        "Сделай сегодня лучше, чем вчера",
        "Твой день - твои правила",
        "Двигайся к цели",
        "Не откладывай на завтра",
        "Время работает на тебя"
    )
    
    /**
     * Получить мотивационный лозунг на основе статуса пула и планов
     */
    fun getSlogan(
        poolStatus: PoolStatus,
        plans: List<PlanItem> = emptyList(),
        goodCount: Int = 0,
        badCount: Int = 0
    ): String {
        return when (poolStatus) {
            PoolStatus.BEFORE_START, PoolStatus.AFTER_END -> {
                restSlogans.random(random)
            }
            PoolStatus.ACTIVE -> {
                when {
                    plans.isNotEmpty() -> {
                        val randomPlan = plans.random(random)
                        planTemplates.random(random).format(randomPlan.text)
                    }
                    goodCount > 0 && badCount == 0 -> {
                        "Отличная работа! Продолжай в том же духе!"
                    }
                    goodCount > badCount -> {
                        "Ты на правильном пути! Так держать!"
                    }
                    badCount > goodCount -> {
                        "Не сдавайся! Каждый день - новый шанс!"
                    }
                    else -> {
                        activePoolSlogans.random(random)
                    }
                }
            }
        }
    }
    
    /**
     * Получить случайный лозунг из списка
     */
    private fun <T> List<T>.random(random: Random): T {
        return this[random.nextInt(this.size)]
    }
}

