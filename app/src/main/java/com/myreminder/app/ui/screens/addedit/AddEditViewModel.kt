package com.myreminder.app.ui.screens.addedit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.myreminder.app.data.local.AppDatabase
import com.myreminder.app.data.local.SettingsDataStore
import com.myreminder.app.data.local.TaskEntity
import com.myreminder.app.data.model.Priority
import com.myreminder.app.data.model.ReminderOption
import com.myreminder.app.data.model.TaskType
import com.myreminder.app.notification.AlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

data class AddEditUiState(
    val title: String = "",
    val company: String = "",
    val type: TaskType = TaskType.INTERVIEW,
    val date: LocalDate = LocalDate.now(),
    val time: LocalTime? = null,
    val meetingLink: String = "",
    val location: String = "",
    val priority: Priority = Priority.MEDIUM,
    val notes: String = "",
    val reminderMinutes: Int = ReminderOption.MIN_30.minutesBefore,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

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
            _uiState.update { it.copy(reminderMinutes = defaultRem) }
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
                        reminderMinutes = task.reminderMinutes
                    )
                }
            }
        }
    }
    
    fun updateTitle(title: String) = _uiState.update { it.copy(title = title) }
    fun updateCompany(company: String) = _uiState.update { it.copy(company = company) }
    fun updateType(type: TaskType) = _uiState.update { it.copy(type = type) }
    fun updateDate(date: LocalDate) = _uiState.update { it.copy(date = date) }
    fun updateTime(time: LocalTime?) = _uiState.update { it.copy(time = time) }
    fun updateMeetingLink(link: String) = _uiState.update { it.copy(meetingLink = link) }
    fun updateLocation(location: String) = _uiState.update { it.copy(location = location) }
    fun updatePriority(priority: Priority) = _uiState.update { it.copy(priority = priority) }
    fun updateNotes(notes: String) = _uiState.update { it.copy(notes = notes) }
    fun updateReminder(minutes: Int) = _uiState.update { it.copy(reminderMinutes = minutes) }

    fun saveTask() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.title.isBlank()) {
                _uiState.update { it.copy(error = "Title is required") }
                return@launch
            }
            
            _uiState.update { it.copy(isSaving = true) }
            
            val task = TaskEntity(
                id = taskId ?: 0L,
                title = state.title,
                company = state.company,
                type = state.type,
                date = state.date,
                time = state.time,
                meetingLink = state.meetingLink.takeIf { it.isNotBlank() },
                location = state.location.takeIf { it.isNotBlank() },
                priority = state.priority,
                notes = state.notes.takeIf { it.isNotBlank() },
                reminderMinutes = state.reminderMinutes
            )
            
            val id = if (taskId != null) {
                dao.updateTask(task)
                taskId
            } else {
                dao.insertTask(task)
            }
            
            val savedTask = task.copy(id = id)
            if (savedTask.reminderMinutes >= 0 && !savedTask.completed) {
                alarmScheduler.scheduleTaskReminder(savedTask)
            } else {
                alarmScheduler.cancelTaskReminder(id)
            }
            
            _uiState.update { it.copy(isSaving = false, isSaved = true) }
        }
    }
}
