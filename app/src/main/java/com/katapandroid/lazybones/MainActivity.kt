package com.katapandroid.lazybones

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.katapandroid.lazybones.ui.MainViewModel
import com.katapandroid.lazybones.ui.PlanScreen
// ReportStatus теперь в MainViewModel
import com.katapandroid.lazybones.ui.ReportsScreen
import com.katapandroid.lazybones.ui.SettingsScreen
import com.katapandroid.lazybones.ui.theme.LazyBonesTheme
import org.koin.androidx.compose.getViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.InteractionSource

sealed class BottomNavItem(val route: String, val label: String, val icon: ImageVector?, val drawableRes: Int? = null) {
    object Main : BottomNavItem("main", "Главная", null) // null, т.к. кастомная буква L
    object Plan : BottomNavItem("plan", "Планирование", Icons.Filled.CalendarToday)
    object Reports : BottomNavItem("reports", "Отчёты", Icons.Filled.List)
    object Settings : BottomNavItem("settings", "Настройки", Icons.Filled.Settings)
}

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LazyBonesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "main") {
                        composable("main") {
                            MainScreen(
                                viewModel = viewModel,
                                onOpenReportForm = { navController.navigate("plan_report") },
                                onOpenPlan = { navController.navigate("plan_plan") }
                            )
                        }
                        composable("plan") {
                            PlanScreen(
                                viewModel = getViewModel(),
                                initialTab = 0
                            )
                        }
                        composable("plan_report") {
                            PlanScreen(
                                viewModel = getViewModel(),
                                initialTab = 1
                            )
                        }
                        composable("plan_plan") {
                            PlanScreen(
                                viewModel = getViewModel(),
                                initialTab = 0
                            )
                        }
                        composable("reports") {
                            ReportsScreen(
                                viewModel = getViewModel()
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                viewModel = getViewModel()
                            )
                        }
                    }
                    CustomBottomNavigation(
                        navController = navController,
                        items = listOf(
                            BottomNavItem.Main,
                            BottomNavItem.Plan,
                            BottomNavItem.Reports,
                            BottomNavItem.Settings
                        ),
                        onItemClick = { item ->
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CustomBottomNavigation(
    navController: NavHostController,
    items: List<BottomNavItem>,
    onItemClick: (BottomNavItem) -> Unit
) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    Log.d("LazyBonesTab", "currentRoute = $currentRoute")
    
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val tabCount = items.size
    // Для plan_report и plan_plan считаем, что выбрана вкладка Plan
    val normalizedRoute = when (currentRoute) {
        "plan_report", "plan_plan" -> "plan"
        else -> currentRoute
    }
    val selectedIndex = items.indexOfFirst { it.route == normalizedRoute }.takeIf { it != -1 } ?: 0
    val tabWidth = (screenWidth - 32.dp) / tabCount
    val bubbleCenter = tabWidth * selectedIndex + tabWidth / 2 + 16.dp
    val animatedOffset by animateDpAsState(
        targetValue = bubbleCenter,
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = 200f
        ),
        label = "bubble_offset"
    )
    val indicatorWidth = 80.dp
    val indicatorHeight = 60.dp
    val bubbleCorner = 20.dp
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Большой анимированный пузырь под всеми табами
        Box(
            Modifier
                .offset(x = animatedOffset - indicatorWidth / 2, y = (-20).dp)
                .size(indicatorWidth, indicatorHeight)
                .align(Alignment.BottomStart)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                            Color.White.copy(alpha = 0.04f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(200f, 200f)
                    ),
                    shape = RoundedCornerShape(bubbleCorner)
                )
                .blur(24.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val selected = selectedIndex == index
                val iconColor = if (selected) MaterialTheme.colorScheme.primary else Color.Gray
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onItemClick(item) }
                ) {
                    if (item is BottomNavItem.Main) {
                        Text(
                            text = "L",
                            fontFamily = FontFamily(Font(R.font.fraktur_regular)),
                            fontSize = 28.sp,
                            color = iconColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.size(28.dp)
                        )
                    } else if (item.icon != null) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = iconColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Text(
                        text = item.label,
                        color = iconColor,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onOpenReportForm: () -> Unit = {},
    onOpenPlan: () -> Unit = {}
) {
    val goodCount = viewModel.goodCount.collectAsState().value
    val badCount = viewModel.badCount.collectAsState().value
    val reportStatus = viewModel.reportStatus.collectAsState().value

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text("Статус отчёта: ${statusText(reportStatus)}", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                CounterBox(count = goodCount, label = "Good", color = Color(0xFF4CAF50))
                CounterBox(count = badCount, label = "Bad", color = Color(0xFFF44336))
            }
            GoodBadProgressBar(
                goodCount = goodCount,
                badCount = badCount,
                modifier = Modifier.fillMaxWidth()
            )
            val timerText = viewModel.timerText.collectAsState().value
            Text(timerText, style = MaterialTheme.typography.bodyLarge)
            val canCreateReport = viewModel.canCreateReport.collectAsState().value
            val canCreatePlan = viewModel.canCreatePlan.collectAsState().value
            CreateButton(
                onOpenReportForm = onOpenReportForm,
                onOpenPlan = onOpenPlan,
                canCreateReport = canCreateReport,
                canCreatePlan = canCreatePlan
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CounterBox(count: Int, label: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$count", style = MaterialTheme.typography.displayMedium, color = color)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateButton(
    onOpenReportForm: () -> Unit,
    onOpenPlan: () -> Unit,
    canCreateReport: Boolean = true,
    canCreatePlan: Boolean = true
) {
    var showDialog by remember { mutableStateOf(false) }

    Button(
        onClick = { showDialog = true },
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text("Создать", style = MaterialTheme.typography.bodyLarge)
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            confirmButton = {},
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            showDialog = false
                            onOpenReportForm()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        enabled = canCreateReport
                    ) {
                        Text(
                            if (canCreateReport) "Создать отчёт" else "Отчет заблокирован",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Button(
                        onClick = {
                            showDialog = false
                            onOpenPlan()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        enabled = canCreatePlan
                    ) {
                        Text(
                            if (canCreatePlan) "Создать план" else "План заблокирован",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        )
    }
}

@Composable
fun GoodBadProgressBar(
    goodCount: Int,
    badCount: Int,
    modifier: Modifier = Modifier
) {
    val total = goodCount + badCount
    val goodRatio = if (total > 0) goodCount.toFloat() / total else 0.5f
    val badRatio = if (total > 0) badCount.toFloat() / total else 0.5f

    Column(modifier = modifier) {
        Text("Прогресс", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFE0E0E0))
        ) {
            if (total > 0) {
                val safeGoodRatio = goodRatio.coerceAtLeast(0.01f) // Минимум 0.01 чтобы weight был больше нуля
                val safeBadRatio = badRatio.coerceAtLeast(0.01f)
                Box(
                    modifier = Modifier
                        .weight(safeGoodRatio)
                        .fillMaxHeight()
                        .background(Color(0xFF4CAF50))
                )
                Box(
                    modifier = Modifier
                        .weight(safeBadRatio)
                        .fillMaxHeight()
                        .background(Color(0xFFF44336))
                )
            } else {
                // Когда нет данных, показываем пустую полоску
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .background(Color(0xFFE0E0E0))
                )
            }
        }
    }
}

@Composable
fun statusText(status: com.katapandroid.lazybones.ui.ReportStatus): String {
    return when (status) {
        com.katapandroid.lazybones.ui.ReportStatus.NOT_FILLED -> "Отчет не заполнен"
        com.katapandroid.lazybones.ui.ReportStatus.IN_PROGRESS -> "В процессе"
        com.katapandroid.lazybones.ui.ReportStatus.SAVED -> "Отчет сформирован"
        com.katapandroid.lazybones.ui.ReportStatus.PUBLISHED -> "Отчет опубликован"
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    // Preview requires ViewModel dependencies - disabled
}