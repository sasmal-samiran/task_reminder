package com.myreminder.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.myreminder.app.data.local.AppDatabase
import com.myreminder.app.data.local.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

class BootReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val validActions = listOf(
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.LOCKED_BOOT_COMPLETED",
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON"
        )

        if (intent.action !in validActions) return

        val pendingResult = goAsync()

        scope.launch {
            try {
                val scheduler = AlarmScheduler(context)
                val settings = SettingsDataStore(context)

                if (settings.getNotificationsEnabledSync()) {
                    val hour = settings.getMorningHourSync()
                    val minute = settings.getMorningMinuteSync()
                    scheduler.scheduleMorningSummary(hour, minute)

                    val db = AppDatabase.getInstance(context)
                    val today = LocalDate.now()
                    val upcomingTasks = db.taskDao().getTasksWithReminders(today)
                    val now = LocalDateTime.now()

                    upcomingTasks.forEach { task ->
                        if (!task.completed && task.getTargetDateTime().isAfter(now)) {
                            scheduler.scheduleTaskReminder(task)
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
