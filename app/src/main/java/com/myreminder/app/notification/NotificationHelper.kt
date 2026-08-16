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
import com.myreminder.app.data.local.SettingsDataStore
import com.myreminder.app.data.local.TaskEntity
import com.myreminder.app.data.model.Priority
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class NotificationHelper(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val settingsDataStore = SettingsDataStore(context)

    init {
        createChannels()
    }

    private fun getSelectedSoundUri(): Uri {
        return try {
            val customUriStr = runBlocking { settingsDataStore.getCustomNotificationSoundUriSync() }
            if (!customUriStr.isNullOrBlank()) {
                Uri.parse(customUriStr)
            } else {
                Settings.System.DEFAULT_NOTIFICATION_URI
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }
        } catch (e: Exception) {
            Settings.System.DEFAULT_NOTIFICATION_URI
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }
    }

    private fun getAudioAttributes(): AudioAttributes {
        return AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
            .build()
    }

    fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = getSelectedSoundUri()
            val audioAttributes = getAudioAttributes()
            val vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)

            // High Priority Channel - High Urgency
            val highChannel = NotificationChannel(
                CHANNEL_HIGH_PRIORITY,
                "Critical Reminders & Deadlines",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority task and interview notifications"
                enableVibration(true)
                this.vibrationPattern = vibrationPattern
                setSound(soundUri, audioAttributes)
                enableLights(true)
                lightColor = 0xFFEF4444.toInt()
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            // Medium Priority Channel
            val mediumChannel = NotificationChannel(
                CHANNEL_MEDIUM_PRIORITY,
                "Medium Priority Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Medium priority task notifications"
                enableVibration(true)
                this.vibrationPattern = longArrayOf(0, 400, 200, 400)
                setSound(soundUri, audioAttributes)
                enableLights(true)
                lightColor = 0xFFF59E0B.toInt()
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            // Low Priority Channel
            val lowChannel = NotificationChannel(
                CHANNEL_LOW_PRIORITY,
                "Low Priority Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Standard low urgency task notifications"
                enableVibration(true)
                setSound(soundUri, audioAttributes)
                lightColor = 0xFF3B82F6.toInt()
            }

            // Morning Summary Channel
            val summaryChannel = NotificationChannel(
                CHANNEL_MORNING_SUMMARY,
                "Morning Summary",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily 7:00 AM summary of your tasks"
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(highChannel)
            notificationManager.createNotificationChannel(mediumChannel)
            notificationManager.createNotificationChannel(lowChannel)
            notificationManager.createNotificationChannel(summaryChannel)
        }
    }

    /**
     * Formats date into human-friendly representation matching reference:
     * - "Today, 16 Aug"
     * - "Tomorrow, 17 Aug"
     * - "24 Aug" (or "16 August")
     */
    fun formatHumanFriendlyDate(date: LocalDate): String {
        val today = LocalDate.now()
        return when {
            date.isEqual(today) -> "Today, ${date.format(DateTimeFormatter.ofPattern("d MMM"))}"
            date.isEqual(today.plusDays(1)) -> "Tomorrow, ${date.format(DateTimeFormatter.ofPattern("d MMM"))}"
            date.year == today.year -> date.format(DateTimeFormatter.ofPattern("d MMM"))
            else -> date.format(DateTimeFormatter.ofPattern("d MMM yyyy"))
        }
    }

    fun formatHumanFriendlyTime(time: LocalTime?): String {
        return time?.format(DateTimeFormatter.ofPattern("h:mm a")) ?: "All Day"
    }

    fun formatFullDateTimeString(date: LocalDate, time: LocalTime?): String {
        val dateStr = formatHumanFriendlyDate(date)
        val timeStr = formatHumanFriendlyTime(time)
        return "$dateStr | $timeStr"
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

        val channelId = when (task.priority) {
            Priority.HIGH -> CHANNEL_HIGH_PRIORITY
            Priority.MEDIUM -> CHANNEL_MEDIUM_PRIORITY
            Priority.LOW -> CHANNEL_LOW_PRIORITY
        }

        val dateText = formatHumanFriendlyDate(task.date)
        val timeText = formatHumanFriendlyTime(task.time)
        val fullDateTimeText = formatFullDateTimeString(task.date, task.time)
        val companyText = if (task.company.isNotBlank()) task.company else task.type.displayName

        val notesText = when {
            !task.notes.isNullOrBlank() -> task.notes
            !task.location.isNullOrBlank() -> "Location: ${task.location}"
            !task.meetingLink.isNullOrBlank() -> "Meeting: ${task.meetingLink}"
            else -> "Reminder for your scheduled ${task.type.displayName}."
        }

        val companyColor = when (task.priority) {
            Priority.HIGH -> 0xFFEF4444.toInt() // Red
            Priority.MEDIUM -> 0xFFF59E0B.toInt() // Amber
            Priority.LOW -> 0xFF3B82F6.toInt() // Blue
        }

        // Build Custom Collapsed RemoteViews
        val collapsedViews = RemoteViews(context.packageName, R.layout.notification_task_reminder_collapsed).apply {
            setTextViewText(R.id.notification_title, task.title)
            setTextViewText(R.id.notification_company, companyText)
            setTextColor(R.id.notification_company, companyColor)
            setTextViewText(R.id.notification_date, dateText)
            setTextViewText(R.id.notification_time, timeText)
            setTextViewText(R.id.notification_notes, notesText)

            when (task.priority) {
                Priority.HIGH -> {
                    setInt(R.id.notification_card, "setBackgroundResource", R.drawable.bg_notification_card_v2_high)
                    setInt(R.id.notification_priority_pill, "setBackgroundResource", R.drawable.bg_priority_pill_high)
                }
                Priority.MEDIUM -> {
                    setInt(R.id.notification_card, "setBackgroundResource", R.drawable.bg_notification_card_v2_medium)
                    setInt(R.id.notification_priority_pill, "setBackgroundResource", R.drawable.bg_priority_pill_medium)
                }
                Priority.LOW -> {
                    setInt(R.id.notification_card, "setBackgroundResource", R.drawable.bg_notification_card_v2_low)
                    setInt(R.id.notification_priority_pill, "setBackgroundResource", R.drawable.bg_priority_pill_low)
                }
            }
        }

        // Build Custom Expanded RemoteViews (Full non-truncated text wrapping)
        val expandedViews = RemoteViews(context.packageName, R.layout.notification_task_reminder_expanded).apply {
            setTextViewText(R.id.notification_title, task.title)
            setTextViewText(R.id.notification_company, "$companyText • ${task.type.displayName}")
            setTextColor(R.id.notification_company, companyColor)
            setTextViewText(R.id.notification_date, dateText)
            setTextViewText(R.id.notification_time, timeText)

            val fullDetails = buildString {
                if (!task.notes.isNullOrBlank()) append(task.notes)
                if (!task.location.isNullOrBlank()) {
                    if (isNotEmpty()) append("\n📍 ") else append("📍 ")
                    append(task.location)
                }
                if (!task.meetingLink.isNullOrBlank()) {
                    if (isNotEmpty()) append("\n🔗 ") else append("🔗 ")
                    append(task.meetingLink)
                }
                if (isEmpty()) append("Scheduled ${task.type.displayName} deadline reminder.")
            }
            setTextViewText(R.id.notification_notes, fullDetails)

            when (task.priority) {
                Priority.HIGH -> {
                    setInt(R.id.notification_card, "setBackgroundResource", R.drawable.bg_notification_card_v2_high)
                    setInt(R.id.notification_priority_pill, "setBackgroundResource", R.drawable.bg_priority_pill_high)
                }
                Priority.MEDIUM -> {
                    setInt(R.id.notification_card, "setBackgroundResource", R.drawable.bg_notification_card_v2_medium)
                    setInt(R.id.notification_priority_pill, "setBackgroundResource", R.drawable.bg_priority_pill_medium)
                }
                Priority.LOW -> {
                    setInt(R.id.notification_card, "setBackgroundResource", R.drawable.bg_notification_card_v2_low)
                    setInt(R.id.notification_priority_pill, "setBackgroundResource", R.drawable.bg_priority_pill_low)
                }
            }
        }

        val soundUri = getSelectedSoundUri()
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
            .setContentTitle(task.title)
            .setContentText("$companyText | $fullDateTimeText")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(task.title)
                    .bigText("${task.company}\n$notesText\n\n📅 $fullDateTimeText")
            )
            .setCustomContentView(collapsedViews)
            .setCustomBigContentView(expandedViews)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setPriority(notificationPriority)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setSound(soundUri)
            .setVibrate(vibrationPattern)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(companyColor)
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
        const val CHANNEL_HIGH_PRIORITY = "channel_task_high_priority_v2"
        const val CHANNEL_MEDIUM_PRIORITY = "channel_task_medium_priority_v2"
        const val CHANNEL_LOW_PRIORITY = "channel_task_low_priority_v2"
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
