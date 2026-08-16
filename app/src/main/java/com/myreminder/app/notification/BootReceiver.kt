package com.myreminder.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.myreminder.app.data.local.AppDatabase
import com.myreminder.app.data.local.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class BootReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "MyReminderBoot"
        private val DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }

    override fun onReceive(context: Context, intent: Intent) {
        val bootAction = intent.action
        val now = LocalDateTime.now()

        Log.d(TAG, "==================================================")
        Log.d(TAG, "DEVICE BOOT / RESTART DETECTED:")
        Log.d(TAG, "  Action:    $bootAction")
        Log.d(TAG, "  Boot Time: ${now.format(DATE_TIME_FORMAT)}")
        Log.d(TAG, "==================================================")

        val validActions = listOf(
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.LOCKED_BOOT_COMPLETED",
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON"
        )

        if (bootAction !in validActions) {
            Log.w(TAG, "Received unhandled action: $bootAction. Skipping.")
            return
        }

        val pendingResult = goAsync()

        scope.launch {
            try {
                val scheduler = AlarmScheduler(context)
                val settings = SettingsDataStore(context)
                val notificationsEnabled = settings.getNotificationsEnabledSync()

                if (!notificationsEnabled) {
                    Log.d(TAG, "Notifications are disabled in Settings. Skipping alarm restoration.")
                    return@launch
                }

                // 1. Restore Morning Summary
                val hour = settings.getMorningHourSync()
                val minute = settings.getMorningMinuteSync()
                scheduler.scheduleMorningSummary(hour, minute)
                Log.d(TAG, "Morning Summary restored for $hour:${minute.toString().padStart(2, '0')}")

                // 2. Query all active incomplete tasks from database
                val db = AppDatabase.getInstance(context)
                val activeTasks = db.taskDao().getActiveIncompleteTasks()
                val currentNow = LocalDateTime.now()
                var restoredCount = 0

                activeTasks.forEach { task ->
                    if (!task.completed && task.getEventDateTime().isAfter(currentNow)) {
                        scheduler.scheduleTaskReminder(task)
                        restoredCount++
                    }
                }

                Log.d(TAG, "Successfully restored $restoredCount active task reminder alarm(s) on reboot.")
            } catch (e: Exception) {
                Log.e(TAG, "Error restoring alarms on boot", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
