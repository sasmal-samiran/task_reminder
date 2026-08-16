package com.myreminder.app.ui.screens.calendar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.myreminder.app.data.local.AppDatabase
import com.myreminder.app.data.local.TaskEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.YearMonth

class CalendarViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getInstance(application).taskDao()

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = _currentMonth

    private val _selectedDate = MutableStateFlow<LocalDate?>(LocalDate.now())
    val selectedDate: StateFlow<LocalDate?> = _selectedDate

    @OptIn(ExperimentalCoroutinesApi::class)
    val taskDatesInMonth: StateFlow<List<LocalDate>> = _currentMonth.flatMapLatest { month ->
        val start = month.atDay(1)
        val end = month.atEndOfMonth()
        dao.getTaskDatesInRange(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedDateTasks: StateFlow<List<TaskEntity>> = _selectedDate.flatMapLatest { date ->
        if (date == null) flowOf(emptyList()) else dao.getTasksByDate(date)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        if (YearMonth.from(date) != _currentMonth.value) {
            _currentMonth.value = YearMonth.from(date)
        }
    }

    fun nextMonth() {
        _currentMonth.update { it.plusMonths(1) }
    }

    fun prevMonth() {
        _currentMonth.update { it.minusMonths(1) }
    }
}
