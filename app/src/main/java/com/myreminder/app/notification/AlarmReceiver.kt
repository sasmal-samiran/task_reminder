package com.myreminder.app.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.myreminder.app.data.local.AppDatabase
import com.myreminder.app.data.local.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class AlarmReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "MyReminderReceiver"
        private val DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }

    override fun onReceive(context: Context, intent: Intent) {
        val now = LocalDateTime.now()
        val action = intent.action
        val taskId = intent.getLongExtra("TASK_ID", -1L)

        Log.d(TAG, "==================================================")
        Log.d(TAG, "ALARM TRIGGERED:")
        Log.d(TAG, "  Trigger Time: ${now.format(DATE_TIME_FORMAT)}")
        Log.d(TAG, "  Action:       $action")
        Log.d(TAG, "  Task ID:      $taskId")
        Log.d(TAG, "==================================================")

        if (action != "com.myreminder.app.TASK_REMINDER" || taskId == -1L) {
            Log.w(TAG, "Invalid action ($action) or missing task ID ($taskId). Ignoring.")
            return
        }

        val pendingResult = goAsync()

        scope.launch {
            try {
                val settings = SettingsDataStore(context)
                val enabled = settings.getNotificationsEnabledSync()
                if (!enabled) {
                    Log.d(TAG, "Notifications are disabled in Settings. Skipping notification delivery for Task ID: $taskId")
                    return@launch
                }

                val db = AppDatabase.getInstance(context)
                val task = db.taskDao().getTaskById(taskId)

                if (task == null) {
                    Log.w(TAG, "Task ID: $taskId not found in database. It might have been deleted.")
                    return@launch
                }

                if (task.completed) {
                    Log.d(TAG, "Task ID: $taskId ('${task.title}') is marked completed. Skipping notification.")
                    return@launch
                }

                // 1. Deliver the notification
                val notificationHelper = NotificationHelper(context)
                notificationHelper.showTaskReminder(task)
                Log.d(TAG, "NOTIFICATION DELIVERED for Task ID: ${task.id} ('${task.title}', Priority: ${task.priority.displayName})")

                // 2. Determine and schedule the next repeated alarm in the interval chain
                val eventDateTime = task.getEventDateTime()
                val currentNow = LocalDateTime.now()

                if (currentNow.isBefore(eventDateTime)) {
                    val intervalMinutes = if (task.intervalMinutes > 0) task.intervalMinutes else 1440
                    val potentialNext = currentNow.plusMinutes(intervalMinutes.toLong())

                    // Cap next alarm at the event deadline
                    val finalNextAlarm = if (potentialNext.isBefore(eventDateTime)) potentialNext else eventDateTime
                    val epochMillis = finalNextAlarm.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

                    val scheduler = AlarmScheduler(context)
                    if (epochMillis > System.currentTimeMillis()) {
                        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                        val nextIntent = Intent(context, AlarmReceiver::class.java).apply {
                            action = "com.myreminder.app.TASK_REMINDER"
                            data = Uri.parse("custom://myreminder/task/${task.id}")
                            putExtra("TASK_ID", task.id)
                        }
                        val pendingIntent = PendingIntent.getBroadcast(
                            context,
                            task.id.toInt(),
                            nextIntent,
                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                        )

                        val diffMinutes = Duration.between(currentNow, finalNextAlarm).toMinutes()
                        val diffSeconds = Duration.between(currentNow, finalNextAlarm).seconds
                        val canExact = scheduler.canScheduleExactAlarms()

                        Log.d(TAG, "SCHEDULING NEXT REPEATED ALARM in interval chain:")
                        Log.d(TAG, "  Task ID:          ${task.id} ('${task.title}')")
                        Log.d(TAG, "  Interval:         ${intervalMinutes}m (${task.getIntervalDisplayName()})")
                        Log.d(TAG, "  Next Alarm Time:  ${finalNextAlarm.format(DATE_TIME_FORMAT)} (in ${diffMinutes}m / ${diffSeconds}s)")
                        Log.d(TAG, "  Event Deadline:   ${eventDateTime.format(DATE_TIME_FORMAT)}")
                        Log.d(TAG, "  Exact Permitted:  $canExact")

                        try {
                            if (canExact) {
                                alarmManager.setExactAndAllowWhileIdle(
                                    AlarmManager.RTC_WAKEUP,
                                    epochMillis,
                                    pendingIntent
                                )
                            } else {
                                alarmManager.setAndAllowWhileIdle(
                                    AlarmManager.RTC_WAKEUP,
                                    epochMillis,
                                    pendingIntent
                                )
                            }
                        } catch (e: SecurityException) {
                            Log.e(TAG, "SecurityException scheduling next alarm for Task ID: ${task.id}", e)
                            alarmManager.setAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                epochMillis,
                                pendingIntent
                            )
                        }
                    }
                } else {
                    Log.d(TAG, "Task ID: ${task.id} ('${task.title}') has reached its event deadline ($eventDateTime). No more alarms.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in AlarmReceiver for Task ID: $taskId", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
