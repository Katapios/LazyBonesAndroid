package com.katapandroid.lazybones

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.katapandroid.lazybones.ui.MainViewModel
import com.katapandroid.lazybones.ui.ReportStatus
import com.katapandroid.lazybones.ui.theme.LazyBonesTheme
import org.koin.androidx.viewmodel.ext.android.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.katapandroid.lazybones.ui.ReportsScreen
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ArrowBack
import org.koin.androidx.compose.getViewModel
import org.koin.androidx.compose.koinViewModel
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import com.katapandroid.lazybones.ui.SettingsScreen
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.getValue
import androidx.navigation.compose.currentBackStackEntryAsState
import com.katapandroid.lazybones.ui.PlanScreen
import com.katapandroid.lazybones.ui.ReportFormScreen

sealed class BottomNavItem(val route: String, val label: String, val icon: ImageVector) {
    object Main : BottomNavItem("main", "Главная", Icons.Default.Home)
    object Plan : BottomNavItem("plan", "Планирование", Icons.Default.Edit)
    object Reports : BottomNavItem("reports", "Отчёты", Icons.AutoMirrored.Filled.List)
    object Settings : BottomNavItem("settings", "Настройки", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModel()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LazyBonesTheme {
                val navController = rememberNavController()
                AppNavHost(navController, mainViewModel)
            }
        }
    }
}

@Composable
fun AppNavHost(navController: NavHostController, mainViewModel: MainViewModel) {
    val items = listOf(
        BottomNavItem.Main,
        BottomNavItem.Plan,
        BottomNavItem.Reports,
        BottomNavItem.Settings
    )
    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController, items)
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Main.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(BottomNavItem.Main.route) {
                MainScreen(
                    mainViewModel,
                    onOpenReportForm = { navController.navigate("reportForm") },
                    onOpenPlan = { navController.navigate(BottomNavItem.Plan.route) }
                )
            }
            composable(BottomNavItem.Plan.route) { PlanScreen() }
            composable(BottomNavItem.Reports.route) { ReportsScreen() }
            composable(BottomNavItem.Settings.route) { SettingsScreen() }
            composable("reportForm") {
                ReportFormScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController, items: List<BottomNavItem>) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    if (item.route == BottomNavItem.Main.route) {
                        // Для главной вкладки всегда возвращаемся на главный экран
                        navController.navigate(item.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    } else if (currentRoute != item.route) {
                        // Для остальных вкладок только если не на текущей
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) }
            )
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
                // Пусто, т.к. используем кастомный контент ниже
            },
            dismissButton = {
                // Пусто, т.к. используем кастомный контент ниже
            },
            title = null,
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Что создаём?", style = MaterialTheme.typography.titleLarge)
                    if (errorText != null) {
                        Text(errorText!!, color = Color.Red, style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Text("Выберите, что хотите создать", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = {
                                try {
                                    showDialog = false
                                    onOpenPlan()
                                } catch (e: Exception) {
                                    errorText = "Ошибка: ${e.localizedMessage ?: e.toString()}"
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                "План на день",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        Button(
                            onClick = {
                                try {
                                    showDialog = false
                                    onOpenReportForm()
                                } catch (e: Exception) {
                                    errorText = "Ошибка: ${e.localizedMessage ?: e.toString()}"
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            )
                        ) {
                            Text(
                                "Отчёт за день",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
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
    
    Box(
        modifier = modifier
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.LightGray),
        contentAlignment = Alignment.CenterStart
    ) {
        if (total == 0) {
            // Если нет данных, показываем серую полоску
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Gray)
            )
        } else {
            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                // Зелёная часть (good)
                if (goodCount > 0) {
                    Box(
                        modifier = Modifier
                            .weight(goodCount.toFloat())
                            .fillMaxHeight()
                            .background(Color(0xFF4CAF50))
                    )
                }
                // Красная часть (bad)
                if (badCount > 0) {
                    Box(
                        modifier = Modifier
                            .weight(badCount.toFloat())
                            .fillMaxHeight()
                            .background(Color(0xFFF44336))
                    )
                }
            }
        }
    }
}

fun statusText(status: ReportStatus): String = when (status) {
    ReportStatus.NOT_STARTED -> "Не начат"
    ReportStatus.IN_PROGRESS -> "В процессе"
    ReportStatus.DONE -> "Сделан"
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    LazyBonesTheme {
        // Моки для превью
        val goodCount = 7
        val badCount = 2
        val reportStatus = ReportStatus.IN_PROGRESS
        val timerTimeText = "01:23:45"
        // Вынесем UI-логику в отдельную функцию для превью
        MainScreenMock(
            goodCount = goodCount,
            badCount = badCount,
            reportStatus = reportStatus,
            timerTimeText = timerTimeText
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenMock(
    goodCount: Int,
    badCount: Int,
    reportStatus: ReportStatus,
    timerTimeText: String,
    onOpenReportForm: () -> Unit = {},
    onOpenPlan: () -> Unit = {}
) {
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
            TopAppBar(title = { Text("LazyBones") })
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
fun ReportFormScreen(
    onBack: () -> Unit = {},
    viewModel: com.katapandroid.lazybones.ui.ReportFormViewModel = koinViewModel()
) {
    val content = viewModel.content.collectAsState().value
    val goodCount = viewModel.goodCount.collectAsState().value
    val badCount = viewModel.badCount.collectAsState().value
    var isSaving = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = content,
            onValueChange = { viewModel.setContent(it) },
            label = { Text("Заметка") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = { viewModel.setGoodCount(goodCount + 1) }) { Text("Good: $goodCount") }
            Button(onClick = { viewModel.setBadCount(badCount + 1) }) { Text("Bad: $badCount") }
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = {
                isSaving.value = true
                viewModel.save {
                    isSaving.value = false
                    onBack()
                }
            },
            enabled = !isSaving.value,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isSaving.value) "Сохраняем..." else "Сохранить")
        }
    }
}

@Preview(showBackground = true)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportFormScreenPreview() {
    LazyBonesTheme {
        ReportFormScreenMock(
            content = "Моя заметка",
            goodCount = 2,
            badCount = 1
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportFormScreenMock(
    content: String,
    goodCount: Int,
    badCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TopAppBar(title = { Text("Форма отчёта") })
        OutlinedTextField(
            value = content,
            onValueChange = {},
            label = { Text("Заметка") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = { }) { Text("Good: $goodCount") }
            Button(onClick = { }) { Text("Bad: $badCount") }
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = {},
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Сохранить")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        TopAppBar(title = { Text("Настройки") })
        Text("Заглушка настроек")
    }
}