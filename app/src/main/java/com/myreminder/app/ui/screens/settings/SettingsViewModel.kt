package com.myreminder.app.ui.screens.settings

import android.app.Application
import android.media.RingtoneManager
import android.net.Uri
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

    val defaultReminderHour: StateFlow<Int> = settings.defaultReminderHour
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 7)

    val defaultReminderMinute: StateFlow<Int> = settings.defaultReminderMinute
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val notificationSoundUri: StateFlow<String> = settings.notificationSoundUri
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val notificationSoundName: StateFlow<String> = settings.notificationSoundName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Default Notification Sound")

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

    fun setDefaultReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            settings.setDefaultReminderTime(hour, minute)
        }
    }

    fun setNotificationSound(uri: String, name: String) {
        viewModelScope.launch {
            settings.setNotificationSound(uri, name)
            com.myreminder.app.notification.NotificationHelper(getApplication()).createChannels(uri)
        }
    }

    fun setNotifications(enabled: Boolean) {
        viewModelScope.launch {
            settings.setNotificationsEnabled(enabled)
            if (enabled) {
                alarmScheduler.scheduleMorningSummary(morningHour.value, morningMinute.value)
            } else {
                alarmScheduler.cancelMorningSummary()
            }
        }
    }

    /**
     * Gets the display name for a ringtone URI.
     */
    fun getRingtoneName(uriString: String): String {
        if (uriString.isBlank()) return "Default Notification Sound"
        return try {
            val uri = Uri.parse(uriString)
            val ringtone = RingtoneManager.getRingtone(getApplication(), uri)
            ringtone?.getTitle(getApplication()) ?: "Unknown Sound"
        } catch (_: Exception) {
            "Unknown Sound"
        }
    }
}
