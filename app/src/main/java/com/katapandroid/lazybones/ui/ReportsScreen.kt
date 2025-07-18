package com.katapandroid.lazybones.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.katapandroid.lazybones.data.Post
import org.koin.androidx.compose.getViewModel
import java.util.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Delete

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel = getViewModel(),
    onReportClick: (Post) -> Unit = {},
    onCreateReport: () -> Unit = {}
) {
    val posts = viewModel.posts.collectAsState().value
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Text("Список отчётов", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        if (posts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Нет отчётов")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(0.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(posts) { post ->
                    ReportCard(post, onClick = { onReportClick(post) })
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onCreateReport, modifier = Modifier.align(Alignment.End)) {
            Text("+")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportCard(post: Post, onClick: () -> Unit = {}) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Дата: ${post.date}", style = MaterialTheme.typography.titleMedium)
            Text("Good: ${post.goodCount}  Bad: ${post.badCount}", style = MaterialTheme.typography.bodyMedium)
            Text("Статус: ${if (post.published) "Опубликован" else "Черновик"}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Preview(showBackground = true)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreenPreview() {
    val mockPosts = listOf(
        Post(1, Date(), "Тест 1", listOf(), listOf(), false, 3, 1),
        Post(2, Date(), "Тест 2", listOf(), listOf(), true, 5, 0)
    )
    MaterialTheme {
        ReportsScreenMock(posts = mockPosts)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreenMock(posts: List<Post>) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Text("Список отчётов", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        if (posts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Нет отчётов")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(0.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(posts) { post ->
                    ReportCard(post)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {}, modifier = Modifier.align(Alignment.End)) {
            Text("+")
        }
    }
} 