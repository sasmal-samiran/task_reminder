package com.myreminder.app.ui.screens.settings

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
    val defaultInterval by viewModel.defaultReminderMinutes.collectAsStateWithLifecycle()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val customSoundUri by viewModel.customSoundUri.collectAsStateWithLifecycle()
    val soundTitle by viewModel.soundTitle.collectAsStateWithLifecycle()
    val isPlayingPreview by viewModel.isPlayingPreview.collectAsStateWithLifecycle()

    var showTimePicker by remember { mutableStateOf(false) }

    // Ringtone Picker Launcher
    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val pickedUri: Uri? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            viewModel.setCustomSound(pickedUri)
        }
    }

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
            // Notification Sound Setting with System Picker & Preview
            ListItem(
                headlineContent = { Text("Notification Sound") },
                supportingContent = { Text(soundTitle) },
                leadingContent = {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Play/Stop Preview Button
                        IconButton(onClick = { viewModel.togglePlayPreview() }) {
                            Icon(
                                imageVector = if (isPlayingPreview) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = if (isPlayingPreview) "Stop Preview" else "Play Preview",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Select / Change Sound Button
                        TextButton(onClick = {
                            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Notification Sound")
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, Settings.System.DEFAULT_NOTIFICATION_URI)
                                if (!customSoundUri.isNullOrBlank()) {
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(customSoundUri))
                                }
                            }
                            ringtonePickerLauncher.launch(intent)
                        }) {
                            Text("Change")
                        }
                    }
                }
            )
            HorizontalDivider()

            // Morning Reminder Time
            ListItem(
                headlineContent = { Text("Morning Summary (7:00 AM)") },
                supportingContent = {
                    val time = LocalTime.of(morningHour, morningMinute)
                    Text(time.format(DateTimeFormatter.ofPattern("h:mm a")))
                },
                leadingContent = {
                    Icon(
                        Icons.Default.WbSunny,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier.clickable { showTimePicker = true }
            )
            HorizontalDivider()

            // Default Repeat Interval
            var intervalExpanded by remember { mutableStateOf(false) }
            val intervalOptions = listOf(
                30 to "30 minutes",
                60 to "1 hour",
                1440 to "1 day",
                10080 to "1 week"
            )
            val currentIntervalLabel = intervalOptions.find { it.first == defaultInterval }?.second ?: "1 day"

            ListItem(
                headlineContent = { Text("Default Repeat Interval") },
                supportingContent = {
                    ExposedDropdownMenuBox(
                        expanded = intervalExpanded,
                        onExpandedChange = { intervalExpanded = it }
                    ) {
                        Text(
                            text = currentIntervalLabel,
                            modifier = Modifier.menuAnchor(),
                            color = MaterialTheme.colorScheme.primary
                        )
                        ExposedDropdownMenu(
                            expanded = intervalExpanded,
                            onDismissRequest = { intervalExpanded = false }
                        ) {
                            intervalOptions.forEach { (mins, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        viewModel.setDefaultReminder(mins)
                                        intervalExpanded = false
                                    }
                                )
                            }
                        }
                    }
                },
                leadingContent = {
                    Icon(
                        Icons.Default.Repeat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            )
            HorizontalDivider()

            // Enable / Disable All Notifications
            ListItem(
                headlineContent = { Text("Enable Notifications") },
                supportingContent = { Text("Turn all reminder notifications and summaries on or off") },
                leadingContent = {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
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
