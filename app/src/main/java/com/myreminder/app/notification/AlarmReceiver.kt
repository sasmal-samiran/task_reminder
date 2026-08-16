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

class AlarmReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.myreminder.app.TASK_REMINDER") return

        val taskId = intent.getLongExtra("TASK_ID", -1L)
        Log.d("AlarmReceiver", "Alarm received for taskId: $taskId")
        if (taskId == -1L) return

        val pendingResult = goAsync()

        scope.launch {
            try {
                val settings = SettingsDataStore(context)
                if (!settings.getNotificationsEnabledSync()) {
                    Log.d("AlarmReceiver", "Notifications are disabled in settings. Skipping.")
                    return@launch
                }

                val db = AppDatabase.getInstance(context)
                val task = db.taskDao().getTaskById(taskId)

                if (task != null && !task.completed) {
                    val now = LocalDateTime.now()
                    val eventDateTime = task.getEventDateTime()

                    // Only show notification if event time hasn't passed
                    if (now.isBefore(eventDateTime) || now.isEqual(eventDateTime)) {
                        val soundUri = settings.getNotificationSoundUriSync()
                        val notificationHelper = NotificationHelper(context)
                        notificationHelper.showTaskReminder(task, soundUri)
                        Log.d("AlarmReceiver", "Showed notification for task $taskId: ${task.title}")

                        // Chain and schedule the next repeat interval reminder
                        val scheduler = AlarmScheduler(context)
                        scheduler.scheduleTaskReminder(task, isNextChainedAlarm = true)
                    } else {
                        Log.d("AlarmReceiver", "Task $taskId event deadline has passed.")
                    }
                } else {
                    Log.d("AlarmReceiver", "Task $taskId is null or already completed.")
                }
            } catch (e: Exception) {
                Log.e("AlarmReceiver", "Error processing reminder alarm: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
