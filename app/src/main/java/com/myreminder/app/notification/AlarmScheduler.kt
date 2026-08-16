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
     * Schedules the next reminder alarm for a task from reminderStart until eventTime at intervalMinutes.
     */
    fun scheduleTaskReminder(task: TaskEntity) {
        if (task.completed) return

        val now = LocalDateTime.now()
        val eventDateTime = task.getEventDateTime()
        val reminderStart = task.getReminderStartDateTime()
        val intervalMinutes = if (task.intervalMinutes > 0) task.intervalMinutes else 1440

        // If event deadline has already passed, no alarm needed
        if (!eventDateTime.isAfter(now)) return

        val nextAlarmTime: LocalDateTime = when {
            // Case 1: Reminder start time is in the future -> schedule at reminder start
            now.isBefore(reminderStart) -> reminderStart

            // Case 2: We are at or past reminder start time -> schedule next interval step
            else -> {
                val potentialNext = now.plusMinutes(intervalMinutes.toLong())
                if (potentialNext.isBefore(eventDateTime)) potentialNext else eventDateTime
            }
        }

        val epochMillis = nextAlarmTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (epochMillis <= System.currentTimeMillis()) return
        if (!canScheduleExactAlarms()) return

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

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            epochMillis,
            pendingIntent
        )
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

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            epochMillis,
            pendingIntent
        )
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
