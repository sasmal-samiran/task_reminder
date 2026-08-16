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
import java.time.format.DateTimeFormatter

data class AddEditUiState(
    val title: String = "",
    val company: String = "",
    val type: TaskType = TaskType.DEADLINE, // Default task type is DEADLINE
    val date: LocalDate = LocalDate.now(), // Event Date
    val time: LocalTime? = LocalTime.now().plusHours(2).withMinute(0).withSecond(0), // Event Time
    val reminderDate: LocalDate = LocalDate.now(), // Reminder Start Date
    val reminderTime: LocalTime? = LocalTime.now().withMinute(0).withSecond(0), // Reminder Start Time
    val intervalMinutes: Int = 1440, // 30 mins, 60 mins (1 hr), 1440 mins (1 day), 10080 mins (1 week)
    val meetingLink: String = "",
    val location: String = "",
    val priority: Priority = Priority.HIGH,
    val notes: String = "",
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
    val infoMessage: String? = null
) {
    fun getEventDateTime(): LocalDateTime {
        val targetTime = time ?: LocalTime.of(23, 59)
        return LocalDateTime.of(date, targetTime)
    }

    fun getReminderStartDateTime(): LocalDateTime {
        val rTime = reminderTime ?: time ?: LocalTime.of(9, 0)
        return LocalDateTime.of(reminderDate, rTime)
    }

    fun getIntervalLabel(): String {
        return when (intervalMinutes) {
            30 -> "30 minutes"
            60 -> "1 hour"
            1440 -> "1 day"
            10080 -> "1 week"
            else -> if (intervalMinutes % 1440 == 0) "${intervalMinutes / 1440} days" else "$intervalMinutes minutes"
        }
    }

    fun getScheduleExplanation(): String {
        val startFormatted = formatDateTime(reminderDate, reminderTime)
        val endFormatted = formatDateTime(date, time)
        val intervalStr = getIntervalLabel()
        return "Notifications will be sent starting from $startFormatted until $endFormatted in intervals of $intervalStr."
    }

    private fun formatDateTime(d: LocalDate, t: LocalTime?): String {
        val today = LocalDate.now()
        val dStr = when {
            d.isEqual(today) -> "Today"
            d.isEqual(today.plusDays(1)) -> "Tomorrow"
            else -> d.format(DateTimeFormatter.ofPattern("d MMM"))
        }
        val tStr = t?.format(DateTimeFormatter.ofPattern("h:mm a")) ?: "end of day"
        return "$dStr at $tStr"
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
            loadDefaults()
        }
    }

    private fun loadDefaults() {
        viewModelScope.launch {
            val defaultInterval = settings.defaultReminderMinutes.first()
            val initialInterval = if (defaultInterval in listOf(30, 60, 1440, 10080)) defaultInterval else 1440
            _uiState.update {
                it.copy(
                    type = TaskType.DEADLINE,
                    intervalMinutes = initialInterval
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
                        reminderDate = task.reminderDate,
                        reminderTime = task.reminderTime,
                        intervalMinutes = if (task.intervalMinutes > 0) task.intervalMinutes else 1440,
                        meetingLink = task.meetingLink ?: "",
                        location = task.location ?: "",
                        priority = task.priority,
                        notes = task.notes ?: ""
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

    fun updateEventDate(date: LocalDate) {
        _uiState.update {
            // If reminder date was after new event date, adjust reminder date
            val adjustedReminderDate = if (it.reminderDate.isAfter(date)) date else it.reminderDate
            it.copy(date = date, reminderDate = adjustedReminderDate, error = null)
        }
        validateSchedule()
    }

    fun updateEventTime(time: LocalTime?) {
        _uiState.update { it.copy(time = time, error = null) }
        validateSchedule()
    }

    fun updateReminderDate(date: LocalDate) {
        _uiState.update { it.copy(reminderDate = date, error = null) }
        validateSchedule()
    }

    fun updateReminderTime(time: LocalTime?) {
        _uiState.update { it.copy(reminderTime = time, error = null) }
        validateSchedule()
    }

    fun updateInterval(minutes: Int) {
        _uiState.update { it.copy(intervalMinutes = minutes, error = null) }
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

    private fun validateSchedule(): Boolean {
        val state = _uiState.value
        val now = LocalDateTime.now()
        val eventDateTime = state.getEventDateTime()
        val reminderStart = state.getReminderStartDateTime()

        // 1. Check if event is in the past
        if (!eventDateTime.isAfter(now)) {
            _uiState.update { it.copy(error = "Event date and time must be in the future.") }
            return false
        }

        // 2. Check if reminder start is after event time
        if (reminderStart.isAfter(eventDateTime)) {
            _uiState.update { it.copy(error = "Reminder start time cannot be later than the event deadline.") }
            return false
        }

        // 3. Inform user if reminder start is in the past
        if (reminderStart.isBefore(now)) {
            _uiState.update {
                it.copy(
                    error = null,
                    infoMessage = "Note: Reminder start time has already passed. Notifications will begin immediately until the event deadline."
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
                _uiState.update { it.copy(error = "Title is required.") }
                return@launch
            }

            if (!validateSchedule()) {
                return@launch
            }

            _uiState.update { it.copy(isSaving = true, error = null) }

            val task = TaskEntity(
                id = taskId ?: 0L,
                title = state.title.trim(),
                company = state.company.trim(),
                type = state.type,
                date = state.date,
                time = state.time,
                reminderDate = state.reminderDate,
                reminderTime = state.reminderTime,
                intervalMinutes = state.intervalMinutes,
                meetingLink = state.meetingLink.takeIf { it.isNotBlank() }?.trim(),
                location = state.location.takeIf { it.isNotBlank() }?.trim(),
                priority = state.priority,
                notes = state.notes.takeIf { it.isNotBlank() }?.trim()
            )

            val id = if (taskId != null) {
                dao.updateTask(task)
                taskId
            } else {
                dao.insertTask(task)
            }

            val savedTask = task.copy(id = id)
            if (!savedTask.completed && savedTask.getEventDateTime().isAfter(LocalDateTime.now())) {
                alarmScheduler.scheduleTaskReminder(savedTask)
            } else {
                alarmScheduler.cancelTaskReminder(id)
            }

            _uiState.update { it.copy(isSaving = false, isSaved = true) }
        }
    }
}
