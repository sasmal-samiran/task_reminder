package com.myreminder.app

import android.app.Application
import com.myreminder.app.notification.NotificationHelper

class MyReminderApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Channels are created in NotificationHelper's init block
        NotificationHelper(this)
    }
}
