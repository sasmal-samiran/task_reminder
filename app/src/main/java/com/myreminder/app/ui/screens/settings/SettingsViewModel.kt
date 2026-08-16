package com.myreminder.app.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.myreminder.app.data.local.SettingsDataStore
import com.myreminder.app.notification.AlarmScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val settings = SettingsDataStore(application)
    private val alarmScheduler = AlarmScheduler(application)

    val morningHour: StateFlow<Int> = settings.morningReminderHour
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 7)
        
    val morningMinute: StateFlow<Int> = settings.morningReminderMinute
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
        
    val defaultReminderMinutes: StateFlow<Int> = settings.defaultReminderMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 30)
        
    val notificationsEnabled: StateFlow<Boolean> = settings.notificationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setMorningTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            settings.setMorningReminderTime(hour, minute)
            if (notificationsEnabled.value) {
                alarmScheduler.scheduleMorningSummary(hour, minute)
            }
        }
    }

    fun setDefaultReminder(minutes: Int) {
        viewModelScope.launch {
            settings.setDefaultReminderMinutes(minutes)
        }
    }

    fun setNotifications(enabled: Boolean) {
        viewModelScope.launch {
            settings.setNotificationsEnabled(enabled)
            if (enabled) {
                alarmScheduler.scheduleMorningSummary(morningHour.value, morningMinute.value)
            } else {
                alarmScheduler.cancelMorningSummary()
                // Might also want to cancel all task reminders, or keep it simple
            }
        }
    }
}
