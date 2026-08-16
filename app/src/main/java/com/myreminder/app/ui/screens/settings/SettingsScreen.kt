package com.myreminder.app.ui.screens.settings

import android.app.Activity
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    val defaultReminderHour by viewModel.defaultReminderHour.collectAsStateWithLifecycle()
    val defaultReminderMinute by viewModel.defaultReminderMinute.collectAsStateWithLifecycle()
    val soundUri by viewModel.notificationSoundUri.collectAsStateWithLifecycle()
    val soundName by viewModel.notificationSoundName.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var showMorningTimePicker by remember { mutableStateOf(false) }
    var showReminderTimePicker by remember { mutableStateOf(false) }

    // Ringtone picker launcher
    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val pickedUri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            val uriStr = pickedUri?.toString() ?: ""
            val name = if (pickedUri != null) {
                try {
                    val ringtone = RingtoneManager.getRingtone(context, pickedUri)
                    ringtone?.getTitle(context) ?: "Custom Sound"
                } catch (_: Exception) { "Custom Sound" }
            } else {
                "Default Notification Sound"
            }
            viewModel.setNotificationSound(uriStr, name)
        }
    }

    if (showMorningTimePicker) {
        val timePickerState = rememberTimePickerState(initialHour = morningHour, initialMinute = morningMinute)
        AlertDialog(
            onDismissRequest = { showMorningTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setMorningTime(timePickerState.hour, timePickerState.minute)
                    showMorningTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showMorningTimePicker = false }) { Text("Cancel") } },
            text = { TimePicker(state = timePickerState) }
        )
    }

    if (showReminderTimePicker) {
        val timePickerState = rememberTimePickerState(initialHour = defaultReminderHour, initialMinute = defaultReminderMinute)
        AlertDialog(
            onDismissRequest = { showReminderTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setDefaultReminderTime(timePickerState.hour, timePickerState.minute)
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
            // Morning Summary Time
            ListItem(
                headlineContent = { Text("Morning Summary Time") },
                supportingContent = {
                    val time = LocalTime.of(morningHour, morningMinute)
                    Text(time.format(DateTimeFormatter.ofPattern("h:mm a")))
                },
                modifier = Modifier.clickable { showMorningTimePicker = true }
            )
            HorizontalDivider()

            // Default Reminder Time
            ListItem(
                headlineContent = { Text("Default Reminder Time") },
                supportingContent = {
                    val time = LocalTime.of(defaultReminderHour, defaultReminderMinute)
                    Text("New reminders default to ${time.format(DateTimeFormatter.ofPattern("h:mm a"))}")
                },
                modifier = Modifier.clickable { showReminderTimePicker = true }
            )
            HorizontalDivider()

            // Default Reminder Interval
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

            // Notification Sound
            ListItem(
                headlineContent = { Text("Notification Sound") },
                supportingContent = { Text(soundName) },
                leadingContent = {
                    Icon(Icons.Default.MusicNote, contentDescription = "Sound")
                },
                trailingContent = {
                    // Preview button
                    IconButton(onClick = {
                        try {
                            val uri = if (soundUri.isNotBlank()) Uri.parse(soundUri)
                                else RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                            val ringtone = RingtoneManager.getRingtone(context, uri)
                            ringtone?.play()
                        } catch (_: Exception) {}
                    }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Preview Sound")
                    }
                },
                modifier = Modifier.clickable {
                    val intent = android.content.Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Notification Sound")
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                        if (soundUri.isNotBlank()) {
                            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(soundUri))
                        } else {
                            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                        }
                    }
                    ringtonePickerLauncher.launch(intent)
                }
            )
            HorizontalDivider()

            // Enable Notifications
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
