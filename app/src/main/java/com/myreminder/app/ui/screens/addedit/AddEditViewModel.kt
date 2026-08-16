package com.myreminder.app.ui.screens.addedit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.myreminder.app.data.local.AppDatabase
import com.myreminder.app.data.local.SettingsDataStore
import com.myreminder.app.data.local.TaskEntity
import com.myreminder.app.data.model.Priority
import com.myreminder.app.data.model.ReminderInterval
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
    val type: TaskType = TaskType.DEADLINE,
    // Event date & time (when the task/event is due)
    val eventDate: LocalDate = LocalDate.now().plusDays(1),
    val eventTime: LocalTime? = LocalTime.of(18, 0),
    // Reminder start date & time (when notifications begin)
    val reminderDate: LocalDate = LocalDate.now(),
    val reminderTime: LocalTime = LocalTime.of(7, 0),
    // Reminder interval
    val reminderInterval: ReminderInterval = ReminderInterval.DAY_1,
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
        val t = eventTime ?: LocalTime.of(23, 59)
        return LocalDateTime.of(eventDate, t)
    }

    fun getReminderStartDateTime(): LocalDateTime {
        return LocalDateTime.of(reminderDate, reminderTime)
    }

    fun getIntervalExplanation(): String {
        val eventDt = getEventDateTime()
        val reminderDt = getReminderStartDateTime()
        if (reminderDt.isAfter(eventDt)) return "⚠️ Reminder start cannot be after the event."
        val interval = reminderInterval.displayName.lowercase()
        return "Notifications will start at ${reminderTime.format(DateTimeFormatter.ofPattern("h:mm a"))} on ${reminderDate.format(DateTimeFormatter.ofPattern("d MMM"))} and repeat every $interval until the event."
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
            val defHour = settings.defaultReminderHour.first()
            val defMinute = settings.defaultReminderMinute.first()
            _uiState.update {
                it.copy(
                    reminderTime = LocalTime.of(defHour, defMinute)
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
                        eventDate = task.date,
                        eventTime = task.time,
                        reminderDate = task.reminderDate ?: task.date.minusDays(1),
                        reminderTime = task.reminderTime ?: LocalTime.of(7, 0),
                        reminderInterval = ReminderInterval.fromMinutes(task.reminderIntervalMinutes),
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
            val newReminderDate = if (it.reminderDate.isAfter(date)) date else it.reminderDate
            it.copy(eventDate = date, reminderDate = newReminderDate, error = null)
        }
        validateAll()
    }

    fun updateEventTime(time: LocalTime?) {
        _uiState.update { it.copy(eventTime = time, error = null) }
        validateAll()
    }

    fun updateReminderDate(date: LocalDate) {
        _uiState.update { it.copy(reminderDate = date, error = null) }
        validateAll()
    }

    fun updateReminderTime(time: LocalTime) {
        _uiState.update { it.copy(reminderTime = time, error = null) }
        validateAll()
    }

    fun updateReminderInterval(interval: ReminderInterval) {
        _uiState.update { it.copy(reminderInterval = interval, error = null) }
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

    private fun validateAll(): Boolean {
        val state = _uiState.value
        val now = LocalDateTime.now()
        val eventDateTime = state.getEventDateTime()
        val reminderDateTime = state.getReminderStartDateTime()

        // 1. Event datetime must be in the future
        if (!eventDateTime.isAfter(now)) {
            _uiState.update { it.copy(
                error = "Event date & time must be in the future. Current time: ${now.format(DateTimeFormatter.ofPattern("d MMM, h:mm a"))}.",
                infoMessage = null
            )}
            return false
        }

        // 2. Reminder start cannot be after event
        if (reminderDateTime.isAfter(eventDateTime)) {
            _uiState.update { it.copy(
                error = "Reminder start date & time cannot be after the event date & time.",
                infoMessage = null
            )}
            return false
        }

        // 3. If reminder start is in the past, notify user that alerts begin immediately
        if (reminderDateTime.isBefore(now)) {
            _uiState.update { it.copy(
                error = null,
                infoMessage = "Note: Reminder start time has arrived. Notifications will begin immediately."
            )}
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

            if (!validateAll()) return@launch

            _uiState.update { it.copy(isSaving = true, error = null) }

            val task = TaskEntity(
                id = taskId ?: 0L,
                title = state.title.trim(),
                company = state.company.trim(),
                type = state.type,
                date = state.eventDate,
                time = state.eventTime,
                meetingLink = state.meetingLink.takeIf { it.isNotBlank() }?.trim(),
                location = state.location.takeIf { it.isNotBlank() }?.trim(),
                priority = state.priority,
                notes = state.notes.takeIf { it.isNotBlank() }?.trim(),
                reminderDate = state.reminderDate,
                reminderTime = state.reminderTime,
                reminderIntervalMinutes = state.reminderInterval.minutes,
                // Keep backward-compat fields populated
                reminderDurationValue = 1,
                reminderDurationUnit = "DAYS",
                reminderMinutes = 1440
            )

            val id = if (taskId != null) {
                dao.updateTask(task)
                taskId
            } else {
                dao.insertTask(task)
            }

            val savedTask = task.copy(id = id)
            if (!savedTask.completed) {
                alarmScheduler.scheduleTaskReminder(savedTask)
            } else {
                alarmScheduler.cancelTaskReminder(id)
            }

            _uiState.update { it.copy(isSaving = false, isSaved = true) }
        }
    }
}
