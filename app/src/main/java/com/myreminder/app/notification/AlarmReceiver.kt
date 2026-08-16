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
import java.time.LocalDateTime

class AlarmReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        android.util.Log.d("AlarmReceiver", "onReceive action=$action at ${LocalDateTime.now()}")
        if (action != "com.myreminder.app.TASK_REMINDER") return

        val taskId = intent.getLongExtra("TASK_ID", -1L)
        if (taskId == -1L) {
            android.util.Log.w("AlarmReceiver", "onReceive missing TASK_ID")
            return
        }
        val pendingResult = goAsync()
        scope.launch {
            try {
                try {
                    val settings = SettingsDataStore(context)
                    if (!settings.getNotificationsEnabledSync()) {
                        android.util.Log.d("AlarmReceiver", "Notifications disabled in settings; aborting for task=$taskId")
                        return@launch
                    }
                    val db = AppDatabase.getInstance(context)
                    val task = db.taskDao().getTaskById(taskId)
                    if (task != null && !task.completed) {
                        android.util.Log.d("AlarmReceiver", "Triggering reminder for task=${task.id} title='${task.title}'")
                        // Show the notification
                        val soundUri = settings.getNotificationSoundUriSync()
                        val notificationHelper = NotificationHelper(context)
                        try {
                            val shown = notificationHelper.showTaskReminder(task, soundUri)
                            android.util.Log.d("AlarmReceiver", "showTaskReminder returned=$shown for task=${task.id}")
                        } catch (ex: Exception) {
                            android.util.Log.e("AlarmReceiver", "Exception while showing notification for task=${task.id}: ${ex.message}", ex)
                        }
                        // Schedule the next repeated alarm if event time hasn't passed
                        val now = LocalDateTime.now()
                        val eventDateTime = task.getEventDateTime()
                        if (now.isBefore(eventDateTime)) {
                            // Use the AlarmScheduler to compute and schedule the next interval step
                            val scheduler = AlarmScheduler(context)
                            try {
                                scheduler.scheduleTaskReminder(task)
                                android.util.Log.d("AlarmReceiver", "Scheduled next reminder for task=${task.id}")
                            } catch (ex: Exception) {
                                android.util.Log.e("AlarmReceiver", "Failed to schedule next reminder for task=${task.id}: ${ex.message}", ex)
                            }
                        } else {
                            android.util.Log.d("AlarmReceiver", "Event time passed for task=${task.id}; not scheduling further reminders")
                        }
                    } else {
                        android.util.Log.d("AlarmReceiver", "Task not found or already completed: id=$taskId")
                    }
                } catch (ex: Exception) {
                    android.util.Log.e("AlarmReceiver", "Unexpected error handling alarm for task=$taskId: ${ex.message}", ex)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
