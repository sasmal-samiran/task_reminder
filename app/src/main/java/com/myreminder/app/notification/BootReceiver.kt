package com.myreminder.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.myreminder.app.data.local.AppDatabase
import com.myreminder.app.data.local.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

class BootReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val validActions = listOf(
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.LOCKED_BOOT_COMPLETED",
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON"
        )

        if (intent.action !in validActions) return

        val pendingResult = goAsync()

        scope.launch {
            try {
                val scheduler = AlarmScheduler(context)
                val settings = SettingsDataStore(context)

                android.util.Log.d("BootReceiver", "BootReceiver triggered: restoring alarms (notificationsEnabled=${settings.getNotificationsEnabledSync()})")

                if (settings.getNotificationsEnabledSync()) {
                    // Restore morning summary alarm
                    val hour = settings.getMorningHourSync()
                    val minute = settings.getMorningMinuteSync()
                    try {
                        scheduler.scheduleMorningSummary(hour, minute)
                        android.util.Log.d("BootReceiver", "Restored morning summary alarm for $hour:$minute")
                    } catch (ex: Exception) {
                        android.util.Log.e("BootReceiver", "Failed to restore morning summary alarm: ${ex.message}", ex)
                    }
                    // Restore all task reminder alarms
                    val db = AppDatabase.getInstance(context)
                    val today = LocalDate.now()
                    val upcomingTasks = db.taskDao().getTasksWithReminders(today)
                    val now = LocalDateTime.now()
                    upcomingTasks.forEach { task ->
                        try {
                            if (!task.completed && task.getEventDateTime().isAfter(now)) {
                                scheduler.scheduleTaskReminder(task)
                                android.util.Log.d("BootReceiver", "Restored alarm for task=${task.id} title='${task.title}'")
                            }
                        } catch (ex: Exception) {
                            android.util.Log.e("BootReceiver", "Failed to restore alarm for task=${task.id}: ${ex.message}", ex)
                        }
                    }
                } else {
                    android.util.Log.d("BootReceiver", "Notifications disabled in settings; skipping alarm restoration")
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
