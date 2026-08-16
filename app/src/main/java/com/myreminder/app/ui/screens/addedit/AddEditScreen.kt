package com.myreminder.app.ui.screens.addedit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.myreminder.app.data.model.Priority
import com.myreminder.app.data.model.ReminderInterval
import com.myreminder.app.data.model.TaskType
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddEditViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onNavigateBack()
    }

    // Date pickers
    var showEventDatePicker by remember { mutableStateOf(false) }
    var showEventTimePicker by remember { mutableStateOf(false) }
    var showReminderDatePicker by remember { mutableStateOf(false) }
    var showReminderTimePicker by remember { mutableStateOf(false) }

    // Event Date Picker
    if (showEventDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.eventDate.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showEventDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val pickedDate = Instant.ofEpochMilli(it).atOffset(java.time.ZoneOffset.UTC).toLocalDate()
                        viewModel.updateEventDate(pickedDate)
                    }
                    showEventDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showEventDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    // Event Time Picker
    if (showEventTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = uiState.eventTime?.hour ?: 18,
            initialMinute = uiState.eventTime?.minute ?: 0
        )
        AlertDialog(
            onDismissRequest = { showEventTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateEventTime(LocalTime.of(timePickerState.hour, timePickerState.minute))
                    showEventTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showEventTimePicker = false }) { Text("Cancel") } },
            text = { TimePicker(state = timePickerState) }
        )
    }

    // Reminder Date Picker
    if (showReminderDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.reminderDate.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showReminderDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val pickedDate = Instant.ofEpochMilli(it).atOffset(java.time.ZoneOffset.UTC).toLocalDate()
                        viewModel.updateReminderDate(pickedDate)
                    }
                    showReminderDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showReminderDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    // Reminder Time Picker
    if (showReminderTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = uiState.reminderTime.hour,
            initialMinute = uiState.reminderTime.minute
        )
        AlertDialog(
            onDismissRequest = { showReminderTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateReminderTime(LocalTime.of(timePickerState.hour, timePickerState.minute))
                    showReminderTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showReminderTimePicker = false }) { Text("Cancel") } },
            text = { TimePicker(state = timePickerState) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Reminder") },
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
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Error Banner
            if (uiState.error != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(uiState.error!!, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Info Banner
            if (uiState.infoMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = "Info", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(uiState.infoMessage!!, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // ===== SECTION 1: Task Details =====
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::updateTitle,
                label = { Text("Reminder / Task Title *") },
                placeholder = { Text("e.g. Submit project / Technical Interview") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.error != null && uiState.title.isBlank()
            )

            OutlinedTextField(
                value = uiState.company,
                onValueChange = viewModel::updateCompany,
                label = { Text("Company / Organization") },
                placeholder = { Text("e.g. Google / University") },
                modifier = Modifier.fillMaxWidth()
            )

            // Task Type
            var typeExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = it }) {
                OutlinedTextField(
                    value = uiState.type.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Task Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                    TaskType.values().forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.displayName) },
                            onClick = { viewModel.updateType(type); typeExpanded = false }
                        )
                    }
                }
            }

            HorizontalDivider()

            // ===== SECTION 2: Event Date & Time =====
            Text("📅 Event Date & Time", style = MaterialTheme.typography.titleMedium)
            Text("When the task or event is due.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showEventDatePicker = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = "Event Date", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(uiState.eventDate.format(DateTimeFormatter.ofPattern("d MMM yyyy")))
                }
                OutlinedButton(onClick = { showEventTimePicker = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Schedule, contentDescription = "Event Time", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(uiState.eventTime?.format(DateTimeFormatter.ofPattern("h:mm a")) ?: "Set Time")
                }
            }

            HorizontalDivider()

            // ===== SECTION 3: Reminder Start Date & Time =====
            Text("🔔 Reminder Start", style = MaterialTheme.typography.titleMedium)
            Text("When notifications begin sending. Default: 7:00 AM (changeable in Settings).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showReminderDatePicker = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = "Reminder Date", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(uiState.reminderDate.format(DateTimeFormatter.ofPattern("d MMM yyyy")))
                }
                OutlinedButton(onClick = { showReminderTimePicker = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = "Reminder Time", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(uiState.reminderTime.format(DateTimeFormatter.ofPattern("h:mm a")))
                }
            }

            HorizontalDivider()

            // ===== SECTION 4: Reminder Interval =====
            Text("🔁 Reminder Interval", style = MaterialTheme.typography.titleMedium)
            Text("How often to repeat notifications between the reminder start and the event.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            var intervalExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = intervalExpanded, onExpandedChange = { intervalExpanded = it }) {
                OutlinedTextField(
                    value = uiState.reminderInterval.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Repeat Every") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = intervalExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = intervalExpanded, onDismissRequest = { intervalExpanded = false }) {
                    ReminderInterval.values().forEach { interval ->
                        DropdownMenuItem(
                            text = { Text(interval.displayName) },
                            onClick = { viewModel.updateReminderInterval(interval); intervalExpanded = false }
                        )
                    }
                }
            }

            // Explanation box
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(uiState.getIntervalExplanation(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }

            HorizontalDivider()

            // ===== SECTION 5: Priority =====
            Text("Priority", style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Priority.values().forEach { priority ->
                    FilterChip(
                        selected = uiState.priority == priority,
                        onClick = { viewModel.updatePriority(priority) },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(10.dp).background(priority.color, shape = RoundedCornerShape(50)))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(priority.displayName)
                            }
                        }
                    )
                }
            }

            HorizontalDivider()

            // ===== SECTION 6: Extra Details =====
            OutlinedTextField(
                value = uiState.meetingLink,
                onValueChange = viewModel::updateMeetingLink,
                label = { Text("Meeting / Test Link") },
                placeholder = { Text("https://meet.google.com/...") },
                leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.location,
                onValueChange = viewModel::updateLocation,
                label = { Text("Location / Campus") },
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.notes,
                onValueChange = viewModel::updateNotes,
                label = { Text("Notes / Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            // ===== Save / Cancel =====
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onNavigateBack) { Text("Cancel") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { viewModel.saveTask() }, enabled = !uiState.isSaving) {
                    Text(if (uiState.isSaving) "Saving..." else "Save Reminder")
                }
            }
        }
    }
}
