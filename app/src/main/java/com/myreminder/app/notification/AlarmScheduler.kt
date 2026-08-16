package com.myreminder.app.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import com.myreminder.app.data.local.TaskEntity
import java.time.LocalDateTime
import java.time.ZoneId

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Schedules reminder alarms for a task.
     *
     * @param task The task to schedule.
     * @param isNextChainedAlarm True if called from AlarmReceiver after a previous reminder alarm fired.
     */
    fun scheduleTaskReminder(task: TaskEntity, isNextChainedAlarm: Boolean = false) {
        if (task.completed) {
            cancelTaskReminder(task.id)
            return
        }

        val now = LocalDateTime.now()
        val eventDateTime = task.getEventDateTime()

        // If event deadline has already passed, no alarm needed
        if (!eventDateTime.isAfter(now)) {
            Log.d("AlarmScheduler", "Task ${task.id} eventDateTime $eventDateTime is in past. Skipping.")
            return
        }

        val reminderStart = task.getReminderStartDateTime()
        val intervalMinutes = task.reminderIntervalMinutes.toLong().coerceAtLeast(1)

        val nextAlarmTime: LocalDateTime = when {
            // Case 1: Reminder start date/time is in the future -> wait until that start time
            reminderStart.isAfter(now) -> reminderStart

            // Case 2: Reminder start was in the past or right now, and this is a NEW / edit schedule
            !isNextChainedAlarm -> {
                // Fire immediately (1-2 seconds from now) so the user gets their first notification right away
                now.plusSeconds(1)
            }

            // Case 3: Chained repeat after an alarm just fired
            else -> {
                val nextStep = now.plusMinutes(intervalMinutes)
                if (nextStep.isAfter(eventDateTime)) {
                    if (now.isBefore(eventDateTime)) eventDateTime else return
                } else {
                    nextStep
                }
            }
        }

        val epochMillis = nextAlarmTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (epochMillis <= System.currentTimeMillis() - 2000) {
            Log.d("AlarmScheduler", "Target time $epochMillis is in past. Skipping.")
            return
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.myreminder.app.TASK_REMINDER"
            `package` = context.packageName
            data = Uri.parse("reminder://task/${task.id}")
            putExtra("TASK_ID", task.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        setAlarmSafely(epochMillis, pendingIntent, task.id)
    }

    private fun setAlarmSafely(epochMillis: Long, pendingIntent: PendingIntent, taskId: Long) {
        try {
            if (canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    epochMillis,
                    pendingIntent
                )
                Log.d("AlarmScheduler", "Scheduled exact alarm for task $taskId at $epochMillis")
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    epochMillis,
                    pendingIntent
                )
                Log.d("AlarmScheduler", "Scheduled inexact allow-while-idle alarm for task $taskId at $epochMillis")
            }
        } catch (e: SecurityException) {
            Log.w("AlarmScheduler", "SecurityException setting exact alarm: ${e.message}. Falling back.")
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    epochMillis,
                    pendingIntent
                )
            } catch (e2: Exception) {
                Log.e("AlarmScheduler", "Fallback failed: ${e2.message}")
                try {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        epochMillis,
                        pendingIntent
                    )
                } catch (e3: Exception) {
                    Log.e("AlarmScheduler", "Final fallback failed: ${e3.message}")
                }
            }
        }
    }

    fun cancelTaskReminder(taskId: Long) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.myreminder.app.TASK_REMINDER"
            `package` = context.packageName
            data = Uri.parse("reminder://task/$taskId")
            putExtra("TASK_ID", taskId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        alarmManager.cancel(pendingIntent)
        Log.d("AlarmScheduler", "Cancelled alarm for task $taskId")
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
            `package` = context.packageName
            data = Uri.parse("reminder://summary/morning")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            MORNING_SUMMARY_REQ_CODE,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        setAlarmSafely(epochMillis, pendingIntent, MORNING_SUMMARY_REQ_CODE.toLong())
    }

    fun cancelMorningSummary() {
        val intent = Intent(context, MorningSummaryReceiver::class.java).apply {
            action = "com.myreminder.app.MORNING_SUMMARY"
            `package` = context.packageName
            data = Uri.parse("reminder://summary/morning")
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
