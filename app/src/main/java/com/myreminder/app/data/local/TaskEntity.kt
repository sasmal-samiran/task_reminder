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
    val type: TaskType = TaskType.DEADLINE,
    val date: LocalDate, // Event date
    val time: LocalTime?, // Event time
    val reminderDate: LocalDate = date, // Reminder start date (when notifications start sending)
    val reminderTime: LocalTime? = time, // Reminder start time
    val intervalMinutes: Int = 1440, // Duration between notifications in minutes (default: 1440 = 1 day)
    val meetingLink: String? = null,
    val location: String? = null,
    val priority: Priority = Priority.HIGH,
    val notes: String? = null,
    val reminderDurationValue: Int = 1, // Retained for schema migration compatibility
    val reminderDurationUnit: String = "DAYS", // Retained for schema migration compatibility
    val reminderMinutes: Int = 1440, // Retained for backward compatibility
    val completed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Target / Event DateTime when the deadline/event occurs.
     */
    fun getEventDateTime(): LocalDateTime {
        val targetTime = time ?: LocalTime.of(23, 59)
        return LocalDateTime.of(date, targetTime)
    }

    /**
     * Reminder Start DateTime from when notifications begin sending.
     */
    fun getReminderStartDateTime(): LocalDateTime {
        val rTime = reminderTime ?: time ?: LocalTime.of(9, 0)
        return LocalDateTime.of(reminderDate, rTime)
    }

    /**
     * Returns human-readable label for repeat interval.
     */
    fun getIntervalDisplayName(): String {
        return when (intervalMinutes) {
            30 -> "30 minutes"
            60 -> "1 hour"
            1440 -> "1 day"
            10080 -> "1 week"
            else -> if (intervalMinutes % 1440 == 0) "${intervalMinutes / 1440} days" else "$intervalMinutes minutes"
        }
    }
}
