package com.myreminder.app.ui.screens.addedit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.myreminder.app.data.local.AppDatabase
import com.myreminder.app.data.local.SettingsDataStore
import com.myreminder.app.data.local.TaskEntity
import com.myreminder.app.data.model.Priority
import com.myreminder.app.data.model.TaskType
import com.myreminder.app.notification.AlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class AddEditUiState(
    val title: String = "",
    val company: String = "",
    val type: TaskType = TaskType.INTERVIEW,
    val date: LocalDate = LocalDate.now(),
    val time: LocalTime? = LocalTime.now().plusHours(1).withMinute(0).withSecond(0),
    val meetingLink: String = "",
    val location: String = "",
    val priority: Priority = Priority.HIGH,
    val notes: String = "",
    val reminderDurationValue: Int = 1,
    val reminderDurationUnit: String = "HOURS", // "MINUTES", "HOURS", "DAYS"
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
    val infoMessage: String? = null
) {
    fun calculateTotalReminderMinutes(): Int {
        if (reminderDurationValue <= 0) return -1
        return when (reminderDurationUnit.uppercase()) {
            "MINUTES", "MINUTE" -> reminderDurationValue
            "HOURS", "HOUR" -> reminderDurationValue * 60
            "DAYS", "DAY" -> reminderDurationValue * 1440
            else -> reminderDurationValue
        }
    }

    fun getTargetDateTime(): LocalDateTime {
        val targetTime = time ?: LocalTime.of(23, 59)
        return LocalDateTime.of(date, targetTime)
    }

    fun getWindowExplanation(): String {
        val totalMins = calculateTotalReminderMinutes()
        if (totalMins <= 0) return "No reminders scheduled."
        val unitLabel = when (reminderDurationUnit.uppercase()) {
            "MINUTES", "MINUTE" -> if (reminderDurationValue == 1) "1 minute" else "$reminderDurationValue minutes"
            "HOURS", "HOUR" -> if (reminderDurationValue == 1) "1 hour" else "$reminderDurationValue hours"
            "DAYS", "DAY" -> if (reminderDurationValue == 1) "1 day" else "$reminderDurationValue days"
            else -> "$reminderDurationValue $reminderDurationUnit"
        }
        return "Repeated reminders will occur during the $unitLabel period before the target time."
    }
}

class AddEditViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).taskDao()
    private val settings = SettingsDataStore(application)
    private val alarmScheduler = AlarmScheduler(application)

    private val taskId: Long? = savedStateHandle.get<Long>("taskId")

    private val _uiState = MutableStateFlow(AddEditUiState())
    val uiState: StateFlow<AddEditUiState> = _uiState

    init {
        if (taskId != null) {
            loadTask(taskId)
        } else {
            loadDefaultReminder()
        }
    }

    private fun loadDefaultReminder() {
        viewModelScope.launch {
            val defaultRem = settings.defaultReminderMinutes.first()
            val (value, unit) = when {
                defaultRem >= 1440 && defaultRem % 1440 == 0 -> Pair(defaultRem / 1440, "DAYS")
                defaultRem >= 60 && defaultRem % 60 == 0 -> Pair(defaultRem / 60, "HOURS")
                defaultRem > 0 -> Pair(defaultRem, "MINUTES")
                else -> Pair(1, "HOURS")
            }
            _uiState.update {
                it.copy(
                    reminderDurationValue = value,
                    reminderDurationUnit = unit
                )
            }
        }
    }

    private fun loadTask(id: Long) {
        viewModelScope.launch {
            val task = dao.getTaskById(id)
            if (task != null) {
                _uiState.update {
                    it.copy(
                        title = task.title,
                        company = task.company,
                        type = task.type,
                        date = task.date,
                        time = task.time,
                        meetingLink = task.meetingLink ?: "",
                        location = task.location ?: "",
                        priority = task.priority,
                        notes = task.notes ?: "",
                        reminderDurationValue = if (task.reminderDurationValue > 0) task.reminderDurationValue else 1,
                        reminderDurationUnit = if (task.reminderDurationUnit.isNotBlank()) task.reminderDurationUnit else "HOURS"
                    )
                }
            }
        }
    }

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title, error = null) }
    }

    fun updateCompany(company: String) {
        _uiState.update { it.copy(company = company) }
    }

    fun updateType(type: TaskType) {
        _uiState.update { it.copy(type = type) }
    }

    fun updateDate(date: LocalDate) {
        _uiState.update { it.copy(date = date, error = null) }
        validateDateTime()
    }

    fun updateTime(time: LocalTime?) {
        _uiState.update { it.copy(time = time, error = null) }
        validateDateTime()
    }

    fun updateMeetingLink(link: String) {
        _uiState.update { it.copy(meetingLink = link) }
    }

    fun updateLocation(location: String) {
        _uiState.update { it.copy(location = location) }
    }

    fun updatePriority(priority: Priority) {
        _uiState.update { it.copy(priority = priority) }
    }

    fun updateNotes(notes: String) {
        _uiState.update { it.copy(notes = notes) }
    }

    fun updateReminderDuration(value: Int, unit: String) {
        _uiState.update {
            it.copy(
                reminderDurationValue = value.coerceAtLeast(1),
                reminderDurationUnit = unit,
                error = null
            )
        }
        validateDateTime()
    }

    private fun validateDateTime(): Boolean {
        val state = _uiState.value
        val now = LocalDateTime.now()
        val targetDateTime = state.getTargetDateTime()

        // 1. Check past date
        if (state.date.isBefore(LocalDate.now())) {
            _uiState.update { it.copy(error = "Target date cannot be in the past. Please select today or a future date.") }
            return false
        }

        // 2. Check past time if today
        if (state.date.isEqual(LocalDate.now()) && state.time != null && state.time.isBefore(LocalTime.now())) {
            _uiState.update { it.copy(error = "Target time cannot be earlier than the current time (${LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("h:mm a"))}).") }
            return false
        }

        // 3. Complete datetime check
        if (!targetDateTime.isAfter(now)) {
            _uiState.update { it.copy(error = "Target date and time must be in the future.") }
            return false
        }

        // 4. Check reminder window
        val totalMinutes = state.calculateTotalReminderMinutes()
        val windowStart = targetDateTime.minusMinutes(totalMinutes.toLong())

        if (windowStart.isBefore(now)) {
            _uiState.update {
                it.copy(
                    error = null,
                    infoMessage = "Note: The reminder window has already begun. Reminders will start immediately until target time."
                )
            }
        } else {
            _uiState.update { it.copy(error = null, infoMessage = null) }
        }

        return true
    }

    fun saveTask() {
        viewModelScope.launch {
            val state = _uiState.value

            if (state.title.isBlank()) {
                _uiState.update { it.copy(error = "Task title is required.") }
                return@launch
            }

            if (!validateDateTime()) {
                return@launch
            }

            _uiState.update { it.copy(isSaving = true, error = null) }

            val totalReminderMinutes = state.calculateTotalReminderMinutes()

            val task = TaskEntity(
                id = taskId ?: 0L,
                title = state.title.trim(),
                company = state.company.trim(),
                type = state.type,
                date = state.date,
                time = state.time,
                meetingLink = state.meetingLink.takeIf { it.isNotBlank() }?.trim(),
                location = state.location.takeIf { it.isNotBlank() }?.trim(),
                priority = state.priority,
                notes = state.notes.takeIf { it.isNotBlank() }?.trim(),
                reminderDurationValue = state.reminderDurationValue,
                reminderDurationUnit = state.reminderDurationUnit,
                reminderMinutes = totalReminderMinutes
            )

            val id = if (taskId != null) {
                dao.updateTask(task)
                taskId
            } else {
                dao.insertTask(task)
            }

            val savedTask = task.copy(id = id)
            if (savedTask.calculateTotalReminderMinutes() >= 0 && !savedTask.completed) {
                alarmScheduler.scheduleTaskReminder(savedTask)
            } else {
                alarmScheduler.cancelTaskReminder(id)
            }

            _uiState.update { it.copy(isSaving = false, isSaved = true) }
        }
    }
}
