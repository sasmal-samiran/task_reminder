package com.myreminder.app.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.myreminder.app.R
import com.myreminder.app.data.local.TaskEntity
import com.myreminder.app.data.model.Priority
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class NotificationHelper(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createChannels()
    }

    /**
     * Gets the notification sound URI:
     * 1. User-selected sound from settings (if set)
     * 2. Android default notification sound (fallback)
     */
    private fun getNotificationSoundUri(customSoundUri: String? = null): Uri {
        if (!customSoundUri.isNullOrBlank()) {
            try {
                return Uri.parse(customSoundUri)
            } catch (_: Exception) { /* fallback */ }
        }
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: Settings.System.DEFAULT_NOTIFICATION_URI
    }

    private fun getAudioAttributes(): AudioAttributes {
        return AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .build()
    }

    fun createChannels(customSoundUri: String? = null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = getNotificationSoundUri(customSoundUri)
            val audioAttributes = getAudioAttributes()

            // High Priority Channel
            val highChannel = NotificationChannel(
                CHANNEL_HIGH_PRIORITY,
                "Critical & High Priority Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent interview and task reminders"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
                setSound(soundUri, audioAttributes)
                enableLights(true)
                lightColor = 0xFFC62828.toInt()
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            // Medium Priority Channel
            val mediumChannel = NotificationChannel(
                CHANNEL_MEDIUM_PRIORITY,
                "Medium Priority Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Standard task and test reminders"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400)
                setSound(soundUri, audioAttributes)
                enableLights(true)
                lightColor = 0xFFD97706.toInt()
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            // Low Priority Channel
            val lowChannel = NotificationChannel(
                CHANNEL_LOW_PRIORITY,
                "Low Priority Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Low urgency reminders"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 200, 100, 200)
                setSound(soundUri, audioAttributes)
                lightColor = 0xFF1E40AF.toInt()
            }

            // Morning Summary Channel
            val summaryChannel = NotificationChannel(
                CHANNEL_MORNING_SUMMARY,
                "Morning Summary",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily morning summary of your tasks"
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(highChannel)
            notificationManager.createNotificationChannel(mediumChannel)
            notificationManager.createNotificationChannel(lowChannel)
            notificationManager.createNotificationChannel(summaryChannel)
        }
    }

    fun formatHumanFriendlyDate(date: LocalDate): String {
        val today = LocalDate.now()
        return when {
            date.isEqual(today) -> "Today, ${date.format(DateTimeFormatter.ofPattern("d MMM"))}"
            date.isEqual(today.plusDays(1)) -> "Tomorrow, ${date.format(DateTimeFormatter.ofPattern("d MMM"))}"
            date.year == today.year -> date.format(DateTimeFormatter.ofPattern("d MMM"))
            else -> date.format(DateTimeFormatter.ofPattern("d MMM yyyy"))
        }
    }

    fun formatHumanFriendlyTime(time: java.time.LocalTime?): String {
        return time?.format(DateTimeFormatter.ofPattern("h:mm a")) ?: "All Day"
    }

    fun formatFullDateTimeString(date: LocalDate, time: java.time.LocalTime?): String {
        val dateStr = formatHumanFriendlyDate(date)
        val timeStr = formatHumanFriendlyTime(time)
        return if (time != null) "$dateStr, $timeStr" else dateStr
    }

    fun showTaskReminder(task: TaskEntity, customSoundUri: String? = null) {
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

        val channelId = when (task.priority) {
            Priority.HIGH -> CHANNEL_HIGH_PRIORITY
            Priority.MEDIUM -> CHANNEL_MEDIUM_PRIORITY
            Priority.LOW -> CHANNEL_LOW_PRIORITY
        }

        val dateText = formatHumanFriendlyDate(task.date)
        val timeText = formatHumanFriendlyTime(task.time)
        val fullDateTimeText = formatFullDateTimeString(task.date, task.time)
        val companyText = if (task.company.isNotBlank()) task.company else "MyReminder"

        // Accent color for company name based on priority
        val companyAccentColor = when (task.priority) {
            Priority.HIGH -> 0xFFF87171.toInt()    // Light red
            Priority.MEDIUM -> 0xFFFBBF24.toInt()  // Amber
            Priority.LOW -> 0xFF60A5FA.toInt()      // Light blue
        }

        // Priority bar drawable
        val priorityBarRes = when (task.priority) {
            Priority.HIGH -> R.drawable.bg_priority_pill_high
            Priority.MEDIUM -> R.drawable.bg_priority_pill_medium
            Priority.LOW -> R.drawable.bg_priority_pill_low
        }

        // Bottom gradient line drawable
        val bottomLineRes = when (task.priority) {
            Priority.HIGH -> R.drawable.bg_bottom_line_high
            Priority.MEDIUM -> R.drawable.bg_bottom_line_medium
            Priority.LOW -> R.drawable.bg_bottom_line_low
        }

        // Build Collapsed RemoteViews
        val collapsedViews = RemoteViews(context.packageName, R.layout.notification_task_reminder_collapsed).apply {
            setTextViewText(R.id.notification_title, task.title)
            setTextViewText(R.id.notification_company, companyText)
            setTextViewText(R.id.notification_date, dateText)
            setTextViewText(R.id.notification_time, timeText)
            setTextColor(R.id.notification_company, companyAccentColor)
            setInt(R.id.notification_priority_bar, "setBackgroundResource", priorityBarRes)
        }

        // Build Expanded RemoteViews
        val expandedViews = RemoteViews(context.packageName, R.layout.notification_task_reminder_expanded).apply {
            setTextViewText(R.id.notification_title, task.title)
            setTextViewText(R.id.notification_company, companyText)
            setTextViewText(R.id.notification_date, dateText)
            setTextViewText(R.id.notification_time, timeText)
            setTextColor(R.id.notification_company, companyAccentColor)
            setInt(R.id.notification_priority_bar, "setBackgroundResource", priorityBarRes)
            setInt(R.id.notification_bottom_line, "setBackgroundResource", bottomLineRes)

            // Show notes/description if available
            val detailParts = mutableListOf<String>()
            task.notes?.takeIf { it.isNotBlank() }?.let { detailParts.add(it) }
            task.location?.takeIf { it.isNotBlank() }?.let { detailParts.add("📍 $it") }

            if (detailParts.isNotEmpty()) {
                setTextViewText(R.id.notification_details, detailParts.joinToString("\n"))
                setViewVisibility(R.id.notification_details, View.VISIBLE)
            } else {
                setViewVisibility(R.id.notification_details, View.GONE)
            }
        }

        val soundUri = getNotificationSoundUri(customSoundUri)
        val vibrationPattern = when (task.priority) {
            Priority.HIGH -> longArrayOf(0, 500, 200, 500, 200, 500)
            Priority.MEDIUM -> longArrayOf(0, 400, 200, 400)
            Priority.LOW -> longArrayOf(0, 200, 100, 200)
        }

        val notificationPriority = when (task.priority) {
            Priority.HIGH -> NotificationCompat.PRIORITY_MAX
            Priority.MEDIUM -> NotificationCompat.PRIORITY_HIGH
            Priority.LOW -> NotificationCompat.PRIORITY_DEFAULT
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("${task.title}")
            .setContentText("$companyText — $fullDateTimeText")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(task.title)
                    .setSummaryText(companyText)
                    .bigText("$companyText\n📅 $fullDateTimeText\n📌 ${task.type.displayName}${if (!task.notes.isNullOrBlank()) "\n\n${task.notes}" else ""}")
            )
            .setCustomContentView(collapsedViews)
            .setCustomBigContentView(expandedViews)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setPriority(notificationPriority)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(soundUri)
            .setVibrate(vibrationPattern)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(task.priority.hexColorInt.toInt())
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

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
            .setBigContentTitle("🔔 Good Morning! You have ${tasks.size} task${if (tasks.size > 1) "s" else ""} today:")

        tasks.take(6).forEach { task ->
            val timeStr = formatHumanFriendlyTime(task.time)
            val companyPrefix = if (task.company.isNotBlank()) "${task.company} " else ""
            inboxStyle.addLine("• $timeStr — $companyPrefix${task.title}")
        }
        if (tasks.size > 6) {
            inboxStyle.setSummaryText("+ ${tasks.size - 6} more upcoming")
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_MORNING_SUMMARY)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Today's Tasks (${tasks.size})")
            .setContentText("You have ${tasks.size} tasks scheduled for today.")
            .setStyle(inboxStyle)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(0xFF1B5E20.toInt())

        notificationManager.notify(MORNING_SUMMARY_ID, builder.build())
    }

    companion object {
        const val CHANNEL_HIGH_PRIORITY = "channel_task_high_priority"
        const val CHANNEL_MEDIUM_PRIORITY = "channel_task_medium_priority"
        const val CHANNEL_LOW_PRIORITY = "channel_task_low_priority"
        const val CHANNEL_MORNING_SUMMARY = "channel_morning_summary"
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
