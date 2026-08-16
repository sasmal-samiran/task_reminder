package com.myreminder.app.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.myreminder.app.data.local.TaskEntity
import java.time.LocalDateTime
import java.time.ZoneId

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Schedules the next reminder alarm for a task using the new interval-based model.
     *
     * Notifications fire from reminderStartDateTime, repeating every reminderIntervalMinutes,
     * until eventDateTime is reached. Each alarm is one-shot; the AlarmReceiver chains the next.
     */
    fun scheduleTaskReminder(task: TaskEntity) {
        if (task.completed) return

        val now = LocalDateTime.now()
        val eventDateTime = task.getEventDateTime()

        // If event time has already passed, no alarm needed
        if (!eventDateTime.isAfter(now)) return

        val reminderStart = task.getReminderStartDateTime()
        val intervalMinutes = task.reminderIntervalMinutes.toLong().coerceAtLeast(1)

        val nextAlarmTime: LocalDateTime = when {
            // Case 1: Reminder start is in the future -> schedule at reminder start
            now.isBefore(reminderStart) -> reminderStart

            // Case 2: Currently between reminder start and event time
            // Find the next interval step: reminderStart + k * interval > now
            else -> {
                val minutesSinceStart = java.time.Duration.between(reminderStart, now).toMinutes()
                val completedIntervals = minutesSinceStart / intervalMinutes
                val nextStep = reminderStart.plusMinutes((completedIntervals + 1) * intervalMinutes)

                // If next step is beyond event time, schedule at event time (final reminder)
                if (nextStep.isAfter(eventDateTime)) {
                    // Only schedule at event time if we haven't passed it
                    if (now.isBefore(eventDateTime)) eventDateTime else return
                } else {
                    nextStep
                }
            }
        }

        val epochMillis = nextAlarmTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        // Ensure we schedule in the future (allow small clock skews)
        val nowMs = System.currentTimeMillis()
        var scheduledMillis = epochMillis
        if (scheduledMillis <= nowMs + 500) {
            android.util.Log.w("AlarmScheduler", "Computed nextAlarmTime ($nextAlarmTime / $scheduledMillis) is <= now ($nowMs). Bumping to now+1s")
            scheduledMillis = nowMs + 1000
        }
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.myreminder.app.TASK_REMINDER"
            putExtra("TASK_ID", task.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Cancel any previous duplicate before scheduling to avoid duplicates
        try {
            alarmManager.cancel(pendingIntent)
        } catch (ex: Exception) {
            android.util.Log.w("AlarmScheduler", "Failed to cancel existing pending intent before scheduling: ${ex.message}")
        }

        if (canScheduleExactAlarms()) {
            android.util.Log.d("AlarmScheduler", "Scheduling exact alarm for task=${task.id} at ${scheduledMillis} (${java.time.Instant.ofEpochMilli(scheduledMillis)})")
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                scheduledMillis,
                pendingIntent
            )
        } else {
            android.util.Log.d("AlarmScheduler", "Exact alarms not available; scheduling inexact alarm for task=${task.id} at ${scheduledMillis} (${java.time.Instant.ofEpochMilli(scheduledMillis)})")
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                scheduledMillis,
                pendingIntent
            )
        }
    }

    fun cancelTaskReminder(taskId: Long) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.myreminder.app.TASK_REMINDER"
            putExtra("TASK_ID", taskId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        alarmManager.cancel(pendingIntent)
    }

    fun scheduleMorningSummary(hour: Int, minute: Int) {
        val now = LocalDateTime.now()
        var alarmTime = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)

        if (alarmTime.isBefore(now) || alarmTime.isEqual(now)) {
            alarmTime = alarmTime.plusDays(1)
        }
        var epochMillis = alarmTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val intent = Intent(context, MorningSummaryReceiver::class.java).apply {
            action = "com.myreminder.app.MORNING_SUMMARY"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            MORNING_SUMMARY_REQ_CODE,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        try {
            alarmManager.cancel(pendingIntent)
        } catch (ex: Exception) {
            android.util.Log.w("AlarmScheduler", "Failed to cancel existing morning summary pending intent: ${ex.message}")
        }
        if (epochMillis <= System.currentTimeMillis() + 500) {
            epochMillis = System.currentTimeMillis() + 1000
        }
        if (canScheduleExactAlarms()) {
            android.util.Log.d("AlarmScheduler", "Scheduling exact morning summary at ${epochMillis} (${java.time.Instant.ofEpochMilli(epochMillis)})")
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                epochMillis,
                pendingIntent
            )
        } else {
            android.util.Log.d("AlarmScheduler", "Exact alarms not available; scheduling inexact morning summary at ${epochMillis} (${java.time.Instant.ofEpochMilli(epochMillis)})")
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                epochMillis,
                pendingIntent
            )
        }
    }

    fun cancelMorningSummary() {
        val intent = Intent(context, MorningSummaryReceiver::class.java).apply {
            action = "com.myreminder.app.MORNING_SUMMARY"
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            MORNING_SUMMARY_REQ_CODE,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        alarmManager.cancel(pendingIntent)
    }

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    companion object {
        const val MORNING_SUMMARY_REQ_CODE = 99999

        fun requestExactAlarmPermission(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    val intent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                } catch (ex: Exception) {
                    android.util.Log.w("AlarmScheduler", "Failed to open exact alarm settings: ${ex.message}")
                }
            }
        }
    }
}
