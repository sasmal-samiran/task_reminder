package com.myreminder.app.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.myreminder.app.data.local.TaskEntity
import java.time.format.DateTimeFormatter

class NotificationHelper(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createChannels()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val taskChannel = NotificationChannel(
                CHANNEL_TASK_REMINDERS,
                "Task Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for upcoming tasks"
            }

            val summaryChannel = NotificationChannel(
                CHANNEL_MORNING_SUMMARY,
                "Morning Summary",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily morning summary of your tasks"
            }

            notificationManager.createNotificationChannel(taskChannel)
            notificationManager.createNotificationChannel(summaryChannel)
        }
    }

    fun showTaskReminder(task: TaskEntity) {
        if (!hasNotificationPermission(context)) return

        val intent = Intent().apply {
            setClassName(context, "com.myreminder.app.MainActivity")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("taskId", task.id)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val timeStr = task.time?.format(DateTimeFormatter.ofPattern("h:mm a")) ?: "All day"
        val text = "${task.type.displayName} at $timeStr"
        val title = "${task.title} - ${task.company}"

        val builder = NotificationCompat.Builder(context, CHANNEL_TASK_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(task.id.toInt(), builder.build())
    }

    fun showMorningSummary(tasks: List<TaskEntity>) {
        if (!hasNotificationPermission(context)) return
        if (tasks.isEmpty()) return

        val intent = Intent().apply {
            setClassName(context, "com.myreminder.app.MainActivity")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            MORNING_SUMMARY_ID,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle("Today's Tasks (${tasks.size})")

        val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")
        tasks.take(5).forEach { task ->
            val timeStr = task.time?.format(timeFormatter) ?: "All day"
            inboxStyle.addLine("$timeStr — ${task.company} ${task.title}")
        }
        if (tasks.size > 5) {
            inboxStyle.setSummaryText("+ ${tasks.size - 5} more")
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_MORNING_SUMMARY)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Morning Summary")
            .setContentText("You have ${tasks.size} tasks today")
            .setStyle(inboxStyle)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(MORNING_SUMMARY_ID, builder.build())
    }

    companion object {
        const val CHANNEL_TASK_REMINDERS = "task_reminders"
        const val CHANNEL_MORNING_SUMMARY = "morning_summary"
        const val MORNING_SUMMARY_ID = 99999

        fun hasNotificationPermission(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        }
    }
}
