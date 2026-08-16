package com.myreminder.app.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Task
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.myreminder.app.ui.components.EmptyState
import com.myreminder.app.ui.components.TaskCard
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToAddTask: () -> Unit,
    onNavigateToTaskDetail: (Long) -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val todayTasks by viewModel.todayTasks.collectAsStateWithLifecycle()
    val tomorrowTasks by viewModel.tomorrowTasks.collectAsStateWithLifecycle()
    val upcomingTasks by viewModel.upcomingTasks.collectAsStateWithLifecycle()

    val currentHour = LocalTime.now().hour
    val greeting = when (currentHour) {
        in 0..11 -> "Good Morning 👋"
        in 12..16 -> "Good Afternoon 👋"
        else -> "Good Evening 👋"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(greeting) },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAddTask) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = true,
                    onClick = { }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Calendar") },
                    label = { Text("Calendar") },
                    selected = false,
                    onClick = onNavigateToCalendar
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    label = { Text("Search") },
                    selected = false,
                    onClick = onNavigateToSearch
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Today's Tasks", style = MaterialTheme.typography.titleLarge)
            }
            if (todayTasks.isEmpty()) {
                item {
                    EmptyState(message = "No tasks for today", icon = Icons.Default.Task)
                }
            } else {
                items(todayTasks) { task ->
                    TaskCard(
                        task = task,
                        onClick = { onNavigateToTaskDetail(task.id) },
                        onComplete = { viewModel.toggleComplete(task.id, it) }
                    )
                }
            }
            
            if (tomorrowTasks.isNotEmpty()) {
                item {
                    Text("Tomorrow", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
                }
                items(tomorrowTasks) { task ->
                    TaskCard(
                        task = task,
                        onClick = { onNavigateToTaskDetail(task.id) },
                        onComplete = { viewModel.toggleComplete(task.id, it) }
                    )
                }
            }
            
            if (upcomingTasks.isNotEmpty()) {
                item {
                    Text("Upcoming", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
                }
                items(upcomingTasks) { task ->
                    TaskCard(
                        task = task,
                        onClick = { onNavigateToTaskDetail(task.id) },
                        onComplete = { viewModel.toggleComplete(task.id, it) }
                    )
                }
            }
        }
    }
}
