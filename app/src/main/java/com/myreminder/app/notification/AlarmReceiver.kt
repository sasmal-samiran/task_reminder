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

class AlarmReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.myreminder.app.TASK_REMINDER") return
        
        val taskId = intent.getLongExtra("TASK_ID", -1L)
        if (taskId == -1L) return

        val pendingResult = goAsync()

        scope.launch {
            try {
                val settings = SettingsDataStore(context)
                if (!settings.getNotificationsEnabledSync()) return@launch

                val db = AppDatabase.getInstance(context)
                val task = db.taskDao().getTaskById(taskId)
                
                if (task != null && !task.completed) {
                    val notificationHelper = NotificationHelper(context)
                    notificationHelper.showTaskReminder(task)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
