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
    val date: LocalDate,
    val time: LocalTime?,
    val meetingLink: String?,
    val location: String?,
    val priority: Priority,
    val notes: String?,
    val reminderDurationValue: Int = 30, // e.g. 15, 1, 2
    val reminderDurationUnit: String = "MINUTES", // "MINUTES", "HOURS", "DAYS"
    val reminderMinutes: Int = 30, // Total minutes for quick calculation / backward compatibility (-1 = no reminder)
    val completed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Calculates the target LocalDateTime for this task.
     * Defaults to 09:00 if no time is specified.
     */
    fun getTargetDateTime(): LocalDateTime {
        val targetTime = time ?: LocalTime.of(9, 0)
        return LocalDateTime.of(date, targetTime)
    }

    /**
     * Total duration in minutes before the target time.
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
     * Calculates when the reminder window begins: target_datetime - reminder_duration.
     */
    fun getReminderWindowStart(): LocalDateTime {
        val totalMins = calculateTotalReminderMinutes()
        if (totalMins <= 0) return getTargetDateTime()
        return getTargetDateTime().minusMinutes(totalMins.toLong())
    }
}
