package com.myreminder.app.ui.screens.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.myreminder.app.data.local.AppDatabase
import com.myreminder.app.data.local.TaskEntity
import com.myreminder.app.notification.AlarmScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).taskDao()
    private val alarmScheduler = AlarmScheduler(application)
    private val taskId: Long = savedStateHandle.get<Long>("taskId") ?: 0L

    val task: StateFlow<TaskEntity?> = dao.getTaskByIdFlow(taskId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun toggleComplete() {
        viewModelScope.launch {
            val t = task.value ?: return@launch
            val newStatus = !t.completed
            dao.setCompleted(t.id, newStatus)
            if (newStatus) {
                alarmScheduler.cancelTaskReminder(t.id)
            } else {
                alarmScheduler.scheduleTaskReminder(t)
            }
        }
    }

    fun deleteTask(onDeleted: () -> Unit) {
        viewModelScope.launch {
            val t = task.value ?: return@launch
            dao.deleteTask(t)
            alarmScheduler.cancelTaskReminder(t.id)
            onDeleted()
        }
    }
}
