package com.myreminder.app.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import com.myreminder.app.data.local.TaskEntity
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        private const val TAG = "MyReminderScheduler"
        const val MORNING_SUMMARY_REQ_CODE = 99999
        val DATE_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }

    /**
     * Calculates the exact next LocalDateTime for the alarm.
     */
    fun calculateNextAlarmTime(task: TaskEntity, now: LocalDateTime = LocalDateTime.now()): LocalDateTime? {
        val eventDateTime = task.getEventDateTime()
        val reminderStart = task.getReminderStartDateTime()
        val intervalMinutes = if (task.intervalMinutes > 0) task.intervalMinutes else 1440

        // If event deadline has already passed or task is completed, no alarm
        if (!eventDateTime.isAfter(now) || task.completed) {
            return null
        }

        return when {
            // Case 1: Reminder start time is in the future
            now.isBefore(reminderStart) -> reminderStart

            // Case 2: Reminder start is right now or past, but event is in future
            else -> {
                // If reminderStart was in the past when scheduled, find the next interval slot
                var slot = reminderStart
                while (!slot.isAfter(now)) {
                    slot = slot.plusMinutes(intervalMinutes.toLong())
                }
                if (slot.isBefore(eventDateTime)) slot else eventDateTime
            }
        }
    }

    /**
     * Schedules the next reminder alarm for a task.
     * Uses setExactAndAllowWhileIdle to guarantee execution during Doze mode.
     */
    fun scheduleTaskReminder(task: TaskEntity) {
        if (task.completed) {
            Log.d(TAG, "Task [ID: ${task.id}, '${task.title}'] is completed. Skipping alarm.")
            return
        }

        val now = LocalDateTime.now()
        val eventDateTime = task.getEventDateTime()
        val reminderStart = task.getReminderStartDateTime()
        val intervalMinutes = if (task.intervalMinutes > 0) task.intervalMinutes else 1440

        if (!eventDateTime.isAfter(now)) {
            Log.d(TAG, "Task [ID: ${task.id}, '${task.title}'] event time ($eventDateTime) is in past (Now: $now). Skipping.")
            return
        }

        // If reminderStart was <= now, and no alarm fired yet (e.g. task just created/edited):
        // If reminderStart is right now or within last 2 minutes, fire immediate alarm in 2 seconds
        val nextAlarmTime: LocalDateTime = if (reminderStart.isBefore(now) && Duration.between(reminderStart, now).toMinutes() < 2) {
            now.plusSeconds(2)
        } else {
            calculateNextAlarmTime(task, now) ?: return
        }

        val epochMillis = nextAlarmTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val currentMillis = System.currentTimeMillis()

        if (epochMillis < currentMillis) {
            Log.w(TAG, "Calculated epochMillis ($epochMillis) is before currentMillis ($currentMillis). Aborting.")
            return
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.myreminder.app.TASK_REMINDER"
            data = Uri.parse("custom://myreminder/task/${task.id}")
            putExtra("TASK_ID", task.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val canExact = canScheduleExactAlarms()
        val timeDiffMinutes = Duration.between(now, nextAlarmTime).toMinutes()
        val timeDiffSeconds = Duration.between(now, nextAlarmTime).seconds

        Log.d(TAG, "==================================================")
        Log.d(TAG, "SCHEDULING ALARM:")
        Log.d(TAG, "  Task ID: ${task.id} | Title: '${task.title}' | Company: '${task.company}'")
        Log.d(TAG, "  Current Time:      ${now.format(DATE_TIME_FORMAT)}")
        Log.d(TAG, "  Reminder Start:    ${reminderStart.format(DATE_TIME_FORMAT)}")
        Log.d(TAG, "  Event Deadline:    ${eventDateTime.format(DATE_TIME_FORMAT)}")
        Log.d(TAG, "  Repeat Interval:   ${intervalMinutes}m (${task.getIntervalDisplayName()})")
        Log.d(TAG, "  Next Alarm Time:   ${nextAlarmTime.format(DATE_TIME_FORMAT)} (in ${timeDiffMinutes}m / ${timeDiffSeconds}s)")
        Log.d(TAG, "  Next Epoch Millis: $epochMillis (Current: $currentMillis)")
        Log.d(TAG, "  Exact Alarm Permitted: $canExact")
        Log.d(TAG, "==================================================")

        try {
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    epochMillis,
                    pendingIntent
                )
            } else {
                Log.w(TAG, "Exact alarms not allowed. Falling back to setAndAllowWhileIdle for Task ID: ${task.id}")
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    epochMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while scheduling exact alarm for Task ID: ${task.id}. Falling back to non-exact.", e)
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                epochMillis,
                pendingIntent
            )
        }
    }

    fun cancelTaskReminder(taskId: Long) {
        Log.d(TAG, "Cancelling scheduled alarm for Task ID: $taskId")
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.myreminder.app.TASK_REMINDER"
            data = Uri.parse("custom://myreminder/task/$taskId")
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

        val epochMillis = alarmTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val intent = Intent(context, MorningSummaryReceiver::class.java).apply {
            action = "com.myreminder.app.MORNING_SUMMARY"
            data = Uri.parse("custom://myreminder/morning_summary")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            MORNING_SUMMARY_REQ_CODE,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val canExact = canScheduleExactAlarms()
        Log.d(TAG, "Scheduling Morning Summary at ${alarmTime.format(DATE_TIME_FORMAT)} (Epoch: $epochMillis, Exact: $canExact)")

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
    }

    fun cancelMorningSummary() {
        Log.d(TAG, "Cancelling Morning Summary alarm")
        val intent = Intent(context, MorningSummaryReceiver::class.java).apply {
            action = "com.myreminder.app.MORNING_SUMMARY"
            data = Uri.parse("custom://myreminder/morning_summary")
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
}
