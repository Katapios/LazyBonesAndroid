package com.katapandroid.lazybones.wear.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import java.text.SimpleDateFormat
import java.util.*

data class StatsData(
    val reportsCount: Int = 0,
    val plansCount: Int = 0,
    val reportsByStatus: Map<String, Int> = emptyMap(),
    val latestReportDate: Long? = null,
    val latestPlanDate: Long? = null
)

@Composable
fun StatsScreen(
    reports: List<ReportItem>,
    plans: List<PlanItem>,
    reportStatus: String? = null
) {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    
    // Вычисляем статистику
    val stats = StatsData(
        reportsCount = reports.size,
        plansCount = plans.size,
        reportsByStatus = reports.groupBy { 
            if (it.published) "Опубликован" else "Сохранён"
        }.mapValues { it.value.size },
        latestReportDate = reports.maxOfOrNull { it.date },
        latestPlanDate = plans.maxOfOrNull { it.date }
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Статистика",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Количество отчетов
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = { }
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Отчёты",
                    fontSize = 11.sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "${stats.reportsCount}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary,
                    textAlign = TextAlign.Center
                )
                if (stats.latestReportDate != null) {
                    Text(
                        text = "Последний: ${dateFormat.format(Date(stats.latestReportDate))}",
                        fontSize = 9.sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        
        // Количество планов
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = { }
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Планы",
                    fontSize = 11.sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "${stats.plansCount}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50),
                    textAlign = TextAlign.Center
                )
                if (stats.latestPlanDate != null) {
                    Text(
                        text = "Последний: ${dateFormat.format(Date(stats.latestPlanDate))}",
                        fontSize = 9.sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        
        // Статус текущего отчёта
        if (reportStatus != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { }
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Статус отчёта",
                        fontSize = 11.sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = translateStatus(reportStatus),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (reportStatus.uppercase()) {
                            "PUBLISHED" -> Color(0xFF4CAF50)
                            "SAVED" -> Color(0xFF2196F3)
                            "IN_PROGRESS" -> Color(0xFFFF9800)
                            "NOT_FILLED" -> Color(0xFF9E9E9E)
                            else -> MaterialTheme.colors.onSurface
                        },
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        
        // Статистика по статусам отчетов
        if (stats.reportsByStatus.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { }
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "По статусам",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    stats.reportsByStatus.forEach { (status, count) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = status,
                                fontSize = 10.sp,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "$count",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (status) {
                                    "Опубликован" -> Color(0xFF4CAF50)
                                    "Сохранён" -> Color(0xFF2196F3)
                                    else -> MaterialTheme.colors.onSurface
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// Функция для перевода статуса на русский
private fun translateStatus(status: String?): String {
    return when (status?.uppercase()) {
        "PUBLISHED" -> "Опубликован"
        "SAVED" -> "Сохранён"
        "DRAFT" -> "Черновик"
        "IN_PROGRESS" -> "Заполняется"
        "NOT_FILLED" -> "Не заполнен"
        "NONE" -> "Нет отчёта"
        null -> "Нет данных"
        else -> status
    }
}

