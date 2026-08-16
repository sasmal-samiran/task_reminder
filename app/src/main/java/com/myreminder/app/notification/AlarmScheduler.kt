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
        if (epochMillis <= System.currentTimeMillis()) return
n        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.myreminder.app.TASK_REMINDER"
            putExtra("TASK_ID", task.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        if (canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                epochMillis,
                pendingIntent
            )
        } else {
            // Fall back to an inexact alarm so reminders still fire approximately when exact alarms are disallowed
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                epochMillis,
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
        if (!canScheduleExactAlarms()) return

        val now = LocalDateTime.now()
        var alarmTime = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)

        if (alarmTime.isBefore(now) || alarmTime.isEqual(now)) {
            alarmTime = alarmTime.plusDays(1)
        }

        val epochMillis = alarmTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val intent = Intent(context, MorningSummaryReceiver::class.java).apply {
            action = "com.myreminder.app.MORNING_SUMMARY"
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            MORNING_SUMMARY_REQ_CODE,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        if (canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                epochMillis,
                pendingIntent
            )
        } else {
            // Fall back to inexact alarm for devices/users that disallow exact alarms
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
    }
}
