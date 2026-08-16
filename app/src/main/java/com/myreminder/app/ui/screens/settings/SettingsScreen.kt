package com.myreminder.app.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.myreminder.app.data.model.ReminderOption
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val morningHour by viewModel.morningHour.collectAsStateWithLifecycle()
    val morningMinute by viewModel.morningMinute.collectAsStateWithLifecycle()
    val defaultReminder by viewModel.defaultReminderMinutes.collectAsStateWithLifecycle()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()

    var showTimePicker by remember { mutableStateOf(false) }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = morningHour,
            initialMinute = morningMinute
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setMorningTime(timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ListItem(
                headlineContent = { Text("Morning Reminder") },
                supportingContent = { 
                    val time = LocalTime.of(morningHour, morningMinute)
                    Text(time.format(DateTimeFormatter.ofPattern("h:mm a"))) 
                },
                modifier = Modifier.clickable { showTimePicker = true }
            )
            HorizontalDivider()

            var reminderExpanded by remember { mutableStateOf(false) }
            val currentReminderStr = ReminderOption.values().find { it.minutesBefore == defaultReminder }?.displayName ?: "Unknown"
            
            ListItem(
                headlineContent = { Text("Default Task Reminder") },
                supportingContent = {
                    ExposedDropdownMenuBox(
                        expanded = reminderExpanded,
                        onExpandedChange = { reminderExpanded = it }
                    ) {
                        Text(
                            text = currentReminderStr,
                            modifier = Modifier.menuAnchor(),
                            color = MaterialTheme.colorScheme.primary
                        )
                        ExposedDropdownMenu(
                            expanded = reminderExpanded,
                            onDismissRequest = { reminderExpanded = false }
                        ) {
                            ReminderOption.values().forEach { opt ->
                                DropdownMenuItem(
                                    text = { Text(opt.displayName) },
                                    onClick = {
                                        viewModel.setDefaultReminder(opt.minutesBefore)
                                        reminderExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            )
            HorizontalDivider()
            
            ListItem(
                headlineContent = { Text("Enable Notifications") },
                supportingContent = { Text("Turn all reminders and morning summaries on or off") },
                trailingContent = {
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { viewModel.setNotifications(it) }
                    )
                }
            )
        }
    }
}
