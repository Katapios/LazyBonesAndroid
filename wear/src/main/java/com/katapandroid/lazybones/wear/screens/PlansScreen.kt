package com.katapandroid.lazybones.wear.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*

data class PlanItem(
    val id: Long,
    val text: String,
    val date: Long = 0L
)

@Composable
fun PlansScreen(plans: List<PlanItem>) {
    val dateFormat = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault())
    
    // Логируем для отладки
    android.util.Log.d("PlansScreen", "🔍 ====== RENDERING PLANS SCREEN ======")
    android.util.Log.d("PlansScreen", "   Total plans: ${plans.size}")
    android.util.Log.d("PlansScreen", "   Plans details: ${plans.map { "id=${it.id}, text='${it.text.take(20)}...', date=${it.date}" }}")
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Планы",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        if (plans.isEmpty()) {
            Text(
                text = "Нет планов",
                fontSize = 12.sp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
            android.util.Log.d("PlansScreen", "⚠️ Plans list is empty!")
        } else {
            // Группируем по датам
            val groupedPlans = plans.groupBy {
                if (it.date > 0) {
                    try {
                        dateFormat.format(java.util.Date(it.date))
                    } catch (e: Exception) {
                        android.util.Log.e("PlansScreen", "Error formatting date ${it.date}", e)
                        "Ошибка даты"
                    }
                } else {
                    "Без даты"
                }
            }
            
            android.util.Log.d("PlansScreen", "📅 Grouped plans by dates: ${groupedPlans.keys}")
            android.util.Log.d("PlansScreen", "   Groups: ${groupedPlans.map { "${it.key}: ${it.value.size} plans" }}")
            android.util.Log.d("PlansScreen", "   Plans with dates: ${plans.map { "${it.id}: date=${it.date}, text='${it.text.take(15)}...'" }}")
            
            // Сортируем даты в обратном порядке (новые сначала)
            // Для "Без даты" ставим в конец
            val sortedDates = groupedPlans.keys.sortedWith(compareByDescending<String> { 
                if (it == "Без даты") "" else it 
            })
            
            android.util.Log.d("PlansScreen", "📅 Sorted dates: $sortedDates")
            
            if (sortedDates.isEmpty()) {
                android.util.Log.e("PlansScreen", "❌ Sorted dates is empty but plans is not!")
                Text(
                    text = "Ошибка группировки",
                    fontSize = 12.sp,
                    color = MaterialTheme.colors.error
                )
            } else {
                sortedDates.forEach { date ->
                    val dayPlans = groupedPlans[date] ?: emptyList()
                    android.util.Log.d("PlansScreen", "📅 Rendering date: '$date' with ${dayPlans.size} plans")
                    
                    if (dayPlans.isEmpty()) {
                        android.util.Log.w("PlansScreen", "⚠️ Day plans for date '$date' is empty!")
                    } else {
                        // Заголовок даты
                        Text(
                            text = date,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colors.primary,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                        
                        // Планы на эту дату
                        dayPlans.forEachIndexed { index, plan ->
                            android.util.Log.d("PlansScreen", "   Rendering plan $index: id=${plan.id}, text='${plan.text}', date=${plan.date}")
                            
                            if (plan.text.isEmpty()) {
                                android.util.Log.w("PlansScreen", "⚠️ Plan ${plan.id} has empty text!")
                            }
                            
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { }
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = plan.text.ifEmpty { "Пустой план #${plan.id}" },
                                        fontSize = 13.sp,
                                        lineHeight = 16.sp
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
