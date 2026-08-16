package com.myreminder.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        val MORNING_REMINDER_HOUR = intPreferencesKey("morning_reminder_hour")
        val MORNING_REMINDER_MINUTE = intPreferencesKey("morning_reminder_minute")
        val DEFAULT_REMINDER_MINUTES = intPreferencesKey("default_reminder_minutes")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val CUSTOM_NOTIFICATION_SOUND_URI = stringPreferencesKey("custom_notification_sound_uri")
    }

    val morningReminderHour: Flow<Int> = context.settingsDataStore.data.map { it[MORNING_REMINDER_HOUR] ?: 7 }
    val morningReminderMinute: Flow<Int> = context.settingsDataStore.data.map { it[MORNING_REMINDER_MINUTE] ?: 0 }
    val defaultReminderMinutes: Flow<Int> = context.settingsDataStore.data.map { it[DEFAULT_REMINDER_MINUTES] ?: 1440 }
    val notificationsEnabled: Flow<Boolean> = context.settingsDataStore.data.map { it[NOTIFICATIONS_ENABLED] ?: true }
    val customNotificationSoundUri: Flow<String?> = context.settingsDataStore.data.map { it[CUSTOM_NOTIFICATION_SOUND_URI] }

    suspend fun setMorningReminderTime(hour: Int, minute: Int) {
        context.settingsDataStore.edit {
            it[MORNING_REMINDER_HOUR] = hour
            it[MORNING_REMINDER_MINUTE] = minute
        }
    }

    suspend fun setDefaultReminderMinutes(minutes: Int) {
        context.settingsDataStore.edit { it[DEFAULT_REMINDER_MINUTES] = minutes }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setCustomNotificationSoundUri(uriString: String?) {
        context.settingsDataStore.edit {
            if (uriString != null) {
                it[CUSTOM_NOTIFICATION_SOUND_URI] = uriString
            } else {
                it.remove(CUSTOM_NOTIFICATION_SOUND_URI)
            }
        }
    }

    // Synchronous getters for use in BroadcastReceivers & Notifications
    suspend fun getNotificationsEnabledSync(): Boolean {
        return context.settingsDataStore.data.map { it[NOTIFICATIONS_ENABLED] ?: true }.first()
    }

    suspend fun getMorningHourSync(): Int {
        return context.settingsDataStore.data.map { it[MORNING_REMINDER_HOUR] ?: 7 }.first()
    }

    suspend fun getMorningMinuteSync(): Int {
        return context.settingsDataStore.data.map { it[MORNING_REMINDER_MINUTE] ?: 0 }.first()
    }

    suspend fun getCustomNotificationSoundUriSync(): String? {
        return context.settingsDataStore.data.map { it[CUSTOM_NOTIFICATION_SOUND_URI] }.first()
    }
}
