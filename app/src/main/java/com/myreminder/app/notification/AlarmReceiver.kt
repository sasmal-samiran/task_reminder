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
        if (intent.action != "com.myreminder.app.TASK_REMINDER") return

        val taskId = intent.getLongExtra("TASK_ID", -1L)
        if (taskId == -1L) return

        val pendingResult = goAsync()

        scope.launch {
            try {
                val settings = SettingsDataStore(context)
                if (!settings.getNotificationsEnabledSync()) return@launch

                val db = AppDatabase.getInstance(context)
                val task = db.taskDao().getTaskById(taskId)

                if (task != null && !task.completed) {
                    val notificationHelper = NotificationHelper(context)
                    notificationHelper.showTaskReminder(task)

                    // Check if we should schedule the next interval alarm
                    val now = LocalDateTime.now()
                    val eventDateTime = task.getEventDateTime()

                    if (now.isBefore(eventDateTime)) {
                        val intervalMinutes = if (task.intervalMinutes > 0) task.intervalMinutes else 1440
                        val nextAlarmTime = now.plusMinutes(intervalMinutes.toLong())

                        // Capped at eventDateTime
                        val finalNextAlarm = if (nextAlarmTime.isBefore(eventDateTime)) nextAlarmTime else eventDateTime
                        val epochMillis = finalNextAlarm.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

                        val scheduler = AlarmScheduler(context)
                        if (epochMillis > System.currentTimeMillis() && scheduler.canScheduleExactAlarms()) {
                            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                            val nextIntent = Intent(context, AlarmReceiver::class.java).apply {
                                action = "com.myreminder.app.TASK_REMINDER"
                                putExtra("TASK_ID", task.id)
                            }
                            val pendingIntent = android.app.PendingIntent.getBroadcast(
                                context,
                                task.id.toInt(),
                                nextIntent,
                                android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
                            )
                            alarmManager.setExactAndAllowWhileIdle(
                                android.app.AlarmManager.RTC_WAKEUP,
                                epochMillis,
                                pendingIntent
                            )
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
