package com.myreminder.app.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.myreminder.app.data.local.AppDatabase
import com.myreminder.app.notification.AlarmScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getInstance(application).taskDao()
    private val alarmScheduler = AlarmScheduler(application)

    val todayTasks = dao.getTasksByDate(LocalDate.now())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tomorrowTasks = dao.getTasksByDate(LocalDate.now().plusDays(1))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val upcomingTasks = dao.getTasksByDateRange(
        LocalDate.now().plusDays(2),
        LocalDate.now().plusDays(8)
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleComplete(taskId: Long, completed: Boolean) {
        viewModelScope.launch {
            dao.setCompleted(taskId, completed)
            val task = dao.getTaskById(taskId)
            if (task != null) {
                if (completed) {
                    alarmScheduler.cancelTaskReminder(taskId)
                } else {
                    alarmScheduler.scheduleTaskReminder(task)
                }
            }
        }
    }
}
