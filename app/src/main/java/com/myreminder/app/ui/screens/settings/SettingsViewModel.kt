package com.myreminder.app.ui.screens.settings

import android.app.Application
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.myreminder.app.data.local.SettingsDataStore
import com.myreminder.app.notification.AlarmScheduler
import com.myreminder.app.notification.NotificationHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val settings = SettingsDataStore(application)
    private val alarmScheduler = AlarmScheduler(application)
    private var previewRingtone: Ringtone? = null

    val morningHour: StateFlow<Int> = settings.morningReminderHour
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 7)

    val morningMinute: StateFlow<Int> = settings.morningReminderMinute
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val defaultReminderMinutes: StateFlow<Int> = settings.defaultReminderMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1440)

    val notificationsEnabled: StateFlow<Boolean> = settings.notificationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val customSoundUri: StateFlow<String?> = settings.customNotificationSoundUri
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isPlayingPreview = MutableStateFlow(false)
    val isPlayingPreview: StateFlow<Boolean> = _isPlayingPreview

    val soundTitle: StateFlow<String> = customSoundUri.map { uriStr ->
        if (uriStr.isNullOrBlank()) {
            "Default Notification Sound"
        } else {
            try {
                val uri = Uri.parse(uriStr)
                val ringtone = RingtoneManager.getRingtone(getApplication(), uri)
                ringtone?.getTitle(getApplication()) ?: "Custom Sound"
            } catch (e: Exception) {
                "Default Notification Sound"
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Default Notification Sound")

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
            }
        }
    }

    fun setCustomSound(uri: Uri?) {
        viewModelScope.launch {
            stopPreview()
            val uriString = uri?.toString()
            settings.setCustomNotificationSoundUri(uriString)
            // Re-create notification channels with the new sound
            NotificationHelper(getApplication()).createChannels()
        }
    }

    fun togglePlayPreview() {
        if (_isPlayingPreview.value) {
            stopPreview()
        } else {
            playPreview()
        }
    }

    private fun playPreview() {
        try {
            stopPreview()
            val uriStr = customSoundUri.value
            val soundUri = if (!uriStr.isNullOrBlank()) {
                Uri.parse(uriStr)
            } else {
                Settings.System.DEFAULT_NOTIFICATION_URI
            }
            previewRingtone = RingtoneManager.getRingtone(getApplication(), soundUri)
            previewRingtone?.play()
            _isPlayingPreview.value = true
        } catch (e: Exception) {
            _isPlayingPreview.value = false
        }
    }

    fun stopPreview() {
        try {
            previewRingtone?.stop()
        } catch (e: Exception) {
            // Ignore
        }
        previewRingtone = null
        _isPlayingPreview.value = false
    }

    override fun onCleared() {
        super.onCleared()
        stopPreview()
    }
}
