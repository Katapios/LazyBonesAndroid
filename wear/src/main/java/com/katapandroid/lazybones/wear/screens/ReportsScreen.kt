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
import com.katapandroid.lazybones.wear.model.ReportItem
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReportsScreen(reports: List<ReportItem>) {
    val dateFormat = SimpleDateFormat("dd.MM", Locale.getDefault())
    
    // Логируем для отладки
    android.util.Log.d("ReportsScreen", "🔍 Rendering ReportsScreen with ${reports.size} reports")
    android.util.Log.d("ReportsScreen", "   Reports: ${reports.map { "id=${it.id}, date=${it.date}, good=${it.goodCount}, bad=${it.badCount}" }}")
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Отчёты",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        if (reports.isEmpty()) {
            Text(
                text = "Нет отчётов",
                fontSize = 12.sp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        } else {
            // Группируем по датам
            val groupedReports = reports.groupBy { 
                dateFormat.format(Date(it.date))
            }
            
            groupedReports.forEach { (date, dayReports) ->
                Text(
                    text = date,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                
                dayReports.forEach { report ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { }
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "✓ ${report.goodCount}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF4CAF50)
                                )
                                if (report.published) {
                                    Text(
                                        text = "Опубликован",
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colors.primary
                                    )
                                }
                                Text(
                                    text = "✗ ${report.badCount}",
                                    fontSize = 11.sp,
                                    color = Color(0xFFF44336)
                                )
                            }
                            
                            // Отображаем хорошие пункты
                            if (report.goodItems.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "Хорошо:",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4CAF50)
                                    )
                                    report.goodItems.forEach { item ->
                                        Text(
                                            text = "  • $item",
                                            fontSize = 10.sp,
                                            color = Color(0xFF4CAF50)
                                        )
                                    }
                                }
                            }
                            
                            // Отображаем плохие пункты
                            if (report.badItems.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "Плохо:",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFF44336)
                                    )
                                    report.badItems.forEach { item ->
                                        Text(
                                            text = "  • $item",
                                            fontSize = 10.sp,
                                            color = Color(0xFFF44336)
                                        )
                                    }
                                }
                            }
                            
                            // Отображаем план
                            if (report.checklist.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "План:",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                                    )
                                    report.checklist.forEach { item ->
                                        Text(
                                            text = "  • $item",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
