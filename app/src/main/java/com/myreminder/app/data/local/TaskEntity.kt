package com.myreminder.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.myreminder.app.data.model.Priority
import com.myreminder.app.data.model.TaskType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val company: String,
    val type: TaskType,
    val date: LocalDate,           // Event date
    val time: LocalTime?,          // Event time
    val meetingLink: String?,
    val location: String?,
    val priority: Priority,
    val notes: String?,
    val reminderDurationValue: Int = 30,
    val reminderDurationUnit: String = "MINUTES",
    val reminderMinutes: Int = 30,
    val reminderDate: LocalDate? = null,     // When reminders start (date)
    val reminderTime: LocalTime? = null,     // When reminders start (time), default 7:00 AM
    val reminderIntervalMinutes: Int = 1440, // Interval between repeated reminders (default 1 day)
    val completed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Returns the event LocalDateTime (when the task/event is due).
     * Defaults to 23:59 if no time is specified.
     */
    fun getEventDateTime(): LocalDateTime {
        val eventTime = time ?: LocalTime.of(23, 59)
        return LocalDateTime.of(date, eventTime)
    }

    /**
     * Alias for backward compatibility.
     */
    fun getTargetDateTime(): LocalDateTime = getEventDateTime()

    /**
     * Returns the reminder start LocalDateTime (when notifications begin).
     * Falls back to event datetime minus 1 day if not explicitly set.
     */
    fun getReminderStartDateTime(): LocalDateTime {
        val rDate = reminderDate ?: date.minusDays(1)
        val rTime = reminderTime ?: LocalTime.of(7, 0)
        return LocalDateTime.of(rDate, rTime)
    }

    /**
     * Total duration in minutes before the target time (backward compat).
     */
    fun calculateTotalReminderMinutes(): Int {
        if (reminderDurationValue <= 0) return -1
        return when (reminderDurationUnit.uppercase()) {
            "MINUTES", "MINUTE" -> reminderDurationValue
            "HOURS", "HOUR" -> reminderDurationValue * 60
            "DAYS", "DAY" -> reminderDurationValue * 1440
            else -> reminderDurationValue
        }
    }

    /**
     * Old method for backward compatibility.
     */
    fun getReminderWindowStart(): LocalDateTime {
        val totalMins = calculateTotalReminderMinutes()
        if (totalMins <= 0) return getTargetDateTime()
        return getTargetDateTime().minusMinutes(totalMins.toLong())
    }
}
