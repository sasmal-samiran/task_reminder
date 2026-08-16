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

class MorningSummaryReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        android.util.Log.d("MorningSummaryReceiver", "onReceive action=$action at ${LocalDate.now()}")
        if (action != "com.myreminder.app.MORNING_SUMMARY") return

        val pendingResult = goAsync()
        scope.launch {
            try {
                val settings = SettingsDataStore(context)
                if (settings.getNotificationsEnabledSync()) {
                    val db = AppDatabase.getInstance(context)
                    val today = LocalDate.now()
                    val tasks = db.taskDao().getTasksForDate(today).filter { !it.completed }
                    if (tasks.isNotEmpty()) {
                        val notificationHelper = NotificationHelper(context)
                        try {
                            val shown = notificationHelper.showMorningSummary(tasks)
                            android.util.Log.d("MorningSummaryReceiver", "showMorningSummary returned=$shown; count=${tasks.size}")
                        } catch (ex: Exception) {
                            android.util.Log.e("MorningSummaryReceiver", "Exception while showing morning summary: ${ex.message}", ex)
                        }
                    } else {
                        android.util.Log.d("MorningSummaryReceiver", "No tasks for morning summary on $today")
                    }
                } else {
                    android.util.Log.d("MorningSummaryReceiver", "Notifications disabled in settings; not posting morning summary")
                }
                val hour = settings.getMorningHourSync()
                val minute = settings.getMorningMinuteSync()
                val scheduler = AlarmScheduler(context)
                scheduler.scheduleMorningSummary(hour, minute)
                android.util.Log.d("MorningSummaryReceiver", "Rescheduled morning summary for $hour:$minute")
            } finally {
                pendingResult.finish()
            }
        }
    }
}
