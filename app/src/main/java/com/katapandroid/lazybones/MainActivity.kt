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
import com.katapandroid.lazybones.ui.ReportFormScreen
import com.katapandroid.lazybones.ui.ReportStatus
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
                                onOpenReportForm = { navController.navigate("report_form") },
                                onOpenPlan = { navController.navigate("plan") }
                            )
                        }
                        composable("plan") {
                            PlanScreen(
                                viewModel = getViewModel()
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
                        composable("report_form") {
                            ReportFormScreen(
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
    val selectedIndex = items.indexOfFirst { it.route == currentRoute }.takeIf { it != -1 } ?: 0
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
    val timerTimeText = viewModel.timerTimeText.collectAsState().value

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
            Text("До конца: $timerTimeText", style = MaterialTheme.typography.bodyLarge)
            CreateButton(
                onOpenReportForm = onOpenReportForm,
                onOpenPlan = onOpenPlan
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
    onOpenPlan: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    Button(
        onClick = { showDialog = true },
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF4CAF50)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Создать", style = MaterialTheme.typography.bodyLarge)
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Отмена")
                }
            },
            title = { Text("Выберите действие") },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            showDialog = false
                            onOpenReportForm()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Создать отчёт")
                    }
                    TextButton(
                        onClick = {
                            showDialog = false
                            onOpenPlan()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Создать план")
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
                Box(
                    modifier = Modifier
                        .weight(goodRatio)
                        .fillMaxHeight()
                        .background(Color(0xFF4CAF50))
                )
                Box(
                    modifier = Modifier
                        .weight(badRatio)
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

fun statusText(status: ReportStatus): String {
    return when (status) {
        ReportStatus.NOT_STARTED -> "Не начат"
        ReportStatus.IN_PROGRESS -> "В процессе"
        ReportStatus.DONE -> "Завершён"
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    LazyBonesTheme {
        MainScreen(
            viewModel = MainViewModel(koinViewModel()),
            onOpenReportForm = {},
            onOpenPlan = {}
        )
    }
}