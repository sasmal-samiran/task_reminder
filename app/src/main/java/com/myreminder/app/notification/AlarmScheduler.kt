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
     * Calculates the sensible repeat interval in minutes based on total window duration.
     */
    fun calculateRepeatIntervalMinutes(totalDurationMinutes: Int): Int {
        return when {
            totalDurationMinutes <= 15 -> 5
            totalDurationMinutes <= 30 -> 10
            totalDurationMinutes <= 120 -> 30 // 2 hours -> every 30 mins
            totalDurationMinutes <= 360 -> 60 // 6 hours -> every 1 hour
            totalDurationMinutes <= 1440 -> 240 // 1 day -> every 4 hours
            totalDurationMinutes <= 4320 -> 360 // 3 days -> every 6 hours
            else -> 720 // > 3 days -> every 12 hours
        }
    }

    /**
     * Schedules the next reminder alarm for a task within its duration window.
     */
    fun scheduleTaskReminder(task: TaskEntity) {
        if (task.completed) return

        val totalDurationMinutes = task.calculateTotalReminderMinutes()
        if (totalDurationMinutes < 0) return

        val now = LocalDateTime.now()
        val targetDateTime = task.getTargetDateTime()

        // If target time has already passed, no alarm needed
        if (!targetDateTime.isAfter(now)) return

        val windowStart = targetDateTime.minusMinutes(totalDurationMinutes.toLong())
        val nextAlarmTime: LocalDateTime = when {
            // Case 1: Window start is in the future -> schedule at window start
            now.isBefore(windowStart) -> windowStart

            // Case 2: Currently inside the window -> schedule next step or now
            else -> {
                val intervalMinutes = calculateRepeatIntervalMinutes(totalDurationMinutes)
                val potentialNext = now.plusMinutes(intervalMinutes.toLong())
                if (potentialNext.isBefore(targetDateTime)) potentialNext else targetDateTime
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
