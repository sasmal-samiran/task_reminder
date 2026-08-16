package com.myreminder.app.data.model

enum class ReminderInterval(val displayName: String, val minutes: Int) {
    MIN_5("5 Minutes", 5),
    MIN_30("30 Minutes", 30),
    HOUR_1("1 Hour", 60),
    DAY_1("1 Day", 1440),
    WEEK_1("1 Week", 10080);

    companion object {
        fun fromMinutes(minutes: Int): ReminderInterval {
            return entries.find { it.minutes == minutes } ?: DAY_1
        }
    }
}
