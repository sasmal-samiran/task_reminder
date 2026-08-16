package com.myreminder.app.ui.screens.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    viewModel: TaskDetailViewModel = viewModel()
) {
    val task by viewModel.task.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Reminder") },
            text = { Text("Are you sure you want to delete this reminder?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteTask(onNavigateBack)
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reminder Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    task?.let { t ->
                        IconButton(onClick = { onNavigateToEdit(t.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        val t = task ?: return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AssistChip(
                    onClick = { },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(t.priority.color, shape = RoundedCornerShape(50))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("${t.priority.displayName} Priority")
                        }
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                AssistChip(
                    onClick = { },
                    label = { Text(t.type.displayName) }
                )
            }

            Text(t.title, style = MaterialTheme.typography.headlineMedium)

            if (t.company.isNotBlank()) {
                Text(t.company, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            HorizontalDivider()

            val eventDatePrefix = when {
                t.date.isEqual(LocalDate.now()) -> "Today"
                t.date.isEqual(LocalDate.now().plusDays(1)) -> "Tomorrow"
                else -> t.date.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
            }

            // Event Date & Time
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Event, contentDescription = "Event Date", tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Event Deadline", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text("$eventDatePrefix • ${t.time?.format(DateTimeFormatter.ofPattern("h:mm a")) ?: "All Day"}", style = MaterialTheme.typography.bodyLarge)
                }
            }

            // Reminder Start Date & Time
            val reminderDatePrefix = when {
                t.reminderDate.isEqual(LocalDate.now()) -> "Today"
                t.reminderDate.isEqual(LocalDate.now().plusDays(1)) -> "Tomorrow"
                else -> t.reminderDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.NotificationsActive, contentDescription = "Reminder Start", tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Reminders Start", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text("$reminderDatePrefix • ${t.reminderTime?.format(DateTimeFormatter.ofPattern("h:mm a")) ?: "All Day"}", style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Repeat Interval
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Repeat, contentDescription = "Repeat Interval", tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Repeat Frequency", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text("Every ${t.getIntervalDisplayName()} until event deadline", style = MaterialTheme.typography.bodyMedium)
                }
            }

            if (!t.location.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = "Location")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(t.location)
                }
            }

            if (!t.meetingLink.isNullOrBlank()) {
                Button(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(t.meetingLink))
                    context.startActivity(intent)
                }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Link, contentDescription = "Link")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Join Link")
                }
            }

            if (!t.notes.isNullOrBlank()) {
                Text("Notes", style = MaterialTheme.typography.titleMedium)
                Text(t.notes)
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { viewModel.toggleComplete() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (t.completed) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(if (t.completed) "Mark Incomplete" else "Mark Complete")
            }
        }
    }
}
