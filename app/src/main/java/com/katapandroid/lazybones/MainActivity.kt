package com.katapandroid.lazybones

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.katapandroid.lazybones.ui.MainViewModel
import com.katapandroid.lazybones.ui.ReportStatus
import com.katapandroid.lazybones.ui.theme.LazyBonesTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModel()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LazyBonesTheme {
                MainScreen(mainViewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val goodCount = viewModel.goodCount.collectAsState().value
    val badCount = viewModel.badCount.collectAsState().value
    val reportStatus = viewModel.reportStatus.collectAsState().value
    val timerProgress = viewModel.timerProgress.collectAsState().value
    val timerTimeText = viewModel.timerTimeText.collectAsState().value

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("LazyBones") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text("Статус отчёта: ${statusText(reportStatus)}", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                CounterBox(count = goodCount, label = "Good", color = MaterialTheme.colorScheme.primary)
                CounterBox(count = badCount, label = "Bad", color = MaterialTheme.colorScheme.error)
            }
            LinearProgressIndicator(progress = timerProgress, modifier = Modifier.fillMaxWidth())
            Text("До конца: $timerTimeText", style = MaterialTheme.typography.bodyLarge)
            Button(onClick = { /* TODO: переход к форме отчёта */ }) {
                Text("Создать/Редактировать отчёт")
            }
        }
    }
}

@Composable
fun CounterBox(count: Int, label: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$count", style = MaterialTheme.typography.displayMedium, color = color)
        Text(label, style = MaterialTheme.typography.bodyMedium)
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
        CounterBox(5, "Good", MaterialTheme.colorScheme.primary)
    }
}