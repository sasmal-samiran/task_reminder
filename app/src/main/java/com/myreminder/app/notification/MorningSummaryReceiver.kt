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
        if (intent.action != "com.myreminder.app.MORNING_SUMMARY") return

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
                        notificationHelper.showMorningSummary(tasks)
                    }
                }

                val hour = settings.getMorningHourSync()
                val minute = settings.getMorningMinuteSync()
                val scheduler = AlarmScheduler(context)
                scheduler.scheduleMorningSummary(hour, minute)
                
            } finally {
                pendingResult.finish()
            }
        }
    }
}
