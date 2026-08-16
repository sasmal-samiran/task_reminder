package com.myreminder.app.data.model

enum class ReminderOption(val displayName: String, val minutesBefore: Int) {
    AT_TIME("At time of task", 0),
    MIN_5("5 minutes before", 5),
    MIN_15("15 minutes before", 15),
    MIN_30("30 minutes before", 30),
    HOUR_1("1 hour before", 60),
    DAY_1("1 day before", 1440),
    NONE("No reminder", -1);

    companion object {
        fun fromMinutes(minutes: Int): ReminderOption {
            return entries.find { it.minutesBefore == minutes } ?: NONE
        }
    }
}
